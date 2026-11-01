package dev.luca.compleanni.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AccentMode { APP, SYSTEM, CUSTOM }

data class Appearance(
    val mode: AccentMode = AccentMode.APP,
    val customColor: Int = 0xFF1E88E5.toInt(),
    /** Colora la pagina di ogni persona con il colore preso dalla sua foto. */
    val photoAccent: Boolean = true,
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
        )
    }

    fun update(context: Context, appearance: Appearance) {
        _state.value = appearance
        sp(context).edit()
            .putString("accent_mode", appearance.mode.name)
            .putInt("accent_color", appearance.customColor)
            .putBoolean("photo_accent", appearance.photoAccent)
            .apply()
    }
}
