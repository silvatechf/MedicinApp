package com.example.medicinapp

import androidx.room.TypeConverter
import java.util.Calendar

class Converters {
    // Ensina o Room a ler um número da base de dados e a convertê-lo para Calendar
    @TypeConverter
    fun fromTimestamp(value: Long?): Calendar? {
        return value?.let {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            cal
        }
    }

    // Ensina o Room a pegar num Calendar e a convertê-lo para número para o guardar
    @TypeConverter
    fun calendarToTimestamp(calendar: Calendar?): Long? {
        return calendar?.timeInMillis
    }
}