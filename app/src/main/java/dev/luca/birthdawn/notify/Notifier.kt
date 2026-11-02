package dev.luca.birthdawn.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.luca.birthdawn.MainActivity
import dev.luca.birthdawn.R
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.formatDayMonth
import java.time.LocalDate

object Notifier {
    private const val CHANNEL_ID = "birthdays"
    const val EXTRA_PERSON_ID = "person_id"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, "Promemoria compleanni", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Avvisi per i compleanni in arrivo" }
        nm.createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showBirthday(context: Context, person: Person, daysBefore: Int, date: LocalDate) {
        val age = person.ageOn(date)
        val title = when (daysBefore) {
            0 -> "🎂 Oggi è il compleanno di ${person.name}!"
            1 -> "Domani è il compleanno di ${person.name}"
            else -> "Tra $daysBefore giorni è il compleanno di ${person.name}"
        }
        val text = if (daysBefore == 0) {
            (age?.let { "Compie $it anni. " } ?: "") + "Ricordati di fare gli auguri!"
        } else {
            formatDayMonth(person.day, person.month) + (age?.let { " · compie $it anni" } ?: "")
        }
        val ideas = person.gifts.count { !it.bought }
        val giftLine = when {
            daysBefore == 0 || ideas == 0 -> null
            ideas == 1 -> "🎁 Hai 1 idea regalo salvata"
            else -> "🎁 Hai $ideas idee regalo salvate"
        }
        val fullText = listOfNotNull(text, giftLine, person.notes.takeIf { it.isNotBlank() }).joinToString("\n")
        post(context, (person.id + daysBefore).hashCode(), title, text, fullText, person.id)
    }

    fun showOwnBirthday(context: Context, name: String, age: Int?) {
        val who = name.trim().ifEmpty { null }
        val title = "🎉 Buon compleanno" + (who?.let { ", $it" } ?: "") + "!"
        val text = (age?.takeIf { it > 0 }?.let { "$it anni! " } ?: "") +
            "Tanti auguri da Birthdawn, goditi la giornata 🎂"
        post(context, 7777, title, text, text, null)
    }

    fun showTest(context: Context): Boolean {
        if (!canNotify(context)) return false
        post(context, 42, "🎂 Notifica di prova", "Le notifiche dei compleanni funzionano!", null, null)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun post(
        context: Context, id: Int, title: String, text: String, bigText: String?, personId: String?
    ) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (personId != null) putExtra(EXTRA_PERSON_ID, personId)
        }
        val pending = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFE8505B.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        if (bigText != null) builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
