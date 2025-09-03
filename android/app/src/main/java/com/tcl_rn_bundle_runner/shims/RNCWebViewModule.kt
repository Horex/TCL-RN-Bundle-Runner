package com.tcl_rn_bundle_runner.shims

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class RNCWebViewModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "RNCWebView"

  @ReactMethod
  fun isFileUploadSupported(promise: Promise) {
    promise.resolve(true)
  }
}

