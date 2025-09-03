package com.tcl_rn_bundle_runner.core

import java.util.Locale

/**
 * Central helper for language/locale values used by the runner.
 */
object LanguageHelper {
  /** Two-letter language code from the current system locale (lowercase), e.g., "en". */
  @JvmStatic
  fun languageOnly(locale: Locale = Locale.getDefault()): String = (locale.language ?: "").lowercase()

  /**
   * Language code used by the vendor bundles.
   * Returns "zh-TW" for Traditional Chinese; otherwise the two-letter language code.
   */
  @JvmStatic
  fun languageCode(locale: Locale = Locale.getDefault()): String =
    if ((locale.language ?: "").equals("zh", ignoreCase = true) && (locale.country ?: "").equals("TW", ignoreCase = true))
      "zh-TW" else (locale.language ?: "").lowercase()

  /** RFC 5646-like tag, e.g., "en-US". */
  @JvmStatic
  fun localeTag(locale: Locale = Locale.getDefault()): String = locale.toLanguageTag()

  /** Uppercase country code, e.g., "US". */
  @JvmStatic
  fun countryCode(locale: Locale = Locale.getDefault(), defaultCode: String = "US"): String =
    (locale.country ?: "").uppercase().ifEmpty { defaultCode }
}
