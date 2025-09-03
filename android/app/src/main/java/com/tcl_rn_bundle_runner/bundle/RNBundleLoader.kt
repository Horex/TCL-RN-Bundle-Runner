package com.tcl_rn_bundle_runner.bundle

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Manages the extracted React Native panel bundle on disk.
 *
 * Bundle layout under <filesDir>/rn_panel/ must contain at minimum:
 *  - main.jsbundle
 *  - manifest.json
 *
 * Creation/updates are done via replaceFromZipUri(). This loader does not read assets.
 */
object RNBundleLoader {
  private const val TAG = "RNBundleLoader"
  private const val OUTER_DIR = "rn_panel"

  data class ExtractResult(
    val baseDir: File,
    val jsBundle: File,
  )

  /** Returns absolute file path to main.jsbundle after ensuring extraction. */
  @JvmStatic
  fun getJSBundleFile(context: Context): String? = ensureExtracted(context)?.jsBundle?.absolutePath

  /** Returns base directory where assets (images, etc.) are extracted. */
  @JvmStatic
  fun getAssetsBaseDir(context: Context): File? = ensureExtracted(context)?.baseDir

  /** Builds a file:// Uri for an extracted asset relative path (e.g., "drawable-xxhdpi/icon.png"). */
  @JvmStatic
  fun buildAssetUri(context: Context, relativePath: String): Uri? {
    val base = getAssetsBaseDir(context) ?: return null
    val f = File(base, relativePath.removePrefix("/"))
    return if (f.exists()) Uri.fromFile(f) else null
  }

  /**
   * Replaces the extracted panel with the contents of the provided ZIP Uri.
   * The ZIP must contain a .jsbundle file at any path. Extracts into
   * <filesDir>/rn_panel/ and writes an .ok marker on success.
   */
  @JvmStatic
  fun replaceFromZipUri(context: Context, zipUri: Uri): Boolean {
    val cr = context.contentResolver
    val targetDir = File(context.filesDir, OUTER_DIR)
    var jsBundle: File? = null
    try {
      cr.openInputStream(zipUri).use { input ->
        if (input == null) return false
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
        ZipInputStream(input).use { zis ->
          var entry: ZipEntry? = zis.nextEntry
          while (entry != null) {
            val name = entry.name
            val outFile = File(targetDir, name)
            if (entry.isDirectory) {
              outFile.mkdirs()
            } else {
              outFile.parentFile?.mkdirs()
              copyStream(zis, outFile)
              if (name.endsWith(".jsbundle")) jsBundle = outFile
            }
            zis.closeEntry()
            entry = zis.nextEntry
          }
        }
      }
    } catch (e: Exception) {
      return false
    }
    if (jsBundle == null) return false
    File(targetDir, ".ok").writeText("ok")
    return true
  }

  /**
   * Returns the valid extracted bundle directory and jsbundle if present; null otherwise.
   * Never reads from packaged assets.
   */
  @Synchronized
  fun ensureExtracted(context: Context): ExtractResult? {
    val targetDir = File(context.filesDir, OUTER_DIR)
    val marker = File(targetDir, ".ok")
    if (targetDir.exists() && marker.exists()) {
      findJsBundle(targetDir)?.let { return ExtractResult(targetDir, it) }
    }
    return null
  }

  private fun copyStream(input: InputStream, outFile: File) {
    FileOutputStream(outFile).use { fos ->
      val buf = ByteArray(DEFAULT_BUFFER_SIZE)
      while (true) {
        val r = input.read(buf)
        if (r <= 0) break
        fos.write(buf, 0, r)
      }
      fos.flush()
    }
  }

  private fun findJsBundle(dir: File): File? =
    dir.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".jsbundle") }

  /**
   * Reads manifest.json from the extracted panel ZIP and returns the RN main component name,
   * derived from the packageName by removing the exact "com.tcl." prefix and any trailing commas.
   * Example: packageName "com.tcl.panel_smart_ac_overseas," -> "panel_smart_ac_overseas"
   */
  @JvmStatic
  fun detectMainComponentFromManifest(context: Context): String? {
    val extracted = ensureExtracted(context) ?: return null
    val manifest = extracted.baseDir.walkTopDown().firstOrNull { it.isFile && it.name == "manifest.json" }
      ?: return null
    return try {
      val text = manifest.readText()
      val root = JSONObject(text)
      // Prefer android.packageName, then ios.packageName, then top-level packageName (if any)
      val raw = when {
        root.optJSONObject("android")?.optString("packageName")?.isNotBlank() == true ->
          root.getJSONObject("android").getString("packageName")
        root.optJSONObject("ios")?.optString("packageName")?.isNotBlank() == true ->
          root.getJSONObject("ios").getString("packageName")
        root.optString("packageName").isNotBlank() -> root.getString("packageName")
        else -> ""
      }.trim()
      if (raw.isEmpty()) return null
      // Strip trailing punctuation (e.g., comma) and whitespace
      val cleaned = raw.trim().trimEnd(',', ';', ' ')
      // Remove the exact prefix "com.tcl." if present
      val withoutPrefix = cleaned.removePrefix("com.tcl.")
      // Result should be the component name
      withoutPrefix
    } catch (e: Exception) {
      null
    }
  }

  @JvmStatic
  fun readAndroidManifestSection(context: Context): Map<String, String>? {
    val extracted = ensureExtracted(context) ?: return null
    val manifest = extracted.baseDir.walkTopDown().firstOrNull { it.isFile && it.name == "manifest.json" }
      ?: return null
    return try {
      val text = manifest.readText()
      val root = JSONObject(text)
      val androidObj = root.optJSONObject("android") ?: return null
      val out = mutableMapOf<String, String>()
      val it = androidObj.keys()
      while (it.hasNext()) {
        val k = it.next()
        out[k] = androidObj.optString(k, "")
      }
      out
    } catch (e: Exception) {
      null
    }
  }
}
