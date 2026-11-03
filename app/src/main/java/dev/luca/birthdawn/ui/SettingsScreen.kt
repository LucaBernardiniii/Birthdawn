@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.luca.birthdawn.ui

import dev.luca.birthdawn.util.findActivity
import dev.luca.birthdawn.util.AppLanguage
import dev.luca.birthdawn.util.formatBirthday
import dev.luca.birthdawn.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import dev.luca.birthdawn.data.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // ---- Backup ----
    val scope = rememberCoroutineScope()
    var busyMessage by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            busyMessage = context.getString(R.string.exporting)
            val result = runCatching { withContext(Dispatchers.IO) { Backup.exportTo(context, uri) } }
            busyMessage = null
            Toast.makeText(
                context,
                result.fold(
                    onSuccess = { context.getString(R.string.backup_saved, context.getString(R.string.backup_summary, it.people, it.photos, it.gifts)) },
                    onFailure = { context.getString(R.string.export_failed, it.message ?: "") },
                ),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    fun setMode(mode: AccentMode) = AppearanceSettings.update(context, appearance.copy(mode = mode))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            // ---------- Lingua ----------
            SettingsSection(stringResource(R.string.language))
            SettingsCard {
                val current = remember { AppLanguage.saved(context) }
                val options = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.lang_auto),
                    AppLanguage.ITALIAN to "Italiano",
                    AppLanguage.ENGLISH to "English",
                )
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                ) {
                    options.forEachIndexed { index, (code, label) ->
                        SegmentedButton(
                            selected = current == code,
                            onClick = {
                                if (code != current) {
                                    AppLanguage.save(context, code)
                                    // Ricrea la schermata per applicare subito la nuova lingua
                                    context.findActivity()?.recreate()
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Text(
                    stringResource(R.string.lang_auto_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
                )
            }

            SettingsSection(stringResource(R.string.your_birthday))
            SettingsCard {
                ListItem(
                    headlineContent = {
                        Text(myBirthday?.let { formatBirthday(it.day, it.month, it.year) } ?: stringResource(R.string.not_set))
                    },
                    supportingContent = {
                        Text(
                            stringResource(if (myBirthday == null) R.string.your_birthday_hint_empty else R.string.your_birthday_hint_set)
                        )
                    },
                    leadingContent = { Text("🎂", style = MaterialTheme.typography.titleLarge) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showMyBirthday = true },
                )
            }

            SettingsSection(stringResource(R.string.notifications))
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.notification_time)) },
                    supportingContent = { Text(stringResource(R.string.every_day_at, timeLabel)) },
                    leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showTimePicker = true },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.send_test)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        if (!Notifier.showTest(context)) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.notifications_disabled),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                )
            }

            // ---------- Colore d'accento ----------
            SettingsSection(stringResource(R.string.theme))
            SettingsCard {
                val options = listOf(
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
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

            SettingsSection(stringResource(R.string.nav_bar))
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.shrink_on_scroll)) },
                    supportingContent = {
                        Text(
                            stringResource(if (appearance.shrinkNavBar) R.string.shrink_on else R.string.shrink_off)
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

            SettingsSection(stringResource(R.string.appearance))
            SettingsCard {
                AccentOption(
                    title = stringResource(R.string.dawn_title),
                    subtitle = stringResource(R.string.dawn_subtitle),
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
                    title = stringResource(R.string.wallpaper_title),
                    subtitle = stringResource(if (supportsDynamicColor) R.string.wallpaper_sub else R.string.wallpaper_unsupported),
                    swatch = systemColor,
                    selected = appearance.mode == AccentMode.SYSTEM,
                    enabled = supportsDynamicColor,
                    onClick = { setMode(AccentMode.SYSTEM) },
                )
                AccentOption(
                    title = stringResource(R.string.custom_title),
                    subtitle = stringResource(R.string.custom_sub),
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
            SettingsSection(stringResource(R.string.person_page))
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.photo_color)) },
                    supportingContent = {
                        Text(stringResource(R.string.photo_color_sub))
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
            SettingsSection(stringResource(R.string.backup))
            SettingsCard {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.export_data)) },
                    supportingContent = {
                        Text(stringResource(R.string.export_data_sub))
                    },
                    leadingContent = { Text("📤", style = MaterialTheme.typography.titleLarge) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { exportLauncher.launch(Backup.suggestedFileName()) },
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.import_data)) },
                    supportingContent = { Text(stringResource(R.string.import_data_sub)) },
                    leadingContent = { Text("📥", style = MaterialTheme.typography.titleLarge) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    },
                )
            }
            Text(
                stringResource(R.string.backup_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            )

            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.import_title)) },
            text = {
                Text(stringResource(R.string.import_text))
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        busyMessage = context.getString(R.string.importing)
                        val languageBefore = AppLanguage.saved(context)
                        val result = runCatching { withContext(Dispatchers.IO) { Backup.importFrom(context, uri) } }
                        busyMessage = null
                        result.onSuccess {
                            // Aggiorna i valori mostrati in questa schermata
                            myBirthday = Prefs.myBirthday(context)
                            timeLabel = Prefs.timeLabel(context)
                        }
                        Toast.makeText(
                            context,
                            result.fold(
                                onSuccess = { context.getString(R.string.backup_restored, context.getString(R.string.backup_summary, it.people, it.photos, it.gifts)) },
                                onFailure = { context.getString(R.string.import_failed, it.message ?: "") },
                            ),
                            Toast.LENGTH_LONG,
                        ).show()
                        // Se il backup aveva un'altra lingua, applicala subito
                        if (result.isSuccess && AppLanguage.saved(context) != languageBefore) {
                            context.findActivity()?.recreate()
                        }
                    }
                }) { Text(stringResource(R.string.import_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    busyMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(16.dp))
                    Text(message)
                }
            },
        )
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
            title = { Text(stringResource(R.string.notification_time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    Prefs.setTime(context, state.hour, state.minute)
                    ReminderScheduler.schedule(context)
                    ReminderScheduler.runCheckIfDue(context)
                    timeLabel = Prefs.timeLabel(context)
                    showTimePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
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
        title = { Text(stringResource(R.string.your_birthday)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.your_name)) },
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
                        Text(stringResource(R.string.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (month == 0) stringResource(R.string.choose_date)
                            else formatBirthday(day, month, year),
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
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
