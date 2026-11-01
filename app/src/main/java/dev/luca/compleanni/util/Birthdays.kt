package dev.luca.compleanni.util

import dev.luca.compleanni.data.Person
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

val ITALIAN: Locale = Locale.ITALIAN

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
    Month.of(month).getDisplayName(TextStyle.FULL, ITALIAN).replaceFirstChar { it.uppercase() }

fun formatDayMonth(day: Int, month: Int): String =
    "$day ${Month.of(month).getDisplayName(TextStyle.FULL, ITALIAN)}"

fun formatLongDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", ITALIAN))
        .replaceFirstChar { it.uppercase() }

fun countdownLabel(days: Int): String = when (days) {
    0 -> "Oggi!"
    1 -> "Domani"
    else -> "Tra $days giorni"
}

fun reminderLabel(days: Int): String = when (days) {
    0 -> "Il giorno stesso"
    1 -> "1 giorno prima"
    7 -> "1 settimana prima"
    14 -> "2 settimane prima"
    30 -> "1 mese prima"
    else -> "$days giorni prima"
}
