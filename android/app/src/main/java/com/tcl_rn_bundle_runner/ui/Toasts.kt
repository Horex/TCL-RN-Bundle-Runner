package com.tcl_rn_bundle_runner.ui

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.tcl_rn_bundle_runner.R

object Toasts {
  fun show(context: Context, text: String, long: Boolean = false) {
    val v = LayoutInflater.from(context).inflate(R.layout.toast_with_icon, null)
    v.findViewById<ImageView>(R.id.toast_icon).setImageResource(R.mipmap.ic_launcher)
    v.findViewById<TextView>(R.id.toast_text).text = text

    Toast(context).apply {
      duration = if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
      view = v
      setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (48 * context.resources.displayMetrics.density).toInt())
      show()
    }
  }
}

