package es.sergiosahuquillo.burnfiles.spike.device

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeviceDetector(private val context: Context) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    /** Cheap and I/O-free: safe to call while choosing the UI layer in onCreate. */
    fun uiMode(): UiMode {
        val uiModeManager = context.getSystemService(UiModeManager::class.java)
        val isTv =
            uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return if (isTv) UiMode.TV else UiMode.MOBILE
    }

    suspend fun matrixId(): MatrixId? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_MATRIX_ID, null)?.let { stored ->
            MatrixId.entries.firstOrNull { it.name == stored }
        }
    }

    suspend fun saveMatrixId(id: MatrixId) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_MATRIX_ID, id.name).commit()
    }

    suspend fun detect(): DeviceInfo = DeviceInfo(
        matrixId = matrixId() ?: MatrixId.OTHER,
        manufacturer = Build.MANUFACTURER.take(MAX_FIELD),
        model = Build.MODEL.take(MAX_FIELD),
        sdkInt = Build.VERSION.SDK_INT,
        release = Build.VERSION.RELEASE.take(MAX_RELEASE),
        fireOs = fireOsVersion(Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT),
        uiMode = uiMode()
    )

    companion object {
        private const val PREFS = "device"
        private const val KEY_MATRIX_ID = "matrix_id"
        private const val MAX_FIELD = 64
        private const val MAX_RELEASE = 16

        /**
         * Fire TV devices are Amazon models starting with "AFT". Fire OS has no public version API,
         * so it is derived from the Android level it is based on (Fire OS 7 = API 28, 8 = API 30).
         */
        fun fireOsVersion(manufacturer: String, model: String, sdkInt: Int): String? {
            if (!manufacturer.equals("Amazon", ignoreCase = true) || !model.startsWith("AFT")) return null
            return when (sdkInt) {
                Build.VERSION_CODES.N_MR1 -> "6"
                Build.VERSION_CODES.P -> "7"
                Build.VERSION_CODES.R -> "8"
                Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "14"
                else -> "unknown-api-$sdkInt"
            }
        }
    }
}
