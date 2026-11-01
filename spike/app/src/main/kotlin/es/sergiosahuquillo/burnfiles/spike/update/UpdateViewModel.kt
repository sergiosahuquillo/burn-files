package es.sergiosahuquillo.burnfiles.spike.update

import android.app.Activity
import android.content.pm.PackageInstaller
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.sergiosahuquillo.burnfiles.spike.AppContainer
import es.sergiosahuquillo.burnfiles.spike.BuildConfig
import es.sergiosahuquillo.burnfiles.spike.access.AttemptOutcome
import es.sergiosahuquillo.burnfiles.spike.access.SettingsIntents
import es.sergiosahuquillo.burnfiles.spike.access.launchSafely
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UpdatePhase {
    IDLE,
    CHECKING,
    FOUND,
    UP_TO_DATE,
    DOWNLOADING,
    VERIFIED,
    NEEDS_PERMISSION,
    INSTALLING,
    FAILED
}

data class UpdateUiState(
    val installedVersionName: String = BuildConfig.VERSION_NAME,
    val installedVersionCode: Int = BuildConfig.VERSION_CODE,
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val release: SpikeRelease? = null,
    val downloaded: Long = 0,
    val total: Long = 0,
    val lastStep: UpdateStep? = null,
    val lastOutcome: UpdateOutcome? = null,
    val rateLimitResetEpochSeconds: Long? = null,
    val confirmCancel: Boolean = false
) {
    val busy get() = phase == UpdatePhase.CHECKING || phase == UpdatePhase.DOWNLOADING ||
        phase == UpdatePhase.INSTALLING
}

/** Flow QUERY → DOWNLOAD → VERIFY_SHA256 → VERIFY_CERT → INSTALL_PERMISSION → INSTALL (data-model.md). */
class UpdateViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private var job: Job? = null
    private var verifiedApk: File? = null

    init {
        viewModelScope.launch {
            // Coming back from the "unknown sources" settings screen.
            container.access.resumes.collect {
                if (_state.value.phase == UpdatePhase.NEEDS_PERMISSION && container.installer.canInstall()) {
                    record(UpdateStep.INSTALL_PERMISSION, UpdateOutcome.OK)
                    startInstall()
                }
            }
        }
        viewModelScope.launch { container.installEvents.collect(::onInstallerStatus) }
    }

    private val from get() = _state.value.installedVersionCode
    private val to get() = _state.value.release?.versionCode ?: 0

    fun check() {
        if (_state.value.busy) return
        job =
            viewModelScope.launch {
                _state.update {
                    it.copy(phase = UpdatePhase.CHECKING, release = null, rateLimitResetEpochSeconds = null)
                }
                when (val result = container.releases.releases()) {
                    is ReleasesResult.Ok -> {
                        val release = ReleaseSelector.select(result.releases)
                        _state.update { it.copy(release = release) }
                        when {
                            release == null -> fail(UpdateStep.QUERY, UpdateOutcome.FAILED)

                            release.versionCode <= from -> {
                                record(UpdateStep.QUERY, UpdateOutcome.OK)
                                _state.update { it.copy(phase = UpdatePhase.UP_TO_DATE) }
                            }

                            else -> {
                                record(UpdateStep.QUERY, UpdateOutcome.OK)
                                _state.update { it.copy(phase = UpdatePhase.FOUND) }
                            }
                        }
                    }

                    is ReleasesResult.RateLimited -> {
                        _state.update { it.copy(rateLimitResetEpochSeconds = result.resetEpochSeconds) }
                        fail(UpdateStep.QUERY, UpdateOutcome.RATE_LIMITED)
                    }

                    is ReleasesResult.NetworkError -> fail(UpdateStep.QUERY, UpdateOutcome.NETWORK_ERROR)
                }
            }
    }

    fun download() {
        val release = _state.value.release ?: return
        if (_state.value.busy) return
        job =
            viewModelScope.launch {
                _state.update { it.copy(phase = UpdatePhase.DOWNLOADING, downloaded = 0, total = release.apk.size) }
                val started = System.currentTimeMillis()
                val apk =
                    try {
                        val checksumText = container.downloader.downloadText(release.checksum)
                        val expected = Sha256Verifier.parseChecksum(checksumText, release.apk.name)
                        val file =
                            container.downloader.download(release.apk) { done, total ->
                                _state.update { it.copy(downloaded = done, total = total) }
                            }
                        val elapsed = System.currentTimeMillis() - started
                        record(UpdateStep.DOWNLOAD, UpdateOutcome.OK, file.length(), elapsed)
                        file to expected
                    } catch (cancelled: CancellationException) {
                        record(UpdateStep.DOWNLOAD, UpdateOutcome.USER_CANCELLED)
                        _state.update { it.copy(phase = UpdatePhase.FOUND, lastStep = UpdateStep.DOWNLOAD) }
                        throw cancelled
                    } catch (_: IOException) {
                        fail(UpdateStep.DOWNLOAD, UpdateOutcome.NETWORK_ERROR)
                        return@launch
                    }
                verify(apk.first, apk.second, release)
            }
    }

    private suspend fun verify(file: File, expectedSha: String?, release: SpikeRelease) {
        val shaOk = expectedSha != null && withContext(Dispatchers.IO) { Sha256Verifier.matches(file, expectedSha) }
        if (!shaOk) return reject(file, UpdateStep.VERIFY_SHA256, UpdateOutcome.MISMATCH)
        record(UpdateStep.VERIFY_SHA256, UpdateOutcome.OK)

        val (candidate, installed) =
            withContext(Dispatchers.IO) { container.inspector.archive(file) to container.inspector.installed() }
        when {
            candidate == null || !isUpgrade(candidate, installed, release) ->
                reject(file, UpdateStep.VERIFY_CERT, UpdateOutcome.FAILED)

            !CertificateMatcher.matches(installed.certificates, candidate.certificates) ->
                reject(file, UpdateStep.VERIFY_CERT, UpdateOutcome.MISMATCH)

            else -> {
                record(UpdateStep.VERIFY_CERT, UpdateOutcome.OK)
                verifiedApk = file
                _state.update { it.copy(phase = UpdatePhase.VERIFIED) }
            }
        }
    }

    private fun isUpgrade(candidate: ApkFacts, installed: ApkFacts, release: SpikeRelease) =
        candidate.packageName == installed.packageName &&
            candidate.versionCode == release.versionCode.toLong() &&
            candidate.versionCode > installed.versionCode

    /** Asks for the install permission if needed, then opens the system confirmation. */
    fun install(activity: Activity) {
        if (verifiedApk == null || _state.value.busy) return
        if (container.installer.canInstall()) {
            viewModelScope.launch {
                record(UpdateStep.INSTALL_PERMISSION, UpdateOutcome.OK)
                startInstall()
            }
            return
        }
        val launch = activity.launchSafely(SettingsIntents.unknownSources(activity.packageName))
        viewModelScope.launch {
            if (launch.outcome == AttemptOutcome.OPENED) {
                _state.update { it.copy(phase = UpdatePhase.NEEDS_PERMISSION) }
            } else {
                fail(UpdateStep.INSTALL_PERMISSION, UpdateOutcome.ACTIVITY_NOT_FOUND)
            }
        }
    }

    private suspend fun startInstall() {
        val apk = verifiedApk ?: return
        _state.update { it.copy(phase = UpdatePhase.INSTALLING) }
        try {
            container.installer.install(apk, from, to)
        } catch (_: IOException) {
            container.installer.takePending()
            fail(UpdateStep.INSTALL, UpdateOutcome.FAILED)
        } catch (_: SecurityException) {
            container.installer.takePending()
            fail(UpdateStep.INSTALL, UpdateOutcome.FAILED)
        }
    }

    private suspend fun onInstallerStatus(status: Int) {
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> Unit

            // Self-update: the process is about to be replaced; the new version records OK on start.
            PackageInstaller.STATUS_SUCCESS -> Unit

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                container.installer.takePending()
                record(UpdateStep.INSTALL, UpdateOutcome.USER_CANCELLED, installerStatus = status)
                _state.update { it.copy(phase = UpdatePhase.VERIFIED) }
            }

            else -> {
                container.installer.takePending()
                fail(UpdateStep.INSTALL, UpdateOutcome.FAILED, installerStatus = status)
            }
        }
    }

    fun requestCancel() = _state.update { it.copy(confirmCancel = it.phase == UpdatePhase.DOWNLOADING) }

    fun dismissCancel() = _state.update { it.copy(confirmCancel = false) }

    fun cancelDownload() {
        _state.update { it.copy(confirmCancel = false) }
        job?.cancel()
    }

    private suspend fun reject(file: File, step: UpdateStep, outcome: UpdateOutcome) {
        container.downloader.discard(file)
        verifiedApk = null
        fail(step, outcome)
    }

    private suspend fun fail(step: UpdateStep, outcome: UpdateOutcome, installerStatus: Int? = null) {
        record(step, outcome, installerStatus = installerStatus)
        _state.update { it.copy(phase = UpdatePhase.FAILED) }
    }

    private suspend fun record(
        step: UpdateStep,
        outcome: UpdateOutcome,
        bytes: Long = 0,
        durationMs: Long = 0,
        installerStatus: Int? = null
    ) {
        _state.update { it.copy(lastStep = step, lastOutcome = outcome) }
        container.recorder.record(
            Payload.Update(UpdateRecord(from, to, step, outcome, bytes, durationMs, installerStatus))
        )
    }
}
