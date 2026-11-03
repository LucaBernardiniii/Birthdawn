package dev.luca.birthdawn.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/**
 * Lingua dell'app: "system" (segue il telefono), "it" o "en".
 * La lingua viene applicata a tutta l'Activity (testi, mesi, selettori di data e ora)
 * e alle notifiche, avvolgendo il Context con la configurazione giusta.
 */
object AppLanguage {
    const val SYSTEM = "system"
    const val ITALIAN = "it"
    const val ENGLISH = "en"

    private const val KEY = "language"

    /** Lingua effettiva in uso (sempre italiano o inglese). */
    @Volatile
    var locale: Locale = Locale.ITALIAN
        private set

    val isItalian: Boolean get() = locale.language == "it"

    fun saved(context: Context): String =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM

    fun save(context: Context, code: String) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString(KEY, code).commit()
    }

    private fun resolve(code: String): Locale = when (code) {
        ITALIAN -> Locale.ITALIAN
        ENGLISH -> Locale.ENGLISH
        else -> {
            val system = Resources.getSystem().configuration.locales[0]
            if (system.language == "it") Locale.ITALIAN else Locale.ENGLISH
        }
    }

    /** Restituisce un Context con la lingua scelta (da usare in Activity e notifiche). */
    fun wrap(context: Context): Context {
        val loc = resolve(saved(context))
        locale = loc
        Locale.setDefault(loc)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(loc))
        return context.createConfigurationContext(config)
    }
}

/** Trova l'Activity dietro a un Context (serve per riavviarla quando cambia la lingua). */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c != null) {
        if (c is Activity) return c
        c = (c as? ContextWrapper)?.baseContext
    }
    return null
}
