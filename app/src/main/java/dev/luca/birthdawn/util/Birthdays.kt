package dev.luca.birthdawn.util

import android.content.Context
import dev.luca.birthdawn.R
import dev.luca.birthdawn.data.Person
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

/** Data del compleanno in un certo anno (il 29 febbraio diventa 28 negli anni non bisestili). */
fun birthdayIn(year: Int, month: Int, day: Int): LocalDate =
    if (month == 2 && day == 29 && !Year.isLeap(year.toLong())) LocalDate.of(year, 2, 28)
    else LocalDate.of(year, month, day)

fun Person.nextBirthday(today: LocalDate): LocalDate {
    val thisYear = birthdayIn(today.year, month, day)
    return if (thisYear.isBefore(today)) birthdayIn(today.year + 1, month, day) else thisYear
}

fun Person.daysUntil(today: LocalDate): Int =
    ChronoUnit.DAYS.between(today, nextBirthday(today)).toInt()

/** Età che compie in quella data, se l'anno di nascita è noto. */
fun Person.ageOn(date: LocalDate): Int? = year?.let { date.year - it }?.takeIf { it > 0 }

fun Person.occursOn(date: LocalDate): Boolean = birthdayIn(date.year, month, day) == date

fun monthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, AppLanguage.locale)
        .replaceFirstChar { it.uppercase() }

/** "1 novembre" / "November 1" */
fun formatDayMonth(day: Int, month: Int): String {
    val date = LocalDate.of(2000, month, day) // 2000 è bisestile: va bene anche il 29 febbraio
    val pattern = if (AppLanguage.isItalian) "d MMMM" else "MMMM d"
    return date.format(DateTimeFormatter.ofPattern(pattern, AppLanguage.locale))
}

/** "1 novembre 1990" / "November 1, 1990" (senza anno se non è noto) */
fun formatBirthday(day: Int, month: Int, year: Int?): String {
    val base = formatDayMonth(day, month)
    return when {
        year == null -> base
        AppLanguage.isItalian -> "$base $year"
        else -> "$base, $year"
    }
}

/** "Domenica 1 novembre" / "Sunday, November 1" */
fun formatLongDate(date: LocalDate): String {
    val pattern = if (AppLanguage.isItalian) "EEEE d MMMM" else "EEEE, MMMM d"
    return date.format(DateTimeFormatter.ofPattern(pattern, AppLanguage.locale))
        .replaceFirstChar { it.uppercase() }
}

/** Iniziali dei giorni della settimana, da lunedì: L M M G V S D / M T W T F S S */
fun weekdayInitials(): List<String> =
    DayOfWeek.values().map { it.getDisplayName(TextStyle.NARROW, AppLanguage.locale).uppercase() }

fun countdownLabel(context: Context, days: Int): String = when (days) {
    0 -> context.getString(R.string.today_exclaim)
    1 -> context.getString(R.string.tomorrow)
    else -> context.resources.getQuantityString(R.plurals.in_days, days, days)
}

fun reminderLabel(context: Context, days: Int): String = when (days) {
    0 -> context.getString(R.string.reminder_same_day)
    7 -> context.getString(R.string.reminder_1_week)
    14 -> context.getString(R.string.reminder_2_weeks)
    30 -> context.getString(R.string.reminder_1_month)
    else -> context.resources.getQuantityString(R.plurals.reminder_days_before, days, days)
}

/** "compie 30 anni" / "turns 30" */
fun turnsLabel(context: Context, age: Int, capitalized: Boolean = false): String =
    context.resources.getQuantityString(if (capitalized) R.plurals.turns_age_cap else R.plurals.turns_age, age, age)

fun plural(context: Context, id: Int, count: Int): String =
    context.resources.getQuantityString(id, count, count)
