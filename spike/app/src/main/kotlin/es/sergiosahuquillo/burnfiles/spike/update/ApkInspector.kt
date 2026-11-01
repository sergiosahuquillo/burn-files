package es.sergiosahuquillo.burnfiles.spike.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File

data class ApkFacts(val packageName: String, val versionCode: Long, val certificates: Set<String>)

/** Reads package name, version and signing certificates of the installed app and of an archive. */
class ApkInspector(private val context: Context) {
    private val flags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    fun installed(): ApkFacts = facts(context.packageManager.getPackageInfo(context.packageName, flags))

    fun archive(file: File): ApkFacts? =
        context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)?.let(::facts)

    private fun facts(info: PackageInfo) = ApkFacts(
        packageName = info.packageName,
        versionCode = PackageInfoCompat.getLongVersionCode(info),
        certificates = certificates(info)
    )

    private fun certificates(info: PackageInfo): Set<String> {
        val signatures =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signing = info.signingInfo ?: return emptySet()
                if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }
        return signatures.orEmpty().map { CertificateMatcher.fingerprint(it.toByteArray()) }.toSet()
    }
}
