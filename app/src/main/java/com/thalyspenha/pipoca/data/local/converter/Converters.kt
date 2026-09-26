package com.thalyspenha.pipoca.data.local.converter

import androidx.room.TypeConverter
import java.time.LocalDate

/** Datas sem hora (lançamento, aquisição) como string ISO `yyyy-MM-dd`. */
class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
