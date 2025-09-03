package com.tcl_rn_bundle_runner.ui

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import org.json.JSONArray
import org.json.JSONObject

object PayloadNotifier {
  fun showPayload(activity: Activity?, payload: ReadableArray?) {
    val text = toPrettyJson(payload)
    Handler(Looper.getMainLooper()).post {
      val act = activity ?: return@post
      TopBanner.show(act, text)
    }
  }

  fun toPrettyJson(arr: ReadableArray?): String {
    if (arr == null) return "null"
    return jsonArrayPrettyInlineObjects(toJsonArray(arr))
  }

  private fun jsonArrayPrettyInlineObjects(ja: JSONArray): String {
    val sb = StringBuilder()
    sb.append("[\n")
    for (i in 0 until ja.length()) {
      val v = ja.get(i)
      val line = when (v) {
        is JSONObject -> v.toString()
        is JSONArray -> v.toString()
        JSONObject.NULL -> "null"
        is String -> stringToJson(v)
        is Number, is Boolean -> v.toString()
        else -> JSONObject.wrap(v).toString()
      }
      sb.append("  ").append(line)
      if (i < ja.length() - 1) sb.append(",")
      sb.append("\n")
    }
    sb.append("]")
    return sb.toString()
  }

  private fun toJsonArray(arr: ReadableArray): JSONArray {
    val ja = JSONArray()
    for (i in 0 until arr.size()) {
      ja.put(toJsonValue(arr.getType(i), arr, i))
    }
    return ja
  }

  private fun toJsonObject(map: ReadableMap): JSONObject {
    val jo = JSONObject()
    val it = map.keySetIterator()
    while (it.hasNextKey()) {
      val k = it.nextKey()
      val t = map.getType(k)
      val v: Any? = when (t) {
        ReadableType.Null -> JSONObject.NULL
        ReadableType.Boolean -> map.getBoolean(k)
        ReadableType.Number -> map.getDouble(k)
        ReadableType.String -> map.getString(k)
        ReadableType.Map -> map.getMap(k)?.let { toJsonObject(it) } ?: JSONObject.NULL
        ReadableType.Array -> map.getArray(k)?.let { toJsonArray(it) } ?: JSONObject.NULL
      }
      jo.put(k, v)
    }
    return jo
  }

  private fun toJsonValue(type: ReadableType, arr: ReadableArray, index: Int): Any? = when (type) {
    ReadableType.Null -> JSONObject.NULL
    ReadableType.Boolean -> arr.getBoolean(index)
    ReadableType.Number -> arr.getDouble(index)
    ReadableType.String -> arr.getString(index)
    ReadableType.Map -> toJsonObject(arr.getMap(index)!!)
    ReadableType.Array -> toJsonArray(arr.getArray(index)!!)
  }

  private fun stringToJson(s: String): String = "\"${escape(s)}\""
  private fun escape(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
