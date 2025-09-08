package com.example.medicinapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LembreteDao {

    // Regra para inserir um novo lembrete.
    // A função de inserir agora devolve o ID do novo lembrete, o que é mais robusto.
    @Insert
    suspend fun inserir(lembrete: Lembrete): Long

    // Regra para apagar um lembrete existente.
    @Delete
    suspend fun apagar(lembrete: Lembrete)

    // Regra para obter todos os lembretes da tabela, ordenados pela hora.
    @Query("SELECT * FROM tabela_de_lembretes ORDER BY hora ASC")
    fun obterTodos(): Flow<List<Lembrete>>
}