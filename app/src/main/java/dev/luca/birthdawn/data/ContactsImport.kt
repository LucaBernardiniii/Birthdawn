package dev.luca.birthdawn.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.luca.birthdawn.util.Photos
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Legge i compleanni salvati nella rubrica del telefono.
 * Tutto avviene sul dispositivo: nessun dato esce dall'app.
 */
object ContactsImport {

    /** Un compleanno trovato nella rubrica. */
    data class ContactBirthday(
        val contactId: Long,
        val lookupKey: String,
        val name: String,
        val month: Int,
        val day: Int,
        val year: Int?,
        val hasPhoto: Boolean,
        /** true se questa persona è già in Birthdawn (importata prima o aggiunta a mano). */
        val alreadyAdded: Boolean,
    )

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Tutti i contatti con un compleanno, in ordine alfabetico. Da chiamare fuori dal thread principale. */
    fun findBirthdays(context: Context): List<ContactBirthday> {
        if (!hasPermission(context)) return emptyList()
        val existing = PeopleRepository.load(context)
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.LOOKUP_KEY,
            ContactsContract.Data.DISPLAY_NAME,
            ContactsContract.Data.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Event.START_DATE,
        )
        val selection = "${ContactsContract.Data.MIMETYPE} = ? AND " +
            "${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
        val args = arrayOf(
            ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
        )

        val found = LinkedHashMap<String, ContactBirthday>()
        context.contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, args, null)
            ?.use { c ->
                while (c.moveToNext()) {
                    val contactId = c.getLong(0)
                    val lookup = c.getString(1) ?: continue
                    val name = c.getString(2)?.trim().orEmpty()
                    if (name.isEmpty()) continue
                    val date = parseDate(c.getString(4)) ?: continue
                    // Lo stesso contatto può comparire più volte (es. account Google + SIM): ne tiene uno
                    if (found.containsKey(lookup)) continue
                    val already = existing.any { p ->
                        p.contactKey == lookup ||
                            (p.name.equals(name, ignoreCase = true) && p.month == date.month && p.day == date.day)
                    }
                    found[lookup] = ContactBirthday(
                        contactId = contactId,
                        lookupKey = lookup,
                        name = name,
                        month = date.month,
                        day = date.day,
                        year = date.year,
                        hasPhoto = !c.getString(3).isNullOrEmpty(),
                        alreadyAdded = already,
                    )
                }
            }
        return found.values.sortedBy { it.name.lowercase() }
    }

    /** Miniatura della foto del contatto, per l'elenco. */
    fun thumbnail(context: Context, contactId: Long): ImageBitmap? = runCatching {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, false)
            ?.use { BitmapFactory.decodeStream(it) }
            ?.asImageBitmap()
    }.getOrNull()

    /**
     * Aggiunge a Birthdawn i contatti scelti, con le notifiche predefinite.
     * @return quante persone sono state aggiunte
     */
    fun importSelected(context: Context, selected: List<ContactBirthday>, withPhotos: Boolean): Int {
        val people = selected.map { c ->
            val id = UUID.randomUUID().toString()
            val photo = if (withPhotos && c.hasPhoto) {
                runCatching {
                    val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.contactId)
                    ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)
                        ?.let { Photos.importFromStream(context, it, id) }
                }.getOrNull()
            } else null
            Person(
                id = id,
                name = c.name,
                month = c.month,
                day = c.day,
                year = c.year,
                photoPath = photo,
                contactKey = c.lookupKey,
            )
        }
        PeopleRepository.addAll(context, people)
        return people.size
    }

    // ------------------------------------------------------------------ date

    data class ParsedDate(val month: Int, val day: Int, val year: Int?)

    private val ISO = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""")          // 1990-05-12, 1990-05-12T00:00:00Z
    private val NO_YEAR = Regex("""^--(\d{1,2})-?(\d{1,2})$""")           // --05-12, --0512
    private val COMPACT = Regex("""^(\d{4})(\d{2})(\d{2})$""")             // 19900512
    private val DMY = Regex("""^(\d{1,2})[./-](\d{1,2})[./-](\d{4})$""")   // 12/05/1990, 12.05.1990

    /** Interpreta i formati di data usati dalle varie app Rubrica. */
    fun parseDate(raw: String?): ParsedDate? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        val (y, m, d) = ISO.find(s)?.destructured?.let { (y, m, d) -> Triple(y.toInt(), m.toInt(), d.toInt()) }
            ?: NO_YEAR.find(s)?.destructured?.let { (m, d) -> Triple(0, m.toInt(), d.toInt()) }
            ?: COMPACT.find(s)?.destructured?.let { (y, m, d) -> Triple(y.toInt(), m.toInt(), d.toInt()) }
            ?: DMY.find(s)?.destructured?.let { (d, m, y) -> Triple(y.toInt(), m.toInt(), d.toInt()) }
            ?: return null
        if (m !in 1..12) return null
        // Controlla il giorno su un anno bisestile, così il 29 febbraio è valido
        if (d !in 1..YearMonth.of(2000, m).lengthOfMonth()) return null
        // Alcune rubriche usano anni finti (es. 1604) quando l'anno non è noto
        val year = y.takeIf { it in 1900..LocalDate.now().year }
        if (year != null && m == 2 && d == 29 && !java.time.Year.isLeap(year.toLong())) {
            return ParsedDate(m, d, null)
        }
        return ParsedDate(m, d, year)
    }
}
