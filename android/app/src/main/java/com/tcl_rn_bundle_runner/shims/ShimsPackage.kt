package com.tcl_rn_bundle_runner.shims

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import android.widget.FrameLayout

class ShimsPackage : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): MutableList<NativeModule> =
    mutableListOf(
      LocalizationModule(reactContext),
      JsImpModule(reactContext),
      RNCAsyncStorageModule(reactContext),
      RNCWebViewModule(reactContext),
      // CameraRoll
      RNCCameraRollModule(reactContext),
      // Lottie
      LottieAnimationViewModule(reactContext),
    )

  override fun createViewManagers(reactContext: ReactApplicationContext): MutableList<ViewManager<*, *>> = mutableListOf(
    // Lottie view
    LottieAnimationViewManager(),
    // react-native-screens
    RNSScreenManager(),
    RNSScreenContainerManager(),
  )
}