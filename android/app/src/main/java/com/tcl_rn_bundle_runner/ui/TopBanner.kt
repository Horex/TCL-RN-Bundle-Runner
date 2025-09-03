package com.tcl_rn_bundle_runner.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import com.tcl_rn_bundle_runner.R

object TopBanner {
  private var currentView: View? = null
  fun show(activity: Activity, text: String) {
    val root = activity.findViewById<ViewGroup>(R.id.root_coordinator)
      ?: activity.window?.decorView as? ViewGroup
      ?: return

    // Remove any existing banner immediately
    currentView?.let { existing ->
      try {
        (existing.parent as? ViewGroup)?.removeView(existing)
      } catch (_: Exception) {}
      currentView = null
    }

    val inflater = LayoutInflater.from(activity)
    val banner = inflater.inflate(R.layout.view_top_banner, root, false)
    val tv = banner.findViewById<TextView>(R.id.banner_text)
    val ok = banner.findViewById<Button>(R.id.banner_ok)
    tv.text = text

    banner.layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    banner.translationY = -200f
    root.addView(banner, 0)
    currentView = banner
    banner.animate()
      .translationY(0f)
      .setDuration(200)
      .setInterpolator(AccelerateDecelerateInterpolator())
      .start()

    ok.setOnClickListener {
      banner.animate()
        .translationY(-banner.height.toFloat())
        .setDuration(180)
        .withEndAction {
          try { root.removeView(banner) } catch (_: Exception) {}
          if (currentView === banner) currentView = null
        }
        .start()
    }
  }
}
