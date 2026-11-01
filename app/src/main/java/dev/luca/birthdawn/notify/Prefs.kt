package dev.luca.birthdawn.notify

import android.content.Context

object Prefs {
    private fun sp(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun hour(context: Context) = sp(context).getInt("hour", 9)
    fun minute(context: Context) = sp(context).getInt("minute", 0)

    fun setTime(context: Context, hour: Int, minute: Int) {
        sp(context).edit().putInt("hour", hour).putInt("minute", minute).apply()
    }

    fun lastCheck(context: Context): String? = sp(context).getString("last_check", null)

    fun setLastCheck(context: Context, date: String) {
        sp(context).edit().putString("last_check", date).apply()
    }

    // ---- Il tuo compleanno ----
    data class MyBirthday(val name: String, val month: Int, val day: Int, val year: Int?)

    fun myBirthday(context: Context): MyBirthday? {
        val sp = sp(context)
        val month = sp.getInt("my_month", 0)
        if (month == 0) return null
        val year = sp.getInt("my_year", 0)
        return MyBirthday(
            name = sp.getString("my_name", "") ?: "",
            month = month,
            day = sp.getInt("my_day", 1),
            year = if (year > 0) year else null,
        )
    }

    fun setMyBirthday(context: Context, value: MyBirthday?) {
        val e = sp(context).edit()
        if (value == null) {
            e.remove("my_month").remove("my_day").remove("my_year").remove("my_name")
        } else {
            e.putInt("my_month", value.month).putInt("my_day", value.day)
                .putInt("my_year", value.year ?: 0).putString("my_name", value.name)
        }
        e.apply()
    }

    // ---- Ordinamento della lista in Home ----
    fun homeSort(context: Context): String = sp(context).getString("home_sort", "UPCOMING") ?: "UPCOMING"

    fun setHomeSort(context: Context, value: String) {
        sp(context).edit().putString("home_sort", value).apply()
    }

    fun timeLabel(context: Context) = "%02d:%02d".format(hour(context), minute(context))
}
