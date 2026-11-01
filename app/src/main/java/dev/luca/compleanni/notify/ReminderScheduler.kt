package dev.luca.compleanni.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.luca.compleanni.data.PeopleRepository
import dev.luca.compleanni.util.daysUntil
import dev.luca.compleanni.util.nextBirthday
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Ogni giorno all'orario scelto (default 09:00) controlla tutte le persone e
 * manda una notifica se mancano esattamente N giorni, con N tra quelli scelti per quella persona.
 */
object ReminderScheduler {
    private const val REQUEST_CODE = 1001

    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(Prefs.hour(context), Prefs.minute(context))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(context))
    }

    /** Recupera il controllo di oggi se è già passato l'orario (es. telefono spento alle 9). */
    fun runCheckIfDue(context: Context) {
        val now = LocalDateTime.now()
        val time = LocalTime.of(Prefs.hour(context), Prefs.minute(context))
        if (now.toLocalTime().isBefore(time)) return
        runCheck(context, now.toLocalDate())
    }

    fun runCheck(context: Context, today: LocalDate) {
        if (Prefs.lastCheck(context) == today.toString()) return
        Prefs.setLastCheck(context, today.toString())
        PeopleRepository.load(context).forEach { person ->
            val days = person.daysUntil(today)
            if (days in person.reminderDays) {
                Notifier.showBirthday(context, person, days, person.nextBirthday(today))
            }
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.runCheck(context, LocalDate.now())
        ReminderScheduler.schedule(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.schedule(context)
        ReminderScheduler.runCheckIfDue(context)
    }
}
