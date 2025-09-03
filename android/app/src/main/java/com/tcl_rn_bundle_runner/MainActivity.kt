package com.tcl_rn_bundle_runner

import android.app.Activity
import android.content.Intent
import android.app.ActivityManager
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.TextView
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import com.tcl_rn_bundle_runner.ui.Toasts
import org.json.JSONObject
import com.tcl_rn_bundle_runner.core.PanelRuntimeManager


class MainActivity : Activity() {
  companion object {
    private const val REQ_PICK_ZIP = 1001
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launcher)

    findViewById<View>(R.id.start_button).setOnClickListener {
      val jsPath = PanelRuntimeManager.getJsBundlePath(this)
      if (jsPath == null || !java.io.File(jsPath).exists()) {
        Toasts.show(this, "No bundle installed. Pick a ZIP first.")
        return@setOnClickListener
      }
      startActivity(Intent(this, PanelActivity::class.java))
    }

    findViewById<View>(R.id.pick_zip_button).setOnClickListener {
      openZipPicker()
    }

    refreshBundleInfo()
  }

  private fun openZipPicker() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      type = "application/zip"
      putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://"))
    }
    startActivityForResult(intent, REQ_PICK_ZIP)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQ_PICK_ZIP && resultCode == RESULT_OK) {
      val uri = data?.data ?: return
      // Persist read permission if available
      try {
        val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
      } catch (_: Exception) { }

      val ok = PanelRuntimeManager.installBundleFromUri(this, uri)
      if (ok) {
        // Apply the installed bundle in a single, consistent step
        val applied = PanelRuntimeManager.applyInstalledBundle(application)
        refreshBundleInfo()
        Toasts.show(this, if (applied) "Bundle replaced." else "Bundle installed, apply failed")
        // Stay on launcher; user can press Start to open the panel
      } else {
        Toasts.show(this, "Failed to replace bundle")
      }
    }
  }

  override fun onResume() {
    super.onResume()
    refreshBundleInfo()
  }

  private fun refreshBundleInfo() {
    val infoView = findViewById<TextView>(R.id.bundle_info)
    val path = PanelRuntimeManager.getJsBundlePath(this)
    if (path == null) {
      infoView.text = "No bundle installed"
      return
    }
    val androidMap = try {
      val dir = java.io.File(filesDir, "rn_panel")
      val manifest = dir.walkTopDown().firstOrNull { it.isFile && it.name == "manifest.json" }
      if (manifest != null) org.json.JSONObject(manifest.readText()).optJSONObject("android")?.let { obj ->
        val out = mutableMapOf<String, String>()
        val it = obj.keys()
        while (it.hasNext()) {
          val k = it.next()
          out[k] = obj.optString(k, "")
        }
        out
      } else null
    } catch (e: Exception) { null }
    if (androidMap == null) {
      infoView.text = "manifest.json missing android section"
      return
    }
    val items = androidMap.entries
      .asSequence()
      .filter { it.value.isNotBlank() }
      .sortedBy { it.key.lowercase() }
      .toList()
    val sb = SpannableStringBuilder()
    items.forEachIndexed { idx, (k, v) ->
      val line = "$k: $v"
      val start = sb.length
      sb.append(line)
      // Bold the field name (before the colon)
      sb.setSpan(StyleSpan(Typeface.BOLD), start, start + k.length, 0)
      if (idx < items.size - 1) sb.append('\n')
    }
    infoView.text = sb
  }
}
