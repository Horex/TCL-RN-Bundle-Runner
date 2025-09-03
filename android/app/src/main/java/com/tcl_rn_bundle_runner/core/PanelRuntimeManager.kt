package com.tcl_rn_bundle_runner.core

import android.app.Application
import android.content.Context
import android.net.Uri
import com.facebook.react.ReactApplication
import com.tcl_rn_bundle_runner.MainApplication
import com.tcl_rn_bundle_runner.shims.ShadowHandler
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PanelRuntimeManager {
  private const val OUT_DIR = "rn_panel"
  @Volatile private var cachedJsPath: String? = null

  fun isBundleInstalled(context: Context): Boolean = getJsBundlePath(context) != null

  fun getJsBundlePath(context: Context): String? {
    val cached = cachedJsPath
    if (cached != null && File(cached).exists()) return cached
    val dir = File(context.filesDir, OUT_DIR)
    if (!dir.exists()) return null
    val js = findJsBundle(dir) ?: return null
    cachedJsPath = js.absolutePath
    return cachedJsPath
  }

  fun installBundleFromUri(context: Context, uri: Uri): Boolean {
    val dir = File(context.filesDir, OUT_DIR)
    if (dir.exists()) dir.deleteRecursively()
    dir.mkdirs()
    var js: File? = null
    try {
      context.contentResolver.openInputStream(uri).use { input ->
        if (input == null) return false
        ZipInputStream(input).use { zis ->
          var entry: ZipEntry? = zis.nextEntry
          while (entry != null) {
            val e = entry!!
            val outFile = File(dir, e.name)
            if (e.isDirectory) {
              outFile.mkdirs()
            } else {
              outFile.parentFile?.mkdirs()
              copy(zis, outFile)
              if (e.name.endsWith(".jsbundle")) js = outFile
            }
            zis.closeEntry()
            entry = zis.nextEntry
          }
        }
      }
    } catch (_: Exception) {
      return false
    }
    if (js == null) return false
    File(dir, ".ok").writeText("ok")
    cachedJsPath = js!!.absolutePath
    return true
  }

  fun refreshMainComponentFromManifest(context: Context) {
    try {
      val dir = File(context.filesDir, OUT_DIR)
      if (!dir.exists()) return
      val manifest = dir.walkTopDown().firstOrNull { it.isFile && it.name == "manifest.json" } ?: return
      val text = manifest.readText()
      val root = JSONObject(text)
      val androidObj = root.optJSONObject("android")
      val pkg = when {
        androidObj?.optString("packageName")?.isNotBlank() == true -> androidObj.getString("packageName")
        root.optString("packageName").isNotBlank() -> root.getString("packageName")
        else -> null
      } ?: return
      val comp = pkg.removePrefix("com.tcl.")
      MainApplication.MAIN_COMPONENT_NAME = comp
    } catch (_: Exception) { }
  }

  fun resetRuntimeState(context: Context) {
    try {
      ShadowHandler.reset()
      context.getSharedPreferences("RNCAsyncStorage", Context.MODE_PRIVATE).edit().clear().apply()
    } catch (_: Exception) { }
  }

  fun hotReloadIfRunning(app: Application) {
    try {
      val rim = (app as ReactApplication).reactNativeHost.reactInstanceManager
      if (rim.currentReactContext != null) rim.recreateReactContextInBackground()
    } catch (_: Exception) { }
  }

  /**
   * Applies an installed bundle by syncing the main component name from the manifest,
   * resetting runtime state, and creating/recreating the RN context with a file bundle.
   * Returns true on success.
   */
  fun applyInstalledBundle(app: Application): Boolean {
    // Ensure MAIN_COMPONENT_NAME reflects the latest manifest
    refreshMainComponentFromManifest(app)
    // Clear runtime state before (re)creating RN context
    resetRuntimeState(app)
    // Ensure RN will load from the installed file bundle and (re)create context
    return forceCreateOrRecreateWithFile(app)
  }

  /**
   * Ensures React will load the installed JS bundle file by forcing the bundle loader to a file
   * loader and creating or recreating the React context as needed. Safe to call multiple times.
   */
  fun forceCreateOrRecreateWithFile(app: Application): Boolean {
    return try {
      val path = getJsBundlePath(app) ?: return false
      val reactApp = app as ReactApplication
      val rim = reactApp.reactNativeHost.reactInstanceManager
      val cls = rim.javaClass

      val jblClass = Class.forName("com.facebook.react.bridge.JSBundleLoader")

      // Try: recreateReactContextInBackgroundFromBundleLoader(JSBundleLoader)
      val recreateWithLoader = try {
        cls.getDeclaredMethod("recreateReactContextInBackgroundFromBundleLoader", jblClass)
      } catch (_: Exception) { null }

      // Build a file loader (try common signatures)
      val loader: Any? = try {
        try {
          // RN <= ~0.71
          val m = jblClass.getMethod("createFileLoader", String::class.java)
          m.invoke(null, path)
        } catch (_: NoSuchMethodException) {
          val m2 = jblClass.getMethod("createFileLoader", java.io.File::class.java)
          m2.invoke(null, java.io.File(path))
        }
      } catch (_: Exception) { null }

      if (recreateWithLoader != null && loader != null) {
        recreateWithLoader.isAccessible = true
        recreateWithLoader.invoke(rim, loader)
        true
      } else {
        if (loader == null) return false
        // Fallback: set private mBundleLoader and (re)create
        val fld = cls.getDeclaredField("mBundleLoader"); fld.isAccessible = true; fld.set(rim, loader)
        if (rim.currentReactContext != null) rim.recreateReactContextInBackground() else rim.createReactContextInBackground()
        true
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun copy(input: java.io.InputStream, outFile: File) {
    FileOutputStream(outFile).use { fos ->
      val buf = ByteArray(64 * 1024)
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
}
