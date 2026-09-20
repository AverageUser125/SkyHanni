package at.hannibal2.skyhanni.utils.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException


/**
 * A Gson type adapter that strictly enforces boolean values during deserialization.
 * If the JSON element is not a boolean, it throws a JsonParseException.
 *
 * For some reason Gson's default boolean deserializer accepts any string as true, which is not what we want. This adapter ensures that only valid boolean values are accepted.
 */
class StrictBooleanTypeAdapter : TypeAdapter<Boolean>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter, value: Boolean) {
        out.value(value)
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): Boolean {
        val token = reader.peek()
        if (token === JsonToken.BOOLEAN) {
            return reader.nextBoolean()
        }
        // Reject strings, numbers, or other types as invalid for strict boolean
        throw IOException("Expected a boolean literal but found: $token")
    }
}
