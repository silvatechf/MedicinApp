package com.example.medicinapp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var medNameEditText: EditText
    private lateinit var medDosageEditText: EditText
    private lateinit var medTypeRadioGroup: RadioGroup
    private lateinit var firstDoseTimeButton: Button
    private lateinit var repeatSwitch: SwitchCompat
    private lateinit var frequencyRadioGroup: RadioGroup
    private lateinit var saveButton: Button
    private lateinit var remindersRecyclerView: RecyclerView

    private lateinit var remindersAdapter: RemindersAdapter
    private var firstDoseTime: Calendar? = null

    // Acesso ao nosso banco de dados
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val lembreteDao by lazy { database.lembreteDao() }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permissão de notificação é necessária.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        medNameEditText = findViewById(R.id.editTextMedName)
        medDosageEditText = findViewById(R.id.editTextMedDosage)
        medTypeRadioGroup = findViewById(R.id.radioGroupMedType)
        firstDoseTimeButton = findViewById(R.id.buttonFirstDoseTime)
        repeatSwitch = findViewById(R.id.switchRepeat)
        frequencyRadioGroup = findViewById(R.id.radioGroupFrequency)
        saveButton = findViewById(R.id.buttonSave)
        remindersRecyclerView = findViewById(R.id.recyclerViewReminders)

        setupRecyclerView()
        observeLembretes() // A função que liga a tela à base de dados!

        createNotificationChannel()
        requestNotificationPermission()

        firstDoseTimeButton.setOnClickListener { showTimePickerDialog() }
        saveButton.setOnClickListener {
            saveReminder()
            hideKeyboard()
        }
        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            frequencyRadioGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun observeLembretes() {
        lifecycleScope.launch {
            lembreteDao.obterTodos().collect { listaDeLembretes ->
                remindersAdapter.submitList(listaDeLembretes)
            }
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter { lembrete ->
            cancelReminder(lembrete)
        }
        remindersRecyclerView.adapter = remindersAdapter
        remindersRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun cancelReminder(lembrete: Lembrete) {
        lifecycleScope.launch(Dispatchers.IO) {
            lembreteDao.apagar(lembrete)
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, lembrete.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
        Toast.makeText(this, "Lembrete para '${lembrete.nome}' cancelado.", Toast.LENGTH_SHORT).show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showTimePickerDialog() {
        val now = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            firstDoseTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DATE, 1)
                }
            }
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            firstDoseTimeButton.text = "Primeira dose às: ${sdf.format(firstDoseTime!!.time)}"
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
    }

    private fun saveReminder() {
        val medName = medNameEditText.text.toString()
        if (medName.isBlank() || firstDoseTime == null) {
            Toast.makeText(this, "Preencha o nome e a hora, por favor.", Toast.LENGTH_SHORT).show()
            return
        }

        val medType = when (medTypeRadioGroup.checkedRadioButtonId) {
            R.id.radioDrops -> MedType.DROPS
            R.id.radioSyrup -> MedType.SYRUP
            else -> MedType.PILL
        }

        val intervalHours = if (repeatSwitch.isChecked) {
            when (frequencyRadioGroup.checkedRadioButtonId) {
                R.id.radioButton6h -> 6
                R.id.radioButton8h -> 8
                R.id.radioButton12h -> 12
                else -> 8
            }
        } else { 0 }

        val firstDoseCalendar = firstDoseTime!!
        val medDosage = medDosageEditText.text.toString()

        val firstLembrete = Lembrete(nome = medName, dosagem = medDosage, hora = firstDoseCalendar, tipo = medType)
        addReminderToDbAndSchedule(firstLembrete)

        if (intervalHours > 0) {
            for (i in 1..3) {
                val nextDoseCalendar = Calendar.getInstance().apply {
                    timeInMillis = firstDoseCalendar.timeInMillis + i * intervalHours * 60 * 60 * 1000
                }
                val nextLembrete = Lembrete(nome = medName, dosagem = medDosage, hora = nextDoseCalendar, tipo = medType)
                addReminderToDbAndSchedule(nextLembrete)
            }
        }

        Toast.makeText(this, "Lembrete(s) salvo(s)!", Toast.LENGTH_LONG).show()
        medNameEditText.text.clear()
        medDosageEditText.text.clear()
        firstDoseTime = null
        firstDoseTimeButton.text = "Selecionar Hora da Primeira Dose"
        repeatSwitch.isChecked = false
    }

    private fun addReminderToDbAndSchedule(lembrete: Lembrete) {
        lifecycleScope.launch(Dispatchers.IO) {
            val newId = lembreteDao.inserir(lembrete)
            launch(Dispatchers.Main) {
                scheduleAlarm(lembrete.copy(id = newId))
            }
        }
    }

    private fun scheduleAlarm(lembrete: Lembrete) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                startActivity(it)
            }
            return
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("MED_NAME", lembrete.nome)
            putExtra("MED_DOSAGE", lembrete.dosagem)
            putExtra("MED_TYPE", lembrete.tipo.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, lembrete.id.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            lembrete.hora.timeInMillis,
            pendingIntent
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarmes de Medicação"
            val descriptionText = "Canal para os alarmes de lembretes de medicação"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channelId = "medication_channel_id"

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                setBypassDnd(true)
            }

            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}


class RemindersAdapter(
    private val onCancelClick: (Lembrete) -> Unit
) : ListAdapter<Lembrete, RemindersAdapter.ViewHolder>(LembreteDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.itemIcon)
        val name: TextView = view.findViewById(R.id.itemName)
        val dosage: TextView = view.findViewById(R.id.itemDosage)
        val time: TextView = view.findViewById(R.id.itemTime)
        val cancelButton: ImageButton = view.findViewById(R.id.buttonCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lembrete, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lembrete = getItem(position)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.name.text = lembrete.nome
        holder.dosage.text = lembrete.dosagem
        holder.time.text = sdf.format(lembrete.hora.time)

        val iconRes = when (lembrete.tipo) {
            MedType.PILL -> R.drawable.ic_pill
            MedType.DROPS -> R.drawable.ic_drops
            MedType.SYRUP -> R.drawable.ic_syrup
        }
        holder.icon.setImageResource(iconRes)

        holder.cancelButton.setOnClickListener {
            onCancelClick(lembrete)
        }
    }
}

class LembreteDiffCallback : DiffUtil.ItemCallback<Lembrete>() {
    override fun areItemsTheSame(oldItem: Lembrete, newItem: Lembrete): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Lembrete, newItem: Lembrete): Boolean {
        return oldItem == newItem
    }
}