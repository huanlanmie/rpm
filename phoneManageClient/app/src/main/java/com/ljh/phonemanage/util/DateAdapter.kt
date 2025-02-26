package com.ljh.phonemanage.util

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 自定义日期适配器，处理多种日期格式
 * 同时支持序列化和反序列化
 */
class DateAdapter : JsonDeserializer<Date>, JsonSerializer<Date> {
    private val dateFormats = arrayOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    
    // 用于序列化的主要格式，修改为包含时分秒
    private val primaryFormat = "yyyy-MM-dd HH:mm:ss"
    
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date? {
        val dateString = json.asString
        
        if (dateString.isNullOrEmpty()) {
            return null
        }
        
        for (format in dateFormats) {
            try {
                val formatter = SimpleDateFormat(format, Locale.getDefault())
                return formatter.parse(dateString)
            } catch (e: ParseException) {
                // 尝试下一种格式
            }
        }
        
        throw JsonParseException("Cannot parse date: $dateString")
    }
    
    /**
     * 序列化Date为指定格式的字符串
     */
    override fun serialize(
        src: Date?,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        if (src == null) {
            return JsonPrimitive("")
        }
        
        val formatter = SimpleDateFormat(primaryFormat, Locale.getDefault())
        return JsonPrimitive(formatter.format(src))
    }
} 