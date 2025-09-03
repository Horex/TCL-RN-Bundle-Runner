package com.tcl_rn_bundle_runner.shims

import com.facebook.react.bridge.*

// Minimal shim for @react-native-camera-roll/camera-roll (RNCCameraRoll)
// Provides method names used by the JS wrapper and returns benign defaults.
@Suppress("UNUSED_PARAMETER")
class RNCCameraRollModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "RNCCameraRoll"

  @ReactMethod
  fun getAlbums(params: ReadableMap?, promise: Promise) {
    val arr = WritableNativeArray() // return []
    promise.resolve(arr)
  }

  @ReactMethod
  fun getPhotos(params: ReadableMap?, promise: Promise) {
    val pageInfo = WritableNativeMap().apply {
      putBoolean("has_next_page", false)
      putString("end_cursor", null)
    }
    val out = WritableNativeMap().apply {
      putArray("edges", WritableNativeArray())
      putMap("page_info", pageInfo)
    }
    promise.resolve(out)
  }

  @ReactMethod
  fun saveToCameraRoll(tag: String?, options: ReadableMap?, promise: Promise) {
    // Just echo the input path/URI back
    promise.resolve(tag)
  }

  // Some versions expose `save` instead of `saveToCameraRoll`
  @ReactMethod
  fun save(tag: String?, options: ReadableMap?, promise: Promise) {
    promise.resolve(tag)
  }

  @ReactMethod
  fun deletePhotos(uris: ReadableArray?, promise: Promise) {
    promise.resolve(true)
  }
}

