package dev.luca.birthdawn.data

import android.content.Context
import dev.luca.birthdawn.util.Photos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Salva le persone in un file JSON nella memoria interna dell'app. */
object PeopleRepository {
    private const val FILE = "people.json"

    private val _people = MutableStateFlow<List<Person>>(emptyList())
    val people: StateFlow<List<Person>> = _people

    @Volatile
    private var loaded = false

    fun load(context: Context): List<Person> {
        if (!loaded) {
            synchronized(this) {
                if (!loaded) {
                    _people.value = readFile(context.applicationContext)
                    loaded = true
                }
            }
        }
        return _people.value
    }

    /** Rilegge i dati dal disco (dopo l'importazione di un backup). */
    fun reload(context: Context) {
        synchronized(this) {
            _people.value = readFile(context.applicationContext)
            loaded = true
        }
    }

    fun get(id: String): Person? = _people.value.firstOrNull { it.id == id }

    fun upsert(context: Context, person: Person) {
        // Mantiene la posizione della persona nella lista
        val current = _people.value
        val index = current.indexOfFirst { it.id == person.id }
        val list = if (index >= 0) current.toMutableList().apply { set(index, person) } else current + person
        _people.value = list
        writeFile(context.applicationContext, list)
    }

    /** Modifica solo le idee regalo di una persona. */
    fun updateGifts(context: Context, personId: String, transform: (List<GiftIdea>) -> List<GiftIdea>) {
        val person = get(personId) ?: return
        upsert(context, person.copy(gifts = transform(person.gifts)))
    }

    fun delete(context: Context, id: String) {
        val person = get(id) ?: return
        person.photoPath?.let { Photos.delete(context, it) }
        val list = _people.value.filterNot { it.id == id }
        _people.value = list
        writeFile(context.applicationContext, list)
    }

    private fun readFile(context: Context): List<Person> {
        val f = File(context.filesDir, FILE)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                runCatching { fromJson(arr.getJSONObject(i)) }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun writeFile(context: Context, list: List<Person>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        val tmp = File(context.filesDir, "$FILE.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(File(context.filesDir, FILE))
    }

    private fun toJson(p: Person) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("month", p.month)
        put("day", p.day)
        p.year?.let { put("year", it) }
        p.photoPath?.let { put("photo", it) }
        put("notes", p.notes)
        put("reminders", JSONArray().apply { p.reminderDays.forEach { put(it) } })
        put("focus", JSONObject().apply {
            put("x", p.photoFocus.x.toDouble())
            put("y", p.photoFocus.y.toDouble())
            put("zoom", p.photoFocus.zoom.toDouble())
        })
        put("gifts", JSONArray().apply {
            p.gifts.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("title", g.title)
                    g.url?.let { put("url", it) }
                    put("bought", g.bought)
                })
            }
        })
    }

    private fun fromJson(o: JSONObject): Person {
        fun has(key: String) = o.has(key) && !o.isNull(key)
        val reminders = if (has("reminders")) {
            val a = o.getJSONArray("reminders")
            (0 until a.length()).map { a.getInt(it) }
        } else Person.DEFAULT_REMINDERS
        val gifts = if (has("gifts")) {
            val a = o.getJSONArray("gifts")
            (0 until a.length()).mapNotNull { i ->
                runCatching {
                    val g = a.getJSONObject(i)
                    GiftIdea(
                        id = g.getString("id"),
                        title = g.optString("title", ""),
                        url = if (g.has("url") && !g.isNull("url")) g.getString("url") else null,
                        bought = g.optBoolean("bought", false),
                    )
                }.getOrNull()
            }
        } else emptyList()
        return Person(
            id = o.getString("id"),
            name = o.getString("name"),
            month = o.getInt("month"),
            day = o.getInt("day"),
            year = if (has("year")) o.getInt("year") else null,
            photoPath = if (has("photo")) o.getString("photo") else null,
            notes = if (has("notes")) o.getString("notes") else "",
            reminderDays = reminders,
            gifts = gifts,
            photoFocus = o.optJSONObject("focus")?.let { f ->
                PhotoFocus(
                    x = f.optDouble("x", 0.5).toFloat(),
                    y = f.optDouble("y", 0.5).toFloat(),
                    zoom = f.optDouble("zoom", 1.0).toFloat(),
                )
            } ?: PhotoFocus(),
        )
    }
}
