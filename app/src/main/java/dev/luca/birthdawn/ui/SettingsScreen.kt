@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.luca.birthdawn.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import dev.luca.birthdawn.util.formatDayMonth
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.luca.birthdawn.notify.Notifier
import dev.luca.birthdawn.notify.Prefs
import dev.luca.birthdawn.notify.ReminderScheduler
import dev.luca.birthdawn.ui.theme.AccentMode
import dev.luca.birthdawn.ui.theme.ThemeMode
import dev.luca.birthdawn.ui.theme.LocalDarkTheme
import dev.luca.birthdawn.ui.theme.dawnBackdrop
import dev.luca.birthdawn.ui.theme.barColor
import dev.luca.birthdawn.ui.theme.AppearanceSettings
import dev.luca.birthdawn.ui.theme.supportsDynamicColor

private val SWATCHES = listOf(
    0xFFE53935, 0xFFFF7043, 0xFFFB8C00, 0xFFFFB300,
    0xFF7CB342, 0xFF43A047, 0xFF00897B, 0xFF00ACC1,
    0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60,
    0xFF6D4C41, 0xFF546E7A,
).map { Color(it) }

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appearance by AppearanceSettings.state.collectAsState()
    var timeLabel by remember { mutableStateOf(Prefs.timeLabel(context)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var myBirthday by remember { mutableStateOf(Prefs.myBirthday(context)) }
    var showMyBirthday by remember { mutableStateOf(false) }

    fun setMode(mode: AccentMode) = AppearanceSettings.update(context, appearance.copy(mode = mode))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ---------- Notifiche ----------
            SettingsSection("Il tuo compleanno")
            SettingsCard {
                ListItem(
                    headlineContent = {
                        Text(myBirthday?.let { formatDayMonth(it.day, it.month) + (it.year?.let { y -> " $y" } ?: "") } ?: "Non impostato")
                    },
                    supportingContent = {
                        Text(
                            if (myBirthday == null) "Inseriscilo e il giorno stesso Birthdawn ti farà gli auguri 🎉"
                            else "Il giorno stesso riceverai una notifica di auguri"
                        )
                    },
                    leadingContent = { Text("🎂", style = MaterialTheme.typography.titleLarge) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showMyBirthday = true },
                )
            }

            SettingsSection("Notifiche")
            SettingsCard {
                ListItem(
                    headlineContent = { Text("Orario delle notifiche") },
                    supportingContent = { Text("Ogni giorno alle $timeLabel") },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showTimePicker = true },
                )
                ListItem(
                    headlineContent = { Text("Invia notifica di prova") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        if (!Notifier.showTest(context)) {
                            Toast.makeText(
                                context,
                                "Notifiche disattivate: abilitale nelle impostazioni di Android",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                )
            }

            // ---------- Colore d'accento ----------
            SettingsSection("Tema")
            SettingsCard {
                val options = listOf(
                    ThemeMode.LIGHT to "Chiaro",
                    ThemeMode.DARK to "Scuro",
                    ThemeMode.SYSTEM to "Dispositivo",
                )
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    options.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = appearance.themeMode == mode,
                            onClick = { AppearanceSettings.update(context, appearance.copy(themeMode = mode)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        ) { Text(label) }
                    }
                }
            }

            SettingsSection("Barra di navigazione")
            SettingsCard {
                ListItem(
                    headlineContent = { Text("Rimpicciolisci scorrendo") },
                    supportingContent = {
                        Text(
                            if (appearance.shrinkNavBar) "La barra in basso diventa compatta quando scorri verso il basso"
                            else "La barra in basso resta sempre della stessa dimensione"
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = appearance.shrinkNavBar,
                            onCheckedChange = {
                                AppearanceSettings.update(context, appearance.copy(shrinkNavBar = it))
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        AppearanceSettings.update(context, appearance.copy(shrinkNavBar = !appearance.shrinkNavBar))
                    },
                )
            }

            SettingsSection("Aspetto")
            SettingsCard {
                AccentOption(
                    title = "Alba (predefinito)",
                    subtitle = "Sfondo che sfuma dall'azzurro del cielo all'arancione del sole",
                    swatch = null,
                    swatchBrush = dawnBackdrop(LocalDarkTheme.current).brush,
                    selected = appearance.mode == AccentMode.DAWN,
                    enabled = true,
                    onClick = { setMode(AccentMode.DAWN) },
                )
                val systemColor = remember {
                    if (supportsDynamicColor) dynamicLightColorScheme(context).primary else null
                }
                AccentOption(
                    title = "Sfondo del telefono",
                    subtitle = if (supportsDynamicColor) "Usa i colori del tuo sfondo (Material You)"
                    else "Richiede Android 12 o superiore",
                    swatch = systemColor,
                    selected = appearance.mode == AccentMode.SYSTEM,
                    enabled = supportsDynamicColor,
                    onClick = { setMode(AccentMode.SYSTEM) },
                )
                AccentOption(
                    title = "Personalizzato",
                    subtitle = "Scegli tu il colore",
                    swatch = Color(appearance.customColor),
                    selected = appearance.mode == AccentMode.CUSTOM,
                    enabled = true,
                    onClick = { setMode(AccentMode.CUSTOM) },
                )
                if (appearance.mode == AccentMode.CUSTOM) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                    ) {
                        SWATCHES.forEach { color ->
                            val isSelected = color.toArgb() == appearance.customColor
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        AppearanceSettings.update(
                                            context,
                                            appearance.copy(mode = AccentMode.CUSTOM, customColor = color.toArgb()),
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // ---------- Pagina persona ----------
            SettingsSection("Pagina della persona")
            SettingsCard {
                ListItem(
                    headlineContent = { Text("Colore dalla foto") },
                    supportingContent = {
                        Text("Ogni persona con una foto avrà la pagina colorata con i toni della sua foto")
                    },
                    leadingContent = { Icon(Icons.Filled.Face, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = appearance.photoAccent,
                            onCheckedChange = {
                                AppearanceSettings.update(context, appearance.copy(photoAccent = it))
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        AppearanceSettings.update(context, appearance.copy(photoAccent = !appearance.photoAccent))
                    },
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    if (showMyBirthday) {
        MyBirthdayDialog(
            initial = myBirthday,
            onDismiss = { showMyBirthday = false },
            onSave = { value ->
                Prefs.setMyBirthday(context, value)
                myBirthday = value
                showMyBirthday = false
            },
        )
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = Prefs.hour(context),
            initialMinute = Prefs.minute(context),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Orario delle notifiche") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.setTime(context, state.hour, state.minute)
                    ReminderScheduler.schedule(context)
                    ReminderScheduler.runCheckIfDue(context)
                    timeLabel = Prefs.timeLabel(context)
                    showTimePicker = false
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun AccentOption(
    title: String,
    subtitle: String,
    swatch: Color?,
    swatchBrush: Brush? = null,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        if (swatchBrush != null) {
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(swatchBrush),
            )
        } else if (swatch != null) {
            Box(
                Modifier
                    .padding(end = 12.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(swatch.copy(alpha = alpha)),
            )
        }
    }
}

@Composable
private fun MyBirthdayDialog(
    initial: Prefs.MyBirthday?,
    onDismiss: () -> Unit,
    onSave: (Prefs.MyBirthday?) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var month by remember { mutableStateOf(initial?.month ?: 0) }
    var day by remember { mutableStateOf(initial?.day ?: 0) }
    var year by remember { mutableStateOf(initial?.year) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Il tuo compleanno") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Il tuo nome (facoltativo)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Surface(
                    onClick = { pickDate = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Data", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (month == 0) "Scegli la data"
                            else formatDayMonth(day, month) + (year?.let { " $it" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = month != 0,
                onClick = { onSave(Prefs.MyBirthday(name.trim(), month, day, year)) },
            ) { Text("Salva") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onSave(null) }) { Text("Rimuovi") }
                }
                TextButton(onClick = onDismiss) { Text("Annulla") }
            }
        },
    )

    if (pickDate) {
        BirthdayDatePicker(
            initialMonth = month,
            initialDay = day,
            initialYear = year,
            hasExisting = month != 0,
            onDismiss = { pickDate = false },
            onConfirm = { m, d, y ->
                month = m; day = d; year = y
                pickDate = false
            },
        )
    }
}
