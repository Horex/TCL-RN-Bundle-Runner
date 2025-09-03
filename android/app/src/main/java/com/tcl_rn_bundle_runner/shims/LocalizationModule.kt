package com.tcl_rn_bundle_runner.shims

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.tcl_rn_bundle_runner.core.LanguageHelper

class LocalizationModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  override fun getName(): String = "ReactLocalization"

  override fun getConstants(): MutableMap<String, Any> =
    mutableMapOf<String, Any>("language" to LanguageHelper.languageOnly())
}
