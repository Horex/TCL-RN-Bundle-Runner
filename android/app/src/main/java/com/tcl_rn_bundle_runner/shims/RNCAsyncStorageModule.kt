package com.tcl_rn_bundle_runner.shims

import android.content.Context
import com.facebook.react.bridge.*

class RNCAsyncStorageModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  private val prefs = reactApplicationContext.getSharedPreferences("RNCAsyncStorage", Context.MODE_PRIVATE)

  override fun getName(): String = "RNCAsyncStorage"

  @ReactMethod
  fun multiGet(keys: ReadableArray, callback: Callback) {
    val result: WritableArray = WritableNativeArray()
    for (i in 0 until keys.size()) {
      val key: String? = keys.getString(i)
      if (key == null) continue
      val pair: WritableArray = WritableNativeArray()
      pair.pushString(key)
      pair.pushString(prefs.getString(key, null))
      result.pushArray(pair)
    }
    callback.invoke(null, result)
  }

  @ReactMethod
  fun multiSet(kvPairs: ReadableArray, callback: Callback) {
    val editor = prefs.edit()
    for (i in 0 until kvPairs.size()) {
      val pair: ReadableArray? = kvPairs.getArray(i)
      if (pair == null) continue
      if (pair.size() >= 2) {
        val key: String? = pair.getString(0)
        if (key == null) continue
        val value = pair.getString(1)
        editor.putString(key, value)
      }
    }
    editor.apply()
    callback.invoke(null)
  }

  @ReactMethod
  fun multiRemove(keys: ReadableArray, callback: Callback) {
    val editor = prefs.edit()
    for (i in 0 until keys.size()) {
      val key: String? = keys.getString(i)
      if (key == null) continue
      editor.remove(key)
    }
    editor.apply()
    callback.invoke(null)
  }

  @ReactMethod
  fun clear(callback: Callback) {
    prefs.edit().clear().apply()
    callback.invoke(null)
  }

  @ReactMethod
  fun getAllKeys(callback: Callback) {
    val keys: WritableArray = WritableNativeArray()
    for (k in prefs.all.keys) keys.pushString(k)
    callback.invoke(null, keys)
  }
}
