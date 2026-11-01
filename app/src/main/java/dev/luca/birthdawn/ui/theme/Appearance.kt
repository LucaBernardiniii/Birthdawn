package dev.luca.birthdawn.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AccentMode { APP, SYSTEM, CUSTOM }

/** Tema chiaro, scuro o come il dispositivo. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Appearance(
    val mode: AccentMode = AccentMode.APP,
    val customColor: Int = 0xFF1E88E5.toInt(),
    /** Colora la pagina di ogni persona con il colore preso dalla sua foto. */
    val photoAccent: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

/** Impostazioni di aspetto, osservabili dalla UI. */
object AppearanceSettings {
    private val _state = MutableStateFlow(Appearance())
    val state: StateFlow<Appearance> = _state

    private fun sp(context: Context) =
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(context: Context) {
        val sp = sp(context)
        _state.value = Appearance(
            mode = runCatching {
                AccentMode.valueOf(sp.getString("accent_mode", AccentMode.APP.name)!!)
            }.getOrDefault(AccentMode.APP),
            customColor = sp.getInt("accent_color", Appearance().customColor),
            photoAccent = sp.getBoolean("photo_accent", true),
            themeMode = runCatching {
                ThemeMode.valueOf(sp.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
            }.getOrDefault(ThemeMode.SYSTEM),
        )
    }

    fun update(context: Context, appearance: Appearance) {
        _state.value = appearance
        sp(context).edit()
            .putString("accent_mode", appearance.mode.name)
            .putInt("accent_color", appearance.customColor)
            .putBoolean("photo_accent", appearance.photoAccent)
            .putString("theme_mode", appearance.themeMode.name)
            .apply()
    }
}
