package dev.luca.birthdawn.data

/**
 * Una persona di cui ricordare il compleanno.
 *
 * @param year anno di nascita, null se non conosciuto
 * @param photoPath nome del file della foto dentro filesDir/photos
 * @param reminderDays giorni di anticipo per le notifiche (0 = il giorno stesso)
 */
data class Person(
    val id: String,
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val photoPath: String? = null,
    val notes: String = "",
    val reminderDays: List<Int> = DEFAULT_REMINDERS,
) {
    companion object {
        val DEFAULT_REMINDERS = listOf(7, 0)
    }
}
