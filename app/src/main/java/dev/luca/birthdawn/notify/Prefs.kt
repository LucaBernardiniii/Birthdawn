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

    fun timeLabel(context: Context) = "%02d:%02d".format(hour(context), minute(context))
}
