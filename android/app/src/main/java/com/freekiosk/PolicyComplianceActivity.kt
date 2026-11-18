package com.freekiosk

import android.app.Activity
import android.os.Bundle
import android.util.Log


class PolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Confirm to the setup wizard before anything that could fail.
        setResult(RESULT_OK)

        try {
        } catch (e: Exception) {
            Log.e(TAG, "Could not store the pending enrollment: ${e.message}", e)
        }

        // Pin FreeKiosk as Home now, while still inside provisioning, so the
        // "choose launcher" picker never appears once the device reaches home.
        // Logs its own failures; never throws.
        DeviceAdminReceiver.pinHomeLauncher(this)

        finish()
    }

    private companion object {
        const val TAG = "FKPolicyCompliance"
    }
}
