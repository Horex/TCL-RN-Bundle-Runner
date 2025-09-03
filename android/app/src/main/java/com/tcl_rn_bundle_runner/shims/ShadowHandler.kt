package com.tcl_rn_bundle_runner.shims

import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * Encapsulates device shadow state and update/ack handling used by vendor RN bundles.
 */
object ShadowHandler {
  private const val TAG = "TCL_AWS_THING"

  // In-memory runtime shadow state (not persisted)
  private var shadowVersion: Int = 1
  private val reported: MutableMap<String, Any> = initialReported()
  private val desired: MutableMap<String, Any> = initialDesired()

  private fun initialReported(): MutableMap<String, Any> = mutableMapOf(
    "powerSwitch" to 0,
    "workMode" to 1,
    "windSpeed" to 3,
    "temperatureType" to 0,
    "currentTemperature" to 26,
    "targetTemperature" to 26,
    "swingWind" to 0,
    "sleep" to 0,
    "errorCodeArr" to emptyList<Any>()
  )

  private fun initialDesired(): MutableMap<String, Any> = mutableMapOf(
    "powerSwitch" to 0,
    "workMode" to 1,
    "windSpeed" to 3,
    "targetTemperature" to 26
  )

  fun reset() {
    shadowVersion = 1
    reported.clear(); reported.putAll(initialReported())
    desired.clear(); desired.putAll(initialDesired())
  }

  // Public API

  fun getDeviceShadowPayload(deviceId: String?): String {
    val dev = deviceId ?: "<null>"
    val payload = "{" +
      "\"state\": { " +
        "\"reported\": ${mapToJsonString(reported)}, " +
        "\"desired\": ${mapToJsonString(desired)} " +
      "}, " +
      "\"metadata\": {}, \"version\": $shadowVersion }"
    Log.i(TAG, "GET \$aws/things/$dev/shadow -> $payload")
    return payload
  }

  /**
   * Applies desired updates provided as an array of maps (RN ReadableArray), updates state,
   * increments version and emits an amazon-accepted ack event to JS if anything changed.
   */
  fun applyDesiredUpdatesFromReadableArray(reactContext: ReactApplicationContext, payload: ReadableArray?) {
    val updates = readableArrayToMapList(payload)
    if (updates.isEmpty()) return
    val changed = mutableMapOf<String, Any>()
    for ((k, v) in flattenDesiredEntries(updates)) {
      when (k) {
        "targetCelsiusDegree" -> {
          val intVal = (v as? Number)?.toInt() ?: continue
          desired["targetTemperature"] = intVal
          reported["targetTemperature"] = intVal
          changed["targetTemperature"] = intVal
        }
        else -> {
          desired[k] = v
          reported[k] = v
          changed[k] = v
        }
      }
    }
    if (changed.isNotEmpty()) {
      shadowVersion += 1
      emitAmazonAcceptedAck(reactContext, changed)
    }
  }

  // internals

  private fun emitAmazonAcceptedAck(reactContext: ReactApplicationContext, changes: Map<String, Any>) {
    try {
      val reportedJson = StringBuilder().apply { append("{") }
      var first = true
      for ((k, v) in changes) {
        if (!first) reportedJson.append(',')
        first = false
        reportedJson.append('"').append(k).append('"').append(':')
        when (v) {
          is Boolean -> reportedJson.append(if (v) "true" else "false")
          is Int, is Long, is Short, is Byte -> reportedJson.append(v.toString())
          is Double, is Float -> reportedJson.append(v.toString())
          is Number -> reportedJson.append(v.toString())
          else -> {
            val s = v.toString().replace("\\", "\\\\").replace("\"", "\\\"")
            reportedJson.append('"').append(s).append('"')
          }
        }
      }
      reportedJson.append("}")
      val deviceId = "debug-device-1"
      val body = "{\"current\":{\"state\":{\"reported\":${reportedJson.toString()}},\"version\":$shadowVersion},\"clientToken\":\"device_${deviceId}\"}"
      val wrapper = Arguments.createMap().apply {
        putString("topic", "\$aws/things/$deviceId/shadow/get/accepted")
        putString("msgBody", body)
      }
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit("onRemoteMessage", wrapper)
      Log.i(TAG, "EMIT onRemoteMessage amazon-accepted: $body")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to emit amazon-accepted ack", e)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun mapToJsonString(map: Map<String, Any>): String {
    val parts = mutableListOf<String>()
    map.forEach { (k, v) ->
      val valueStr = when (v) {
        is String -> "\"$v\""
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> mapToJsonString(v as Map<String, Any>)
        is List<*> -> listToJsonString(v)
        else -> "null"
      }
      parts.add("\"$k\":$valueStr")
    }
    return "{" + parts.joinToString(",") + "}"
  }

  @Suppress("UNCHECKED_CAST")
  private fun listToJsonString(list: List<*>): String {
    val parts = list.map { v ->
      when (v) {
        is String -> "\"$v\""
        is Number, is Boolean -> v.toString()
        is Map<*, *> -> mapToJsonString(v as Map<String, Any>)
        is List<*> -> listToJsonString(v)
        else -> "null"
      }
    }
    return "[" + parts.joinToString(",") + "]"
  }

  private fun readableArrayToMapList(arr: ReadableArray?): List<Map<String, Any>> {
    if (arr == null) return emptyList()
    val out = mutableListOf<Map<String, Any>>()
    for (i in 0 until arr.size()) {
      val m = arr.getMap(i)
      out.add(readableMapToMap(m))
    }
    return out
  }

  private fun readableMapToMap(m: ReadableMap): Map<String, Any> {
    val it = m.keySetIterator()
    val out = mutableMapOf<String, Any>()
    while (it.hasNextKey()) {
      val k = it.nextKey()
      when (m.getType(k)) {
        ReadableType.Null -> out[k] = ""
        ReadableType.Boolean -> out[k] = m.getBoolean(k)
        ReadableType.Number -> {
          val d = m.getDouble(k)
          out[k] = if (d % 1.0 == 0.0) d.toInt() else d
        }
        ReadableType.String -> out[k] = m.getString(k) ?: ""
        ReadableType.Map -> out[k] = readableMapToMap(m.getMap(k)!!)
        ReadableType.Array -> out[k] = readableArrayToMapList(m.getArray(k)!!)
      }
    }
    return out
  }

  private fun flattenDesiredEntries(entries: List<Map<String, Any>>): List<Pair<String, Any>> {
    val out = mutableListOf<Pair<String, Any>>()
    for (u in entries) {
      if (u.containsKey("state")) {
        val state = u["state"]
        val desiredMap = (state as? Map<*, *>)?.get("desired") as? Map<*, *>
        if (desiredMap != null) {
          for ((kAny, vAny) in desiredMap) {
            val k = kAny as? String ?: continue
            out.add(k to (vAny as Any))
          }
          continue
        }
      }
      for ((k, v) in u) out.add(k to v)
    }
    return out
  }
}
