package com.dins.spendwise.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Lets Moshi serialize/deserialize Map&lt;String, Any?&gt; sync payloads.
 */
class MoshiObjectAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Any? = reader.readJsonValue()

    @ToJson
    fun toJson(writer: JsonWriter, value: Any?) {
        writer.jsonValue(value)
    }
}
