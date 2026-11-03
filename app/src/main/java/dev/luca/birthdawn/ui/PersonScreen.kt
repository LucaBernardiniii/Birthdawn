@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.luca.birthdawn.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import dev.luca.birthdawn.util.turnsLabel
import dev.luca.birthdawn.util.formatBirthday
import dev.luca.birthdawn.R
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import dev.luca.birthdawn.ui.theme.AppearanceSettings
import dev.luca.birthdawn.ui.theme.LocalDarkTheme
import dev.luca.birthdawn.ui.theme.schemeFromSeed
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.luca.birthdawn.data.PhotoFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.sp
import dev.luca.birthdawn.data.PeopleRepository
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.notify.Prefs
import dev.luca.birthdawn.util.Photos
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.countdownLabel
import dev.luca.birthdawn.util.daysUntil
import dev.luca.birthdawn.util.formatDayMonth
import dev.luca.birthdawn.util.nextBirthday
import dev.luca.birthdawn.util.reminderLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private val PRESET_REMINDERS = listOf(0, 1, 3, 7, 14, 30)

@Composable
fun PersonScreen(
    existing: Person?,
    onSaved: (String) -> Unit,
    onCancel: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var month by rememberSaveable { mutableIntStateOf(existing?.month ?: 0) }
    var day by rememberSaveable { mutableIntStateOf(existing?.day ?: 0) }
    var year by rememberSaveable { mutableStateOf(existing?.year) }
    var photo by rememberSaveable { mutableStateOf(existing?.photoPath) }
    var focusX by rememberSaveable { mutableFloatStateOf(existing?.photoFocus?.x ?: 0.5f) }
    var focusY by rememberSaveable { mutableFloatStateOf(existing?.photoFocus?.y ?: 0.5f) }
    var focusZoom by rememberSaveable { mutableFloatStateOf(existing?.photoFocus?.zoom ?: 1f) }
    val focus = PhotoFocus(focusX, focusY, focusZoom)
    fun setFocus(f: PhotoFocus) {
        focusX = f.x; focusY = f.y; focusZoom = f.zoom
    }
    var showAdjust by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var remindersStr by rememberSaveable {
        mutableStateOf((existing?.reminderDays ?: Person.DEFAULT_REMINDERS).joinToString(","))
    }
    val reminders: Set<Int> = remindersStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    fun setReminders(set: Set<Int>) {
        remindersStr = set.sortedDescending().joinToString(",")
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomReminder by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val importedPhotos = remember { mutableStateListOf<String>() }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { Photos.importFromUri(context, uri, id) }
                if (saved != null) {
                    photo = saved
                    importedPhotos += saved
                    setFocus(PhotoFocus())
                    showAdjust = true // subito dopo la scelta, fai regolare la parte visibile
                }
            }
        }
    }
    fun launchPicker() = pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    fun close(saved: Boolean) {
        // Elimina le foto non più usate
        val keep = if (saved) photo else existing?.photoPath
        importedPhotos.filter { it != keep }.forEach { Photos.delete(context, it) }
        if (saved && existing?.photoPath != null && existing.photoPath != photo) {
            Photos.delete(context, existing.photoPath)
        }
        if (saved) onSaved(id) else onCancel()
    }

    val canSave = name.isNotBlank() && month in 1..12 && day >= 1

    // Se attivo nelle impostazioni, tutta la pagina usa il colore estratto dalla foto
    PhotoAccentTheme(photo) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            PhotoBanner(
                photo = photo,
                name = name,
                onPick = { launchPicker() },
                focus = focus,
            )

            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (photo != null) {
                    Row(
                        Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(onClick = { showAdjust = true }) { Text(stringResource(R.string.adjust_position)) }
                        TextButton(onClick = {
                            photo = null
                            setFocus(PhotoFocus())
                        }) { Text(stringResource(R.string.remove_photo)) }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))

                // Data di compleanno
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.birthday_date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (month == 0) {
                                Text(stringResource(R.string.choose_date), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            } else {
                                Text(
                                    formatBirthday(day, month, year),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                val preview = Person(id, name, month, day, year)
                                val age = preview.ageOn(preview.nextBirthday(today))
                                Text(
                                    countdownLabel(context, preview.daysUntil(today)) +
                                        (age?.let { " · " + turnsLabel(context, it) } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_date), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_hint)) },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))

                // Notifiche personalizzate
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.notifications), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    if (reminders.isEmpty()) stringResource(R.string.no_reminders_person)
                    else stringResource(R.string.reminders_info, Prefs.timeLabel(context)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    val all = (PRESET_REMINDERS + reminders).distinct().sorted()
                    all.forEach { d ->
                        val selected = d in reminders
                        val isCustom = d !in PRESET_REMINDERS
                        FilterChip(
                            selected = selected,
                            onClick = { setReminders(if (selected) reminders - d else reminders + d) },
                            label = { Text(reminderLabel(context, d)) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                            trailingIcon = if (isCustom) {
                                { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove), modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                        )
                    }
                    AssistChip(
                        onClick = { showCustomReminder = true },
                        label = { Text(stringResource(R.string.custom)) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
                if (reminders.toSet() != Person.DEFAULT_REMINDERS.toSet()) {
                    TextButton(onClick = { setReminders(Person.DEFAULT_REMINDERS.toSet()) }) {
                        Text(stringResource(R.string.restore_default_reminders))
                    }
                }

                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = {
                        val person = Person(
                            id = id,
                            name = name.trim(),
                            month = month,
                            day = day,
                            year = year,
                            photoPath = photo,
                            notes = notes.trim(),
                            reminderDays = reminders.sortedDescending(),
                            // Le idee regalo si gestiscono dalla pagina della persona: vanno mantenute
                            gifts = PeopleRepository.get(id)?.gifts ?: emptyList(),
                            photoFocus = if (photo != null) focus else PhotoFocus(),
                        )
                        PeopleRepository.upsert(context, person)
                        close(saved = true)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text(stringResource(if (existing == null) R.string.add_person else R.string.save_changes)) }
                if (!canSave) {
                    Text(
                        stringResource(R.string.fill_name_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }

        // Barra in alto sopra il banner
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(onClick = { close(saved = false) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (existing != null) {
                RoundIconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = Color.White)
                }
            }
        }

        // Schermata "Regola la foto", sopra a tutto il resto
        if (showAdjust) {
            val bitmap = rememberPhoto(photo, 1400)
            if (bitmap != null) {
                PhotoAdjustScreen(
                    bitmap = bitmap,
                    initial = focus,
                    onDismiss = { showAdjust = false },
                    onConfirm = {
                        setFocus(it)
                        showAdjust = false
                    },
                )
            }
        }
    }

    if (showDatePicker) {
        BirthdayDatePicker(
            initialMonth = month,
            initialDay = day,
            initialYear = year,
            hasExisting = month != 0,
            onDismiss = { showDatePicker = false },
            onConfirm = { m, d, y ->
                month = m; day = d; year = y
                showDatePicker = false
            },
        )
    }

    if (showCustomReminder) {
        CustomReminderDialog(
            onDismiss = { showCustomReminder = false },
            onConfirm = { days ->
                setReminders(reminders + days)
                showCustomReminder = false
            },
        )
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_person_title, existing.name)) },
            text = { Text(stringResource(R.string.delete_person_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    importedPhotos.forEach { Photos.delete(context, it) }
                    PeopleRepository.delete(context, existing.id)
                    onDeleted()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    } // PhotoAccentTheme
}

/** Applica una palette generata dal colore della foto, con transizione morbida. */
@Composable
fun PhotoAccentTheme(photo: String?, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val appearance by AppearanceSettings.state.collectAsState()
    val dark = LocalDarkTheme.current
    val seed by produceState<Int?>(initialValue = null, photo, appearance.photoAccent) {
        value = if (appearance.photoAccent && photo != null) {
            withContext(Dispatchers.Default) { Photos.accentColor(context, photo) }
        } else null
    }
    val base = MaterialTheme.colorScheme
    val target = remember(seed, dark, base) {
        seed?.let {
            val fromPhoto = schemeFromSeed(Color(it), dark)
            // Tema Alba (sfondo trasparente): la foto cambia solo gli accenti, il cielo resta visibile
            if (base.background.alpha < 1f) fromPhoto.copy(
                background = base.background,
                surface = base.surface,
                surfaceContainerLow = base.surfaceContainerLow,
                surfaceContainer = base.surfaceContainer,
            ) else fromPhoto
        } ?: base
    }
    val spec = tween<Color>(durationMillis = 450)
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "pc")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "opc")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "tertiary")
    val background by animateColorAsState(target.background, spec, label = "bg")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "sc")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "sec")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "osec")

    MaterialTheme(
        colorScheme = target.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            tertiary = tertiary,
            background = background,
            surface = background,
            surfaceContainer = surfaceContainer,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground, content = content)
    }
}

@Composable
fun PhotoBanner(
    photo: String?,
    name: String,
    onPick: (() -> Unit)?,
    subtitle: String? = null,
    focus: PhotoFocus = PhotoFocus(),
    height: Dp = BANNER_HEIGHT,
) {
    val bitmap = rememberPhoto(photo, 1400)
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .then(if (onPick != null) Modifier.clickable(onClick = onPick) else Modifier),
    ) {
        if (bitmap != null) {
            FocusedImage(bitmap, focus, Modifier.fillMaxSize())
        } else if (onPick == null) {
            // Visualizzazione senza foto: iniziali grandi
            Text(
                initials(name),
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 96.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Face, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.tap_add_photo), color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
        // Velo scuro in basso per leggere il nome
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name.ifBlank { stringResource(R.string.new_person) },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (bitmap != null && onPick != null) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.25f)) {
                    Text(
                        stringResource(R.string.change_photo),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f)),
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
fun BirthdayDatePicker(
    initialMonth: Int,
    initialDay: Int,
    initialYear: Int?,
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (month: Int, day: Int, year: Int?) -> Unit,
) {
    val currentYear = LocalDate.now().year
    val initialMillis = if (hasExisting) {
        LocalDate.of(initialYear ?: 2000, initialMonth, initialDay)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    } else null
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = 1900..currentYear,
    )
    var yearUnknown by remember { mutableStateOf(hasExisting && initialYear == null) }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onConfirm(date.monthValue, date.dayOfMonth, if (yearUnknown) null else date.year)
                    }
                },
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) {
        DatePicker(
            state = state,
            showModeToggle = true,
            // La casella sta nell'intestazione, così non si sovrappone al resto del calendario
            title = {
                Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) {
                    Text(stringResource(R.string.select_birth_date), style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier
                            .offset(x = (-12).dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { yearUnknown = !yearUnknown }
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = yearUnknown, onCheckedChange = { yearUnknown = it })
                        Text(stringResource(R.string.unknown_year), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
        )
    }
}

@Composable
private fun CustomReminderDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.toIntOrNull()
    val valid = value != null && value in 0..365
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_reminder_title)) },
        text = {
            Column {
                Text(stringResource(R.string.custom_reminder_question))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                    label = { Text(stringResource(R.string.days_before_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !valid,
                    supportingText = { Text(stringResource(R.string.days_before_range)) },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { value?.let(onConfirm) }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
