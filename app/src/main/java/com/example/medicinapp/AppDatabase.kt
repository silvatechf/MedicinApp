package com.example.medicinapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// A anotação @Database diz ao Room que este ficheiro representa a nossa base de dados.
// 'entities' diz-lhe quais são as tabelas (no nosso caso, apenas a de lembretes).
// 'version' é importante para futuras atualizações.
@Database(entities = [Lembrete::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Esta função permite que a base de dados nos dê acesso ao "livro de regras" (DAO).
    abstract fun lembreteDao(): LembreteDao

    // Este 'companion object' é um truque para garantir que só existe UMA instância
    // da base de dados em todo o aplicativo (padrão Singleton).
    companion object {
        // A anotação @Volatile garante que a variável INSTANCE é sempre a mais atualizada.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Se a instância já existir, devolve-a. Se não, cria-a.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lembrete_database" // O nome do ficheiro da base de dados no telemóvel
                ).build()
                INSTANCE = instance
                // retorna a instância
                instance
            }
        }
    }
}