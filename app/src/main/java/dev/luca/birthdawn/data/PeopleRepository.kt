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

    fun get(id: String): Person? = _people.value.firstOrNull { it.id == id }

    fun upsert(context: Context, person: Person) {
        val list = _people.value.filterNot { it.id == person.id } + person
        _people.value = list
        writeFile(context.applicationContext, list)
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
    }

    private fun fromJson(o: JSONObject): Person {
        fun has(key: String) = o.has(key) && !o.isNull(key)
        val reminders = if (has("reminders")) {
            val a = o.getJSONArray("reminders")
            (0 until a.length()).map { a.getInt(it) }
        } else Person.DEFAULT_REMINDERS
        return Person(
            id = o.getString("id"),
            name = o.getString("name"),
            month = o.getInt("month"),
            day = o.getInt("day"),
            year = if (has("year")) o.getInt("year") else null,
            photoPath = if (has("photo")) o.getString("photo") else null,
            notes = if (has("notes")) o.getString("notes") else "",
            reminderDays = reminders,
        )
    }
}
