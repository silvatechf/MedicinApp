package com.example.medicinapp

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.example.medicinapp.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.example.medicinapp.ACTION_SNOOZE"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        when (intent.action) {
            ACTION_TAKEN -> {
                // Ação para quando o botão "Tomei" é clicado
                notificationManager.cancel(notificationId)
                Toast.makeText(context, "Ótimo! Lembrete concluído.", Toast.LENGTH_SHORT).show()
            }
            ACTION_SNOOZE -> {
                // Ação para "Adiar 5 min"
                notificationManager.cancel(notificationId) // Cancela a notificação atual

                // Reagenda o mesmo alarme para 5 minutos no futuro
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutos

                // Recriamos o intent original com os mesmos dados do lembrete
                val originalIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtras(intent.extras ?: return) // Copia todos os dados
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId, // Reutilizamos o ID para poder cancelar/atualizar
                    originalIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(it)
                    }
                    return
                }

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
                Toast.makeText(context, "Lembrete adiado por 5 minutos.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}