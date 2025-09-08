package com.example.medicinapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Calendar

@Entity(tableName = "tabela_de_lembretes")
@TypeConverters(Converters::class)
data class Lembrete(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val dosagem: String,
    val hora: Calendar,
    val tipo: MedType
)