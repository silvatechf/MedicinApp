package com.example.medicinapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medication_channel_id"
        val notificationId = System.currentTimeMillis().toInt() // ID único para esta notificação

        // Ler os dados do lembrete que enviámos com o alarme
        val medName = intent.getStringExtra("MED_NAME") ?: "um remédio"
        val medDosage = intent.getStringExtra("MED_DOSAGE") ?: ""
        val medTypeName = intent.getStringExtra("MED_TYPE")

        // Converter o nome do tipo de volta para o nosso enum
        val medType = try { MedType.valueOf(medTypeName ?: "PILL") } catch (e: Exception) { MedType.PILL }

        val notificationText = if (medDosage.isNotBlank()) "Tomar $medDosage." else "Não se esqueça."

        // Escolher o ícone correto com base no tipo de remédio
        val iconRes = when (medType) {
            MedType.PILL -> R.drawable.ic_pill
            MedType.DROPS -> R.drawable.ic_drops
            MedType.SYRUP -> R.drawable.ic_syrup
        }

        // --- Lógica para os Botões ---

        // Ação para abrir o app ao clicar na notificação principal
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context, notificationId, contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Ação para o botão "Tomei"
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKEN
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 1, takenIntent, // ID único para o PendingIntent
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Ação para o botão "Adiar 5 min"
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            // Passamos todos os dados originais para que o alarme possa ser reagendado
            putExtras(intent.extras ?: return)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, notificationId + 2, snoozeIntent, // ID único para o PendingIntent
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // --- Construir a Notificação Final ---

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle("⏰ Hora de: $medName!")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // A notificação some depois de ser clicada
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Tomei", takenPendingIntent) // Botão 1
            .addAction(0, "Adiar 5 min", snoozePendingIntent) // Botão 2

        // Enviar a notificação
        notificationManager.notify(notificationId, builder.build())
    }
}