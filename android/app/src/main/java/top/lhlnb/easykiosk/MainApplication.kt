package top.lhlnb.easykiosk

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import top.lhlnb.easykiosk.printing.SilentPrintPackage

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here
          add(KioskPackage())
          add(CertificatePackage())
          add(MotionDetectionPackage())
          add(AppLauncherPackage())
          add(OverlayPermissionPackage())
          add(LauncherPackage())
          add(OverlayServicePackage())
          add(SystemInfoPackage())
          add(UpdatePackage())
          add(SoundPlayerPackage())
          add(object : com.facebook.react.ReactPackage {
            override fun createNativeModules(context: com.facebook.react.bridge.ReactApplicationContext): List<com.facebook.react.bridge.NativeModule> = listOf(DeviceUtilitiesModule(context), EmbeddedChromiumModule(context))
            override fun createViewManagers(context: com.facebook.react.bridge.ReactApplicationContext): List<com.facebook.react.uimanager.ViewManager<*, *>> = emptyList()
          })
          add(BlockingOverlayPackage())
          add(AutoBrightnessPackage())
          add(PrintPackage())
          add(SilentPrintPackage())
          add(AccessibilityPackage())
          add(FilePickerPackage())
          add(MediaCachePackage())
          add(WifiControlPackage())
          add(BluetoothControlPackage())
          add(AudioControlPackage())
          add(FlashlightPackage())
          add(RotationControlPackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    // Isolated renderers only load Chromium; they must not initialize RN or read app data.
    if (EmbeddedChromium.isIsolatedProcess()) return
    // Direct-boot receivers and lock activities must not touch credential-protected WebView data.
    // MainActivity starts the engine only when its React surface is actually requested.
    loadReactNative(this)
  }
}
