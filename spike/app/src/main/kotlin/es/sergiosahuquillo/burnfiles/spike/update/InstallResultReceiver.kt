package es.sergiosahuquillo.burnfiles.spike.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import es.sergiosahuquillo.burnfiles.spike.SpikeApp
import kotlinx.coroutines.launch

/** Receives the [PackageInstaller] session status and forwards it to the update screen. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val container = (context.applicationContext as SpikeApp).container
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
            if (confirm != null) {
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            container.installEvents.tryEmit(status)
            return
        }
        val pending = goAsync()
        container.appScope.launch {
            try {
                container.installEvents.emit(status)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION = "es.sergiosahuquillo.burnfiles.spike.INSTALL_STATUS"
    }
}
