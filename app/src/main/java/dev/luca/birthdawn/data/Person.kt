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
    val gifts: List<GiftIdea> = emptyList(),
    val photoFocus: PhotoFocus = PhotoFocus(),
) {
    companion object {
        val DEFAULT_REMINDERS = listOf(7, 0)
    }
}

/** Un'idea regalo: un titolo e, facoltativo, un link (es. pagina di un negozio online). */
data class GiftIdea(
    val id: String,
    val title: String,
    val url: String? = null,
    val bought: Boolean = false,
)

/**
 * Parte della foto da mostrare: [x] e [y] (0..1) sono il punto della foto che va al centro
 * del riquadro, [zoom] (>= 1) l'ingrandimento rispetto al semplice "riempi".
 */
data class PhotoFocus(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val zoom: Float = 1f,
)
