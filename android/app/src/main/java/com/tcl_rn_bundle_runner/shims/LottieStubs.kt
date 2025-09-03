package com.tcl_rn_bundle_runner.shims

import android.widget.FrameLayout
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager

// Minimal stubs for lottie-react-native expectations

class LottieAnimationViewModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "LottieAnimationView"
}

class LottieAnimationViewManager : ViewGroupManager<FrameLayout>() {
  override fun getName(): String = "LottieAnimationView"
  override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout = FrameLayout(reactContext)
}

