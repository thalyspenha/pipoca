package com.thalyspenha.pipoca.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import java.time.LocalDate

class Converters {
    /** Datas sem hora (lançamento, aquisição) como string ISO `yyyy-MM-dd`. */
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    /** Listas curtas de nomes (diretores, criadores) como array JSON. */
    @TypeConverter
    fun stringListToJson(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = Json.decodeFromString(value)
}
