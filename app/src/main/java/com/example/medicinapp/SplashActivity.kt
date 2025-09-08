package com.example.medicinapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Usamos um Handler para criar um atraso de 2.5 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            // O código aqui dentro será executado após o atraso

            // Criamos a intenção de abrir a nossa MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Fechamos a SplashActivity para que o utilizador não possa voltar para ela
            finish()
        }, 2500) // 2500 milissegundos = 2.5 segundos
    }
}