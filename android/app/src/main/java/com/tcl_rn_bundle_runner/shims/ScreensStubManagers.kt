package com.tcl_rn_bundle_runner.shims

import android.widget.FrameLayout
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager

// Minimal stubs for react-native-screens

class RNSScreenManager : ViewGroupManager<FrameLayout>() {
  override fun getName(): String = "RNSScreen"
  override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout = FrameLayout(reactContext)
}

class RNSScreenContainerManager : ViewGroupManager<FrameLayout>() {
  override fun getName(): String = "RNSScreenContainer"
  override fun createViewInstance(reactContext: ThemedReactContext): FrameLayout = FrameLayout(reactContext)
}

