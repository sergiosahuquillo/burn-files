package es.sergiosahuquillo.burnfiles.spike

import android.app.Application
import android.os.Environment
import es.sergiosahuquillo.burnfiles.spike.access.AccessChecker
import es.sergiosahuquillo.burnfiles.spike.access.AccessMonitor
import es.sergiosahuquillo.burnfiles.spike.access.MediaCoverageProbe
import es.sergiosahuquillo.burnfiles.spike.benchmark.BenchmarkRunner
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetCleaner
import es.sergiosahuquillo.burnfiles.spike.benchmark.DatasetGenerator
import es.sergiosahuquillo.burnfiles.spike.device.DeviceDetector
import es.sergiosahuquillo.burnfiles.spike.log.Payload
import es.sergiosahuquillo.burnfiles.spike.log.Recorder
import es.sergiosahuquillo.burnfiles.spike.log.ResultsExporter
import es.sergiosahuquillo.burnfiles.spike.log.ResultsLog
import es.sergiosahuquillo.burnfiles.spike.update.ApkDownloader
import es.sergiosahuquillo.burnfiles.spike.update.ApkInspector
import es.sergiosahuquillo.burnfiles.spike.update.GitHubReleasesClient
import es.sergiosahuquillo.burnfiles.spike.update.SpikeInstaller
import es.sergiosahuquillo.burnfiles.spike.update.UpdateOutcome
import es.sergiosahuquillo.burnfiles.spike.update.UpdateRecord
import es.sergiosahuquillo.burnfiles.spike.update.UpdateStep
import es.sergiosahuquillo.burnfiles.spike.usb.DirectProbe
import es.sergiosahuquillo.burnfiles.spike.usb.DocumentTreeProbe
import es.sergiosahuquillo.burnfiles.spike.usb.VolumeLister
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class SpikeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch { container.onAppStart() }
    }
}

/** Manual dependency container (plan, *Complexity Tracking*: no Hilt in the spike). */
class AppContainer(val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val device = DeviceDetector(app)
    val log = ResultsLog(app.filesDir)
    val recorder = Recorder(device, log, BuildConfig.VERSION_CODE)
    val exporter = ResultsExporter(app, log)

    val accessChecker = AccessChecker(app)
    val access = AccessMonitor(accessChecker)
    val mediaCoverage = MediaCoverageProbe(app.contentResolver)

    @Suppress("DEPRECATION")
    val storageRoot: File = Environment.getExternalStorageDirectory()
    val spikeRoot = File(storageRoot, DatasetCleaner.SPIKE_DIR)
    val datasetRoot = File(spikeRoot, DatasetCleaner.DATASET_DIR)
    val generator = DatasetGenerator(datasetRoot)
    val cleaner = DatasetCleaner(datasetRoot)
    val benchmark = BenchmarkRunner(storageRoot, datasetRoot, app.contentResolver, access, recorder)

    val releases = GitHubReleasesClient()
    val downloader = ApkDownloader(File(app.cacheDir, "updates"))
    val inspector = ApkInspector(app)
    val installer = SpikeInstaller(app)

    /** `PackageInstaller` status codes forwarded by `InstallResultReceiver`. */
    val installEvents = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    val volumes = VolumeLister(app)
    val directProbe = DirectProbe()
    val documentTreeProbe = DocumentTreeProbe(app)

    suspend fun onAppStart() {
        log.load()
        access.peek()
        downloader.clear()
        recordFinishedSelfUpdate()
    }

    /** A self-update kills the process; its success is recorded by the new version on start. */
    private suspend fun recordFinishedSelfUpdate() {
        val (from, to) = installer.takePending() ?: return
        val outcome = if (BuildConfig.VERSION_CODE == to) UpdateOutcome.OK else UpdateOutcome.FAILED
        recorder.record(Payload.Update(UpdateRecord(from, to, UpdateStep.INSTALL, outcome)))
    }
}
