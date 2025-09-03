package com.tcl_rn_bundle_runner.shims

import android.util.Log
import com.facebook.react.bridge.*
import com.tcl_rn_bundle_runner.bundle.RNBundleLoader
import com.tcl_rn_bundle_runner.core.LanguageHelper
import com.tcl_rn_bundle_runner.ui.PayloadNotifier
import org.json.JSONObject

class JsImpModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "JsImpModule"

  @ReactMethod
  fun getDeviceShadow(deviceId: String?, promise: Promise) {
    try {
      promise.resolve(ShadowHandler.getDeviceShadowPayload(deviceId))
    } catch (e: Exception) {
      promise.reject("ERR_DEVICE_SHADOW", e)
    }
  }

  @ReactMethod
  fun sendMessage(path: String?, payload: ReadableArray?) {
    val p = path ?: return
    val payloadJson = try { PayloadNotifier.toPrettyJson(payload) } catch (e: Exception) { payload?.toString() ?: "null" }
    Log.i("TCL_AWS_THING", "SEND path=$p payload=$payloadJson")
    try { PayloadNotifier.showPayload(currentActivity, payload) } catch (_: Exception) { }
    if (p.contains("shadow/update")) {
      ShadowHandler.applyDesiredUpdatesFromReadableArray(reactApplicationContext, payload)
    }
  }


  @ReactMethod
  fun getStateInfo(arg1: String?, arg2: String?, promise: Promise) {
    try {
      val deviceId = "debug-device-1"

      // Try to read values from manifest.json in the extracted panel bundle
      val manifestJson: JSONObject? = try {
        val base = RNBundleLoader.getAssetsBaseDir(reactApplicationContext)
        val mf = base?.walkTopDown()?.firstOrNull { it.isFile && it.name == "manifest.json" }
        if (mf != null) JSONObject(mf.readText()) else null
      } catch (e: Exception) { null }

      val androidObj = manifestJson?.optJSONObject("android")
      val productKey = listOfNotNull(
        androidObj?.optString("productKey"),
        manifestJson?.optString("productKey")
      ).firstOrNull { !it.isNullOrBlank() } ?: "XXXXXXXXXXXXXXXX"

      val panelPackageName = listOfNotNull(
        androidObj?.optString("packageName"),
        manifestJson?.optString("packageName")
      ).firstOrNull { !it.isNullOrBlank() } ?: "com.tcl.XXXXXXXXXXXXXXXX"

      val panelVersion = listOfNotNull(
        androidObj?.optString("rnVersion"),
        manifestJson?.optString("rnVersion")
      ).firstOrNull { !it.isNullOrBlank() } ?: "3.0.6"

      val appLang = LanguageHelper.languageOnly()
      val sysLangCode = LanguageHelper.languageCode()

      val values = mapOf(
        "productKey" to productKey,
        "deviceId" to deviceId,
        "serverHost" to "https://example.invalid",
        "userId" to "debug-user",
        "masterId" to "debug-master",
        "timeZone" to "UTC",
        "appLanguage" to appLang,
        "systemLanguage" to sysLangCode,
        "countryCode" to LanguageHelper.countryCode(),
        "appVersion" to "9.9.9",
        "firmwareVersion" to "0.0.0",
        "newFirmwareVersionAvailable" to "",
        "systemVersion" to (android.os.Build.VERSION.RELEASE ?: ""),
        "appPackageName" to reactApplicationContext.packageName,
        "brand" to (android.os.Build.BRAND ?: "Android"),
        "sdkVersion" to "0.0.0",
        "mqttStatus" to 1,
        "deviceOnlineStatus" to 1,
        "accessToken" to "debug-token",
        "panelVersion" to panelVersion,
        "panelPackageName" to panelPackageName,
        "deviceName" to "Demo AC"
      )
      val keyCandidate1 = arg1?.takeIf { !it.isNullOrBlank() && values.containsKey(it) }
      val keyCandidate2 = arg2?.takeIf { !it.isNullOrBlank() && values.containsKey(it) }
      val key = keyCandidate1 ?: keyCandidate2
      if (key != null) {
        promise.resolve(values[key])
      } else {
        val jsStr = "{" +
          values.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" } +
          ",\"deviceJson\":\"{\\\"productKey\\\":\\\"$productKey\\\",\\\"deviceId\\\":\\\"$deviceId\\\"}\"" +
          "}"
        promise.resolve(jsStr)
      }
    } catch (e: Exception) {
      promise.reject("ERR_STATE_INFO_PROMISE", e)
    }
  }

  @ReactMethod
  fun getSystemLanguage(promise: Promise) {
    try {
      promise.resolve(LanguageHelper.languageCode())
    } catch (e: Exception) {
      promise.reject("ERR_SYSTEM_LANGUAGE", e)
    }
  }

  @ReactMethod
  fun triggerAppAction(actionName: String?, params: ReadableMap?) {
    val act = actionName?.trim().orEmpty()
    when (act) {
      "closeRNActivity" -> currentActivity?.finish()
      // Placeholders for compatibility with vendor SDK; no-ops for now
      "stopLoadingAnimation", "refreshToken", "startActivity", "openIOTDeviceShare", "openDeviceSharePage" -> {
        // no-op
      }
      else -> {
        // no-op for unknown actions
      }
    }
  }
}
