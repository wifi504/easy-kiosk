package top.lhlnb.easykiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log


// The supertype is spelled out rather than imported on purpose. An explicit
// import outranks a same-package declaration in Kotlin, so
// `import android.app.admin.DeviceAdminReceiver` made every
// `DeviceAdminReceiver::class.java` in this file resolve to the *framework*
// class: pinHomeLauncher then handed DevicePolicyManager
// top.lhlnb.easykiosk/android.app.admin.DeviceAdminReceiver, which it rejected with
// "does not exist or is not owned by uid". The same call from KioskModule always
// worked, because no other file carries that import.
class DeviceAdminReceiver : android.app.admin.DeviceAdminReceiver() {

    companion object {
        private const val TAG = "FKDeviceAdmin"

        /**
         * Pin FreeKiosk as the persistent Home launcher (Device Owner only).
         * Called during provisioning so the "choose launcher" prompt never shows:
         * by the time the device reaches the home screen, FreeKiosk is already the
         * locked default. No-op if not Device Owner. The JS/config layer may also
         * toggle this later via KioskModule.setDefaultLauncherMode.
         *
         * Failures are logged rather than swallowed: a silent catch here is why a
         * device reaching the stock launcher left no trace to diagnose.
         */
        fun pinHomeLauncher(context: Context) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                if (!dpm.isDeviceOwnerApp(context.packageName)) {
                    Log.w(TAG, "Not Device Owner — cannot pin FreeKiosk as Home.")
                    return
                }
                val admin = ComponentName(context, DeviceAdminReceiver::class.java)
                val filter = IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
                dpm.addPersistentPreferredActivity(
                    admin, filter, ComponentName(context, MainActivity::class.java)
                )
                Log.i(TAG, "FreeKiosk pinned as the persistent Home launcher.")
            } catch (e: Exception) {
                Log.e(TAG, "Could not pin FreeKiosk as Home: ${e.message}", e)
            }
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        // Older Android provisioning path (< API 31). Pin the launcher now so the
        // picker never appears when the device reaches home.
        pinHomeLauncher(context)
    }
}
