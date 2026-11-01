@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.luca.compleanni.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import dev.luca.compleanni.data.PeopleRepository
import dev.luca.compleanni.data.Person
import dev.luca.compleanni.notify.Prefs
import dev.luca.compleanni.util.Photos
import dev.luca.compleanni.util.ageOn
import dev.luca.compleanni.util.countdownLabel
import dev.luca.compleanni.util.daysUntil
import dev.luca.compleanni.util.formatDayMonth
import dev.luca.compleanni.util.nextBirthday
import dev.luca.compleanni.util.reminderLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

private val PRESET_REMINDERS = listOf(0, 1, 3, 7, 14, 30)

@Composable
fun PersonScreen(existing: Person?, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()

    val id = rememberSaveable { existing?.id ?: UUID.randomUUID().toString() }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var month by rememberSaveable { mutableIntStateOf(existing?.month ?: 0) }
    var day by rememberSaveable { mutableIntStateOf(existing?.day ?: 0) }
    var year by rememberSaveable { mutableStateOf(existing?.year) }
    var photo by rememberSaveable { mutableStateOf(existing?.photoPath) }
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
        onClose()
    }

    val canSave = name.isNotBlank() && month in 1..12 && day >= 1

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
            )

            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                if (photo != null) {
                    TextButton(
                        onClick = { photo = null },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Rimuovi foto") }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
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
                                "Data di compleanno",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (month == 0) {
                                Text("Scegli la data", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            } else {
                                Text(
                                    formatDayMonth(day, month) + (year?.let { " $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                val preview = Person(id, name, month, day, year)
                                val age = preview.ageOn(preview.nextBirthday(today))
                                Text(
                                    countdownLabel(preview.daysUntil(today)) +
                                        (age?.let { " · compie $it anni" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Icon(Icons.Filled.Edit, contentDescription = "Modifica data", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (idee regalo, ecc.)") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))

                // Notifiche personalizzate
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Text("Notifiche", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    if (reminders.isEmpty()) "Nessuna notifica per questa persona."
                    else "Riceverai un avviso alle ${Prefs.timeLabel(context)} nei giorni scelti.",
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
                            label = { Text(reminderLabel(d)) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                            trailingIcon = if (isCustom) {
                                { Icon(Icons.Filled.Close, contentDescription = "Rimuovi", modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null,
                        )
                    }
                    AssistChip(
                        onClick = { showCustomReminder = true },
                        label = { Text("Personalizzata") },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
                if (reminders.toSet() != Person.DEFAULT_REMINDERS.toSet()) {
                    TextButton(onClick = { setReminders(Person.DEFAULT_REMINDERS.toSet()) }) {
                        Text("Ripristina predefinite (7 giorni prima + il giorno stesso)")
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
                        )
                        PeopleRepository.upsert(context, person)
                        close(saved = true)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) { Text(if (existing == null) "Aggiungi persona" else "Salva modifiche") }
                if (!canSave) {
                    Text(
                        "Inserisci nome e data per salvare",
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (existing != null) {
                RoundIconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = Color.White)
                }
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
            title = { Text("Eliminare ${existing.name}?") },
            text = { Text("Il compleanno e la foto verranno rimossi dall'app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    importedPhotos.forEach { Photos.delete(context, it) }
                    PeopleRepository.delete(context, existing.id)
                    onClose()
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun PhotoBanner(photo: String?, name: String, onPick: () -> Unit) {
    val bitmap = rememberPhoto(photo, 1400)
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
            .clickable(onClick = onPick),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
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
                Text("Tocca per aggiungere una foto", color = Color.White, fontWeight = FontWeight.Medium)
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
            Text(
                name.ifBlank { "Nuova persona" },
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (bitmap != null) {
                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.25f)) {
                    Text(
                        "Cambia foto",
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
private fun RoundIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.3f)),
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@Composable
private fun BirthdayDatePicker(
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
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    ) {
        DatePicker(state = state, showModeToggle = true)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { yearUnknown = !yearUnknown }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = yearUnknown, onCheckedChange = { yearUnknown = it })
            Text("Non conosco l'anno di nascita", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CustomReminderDialog(onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    val value = text.toIntOrNull()
    val valid = value != null && value in 0..365
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifica personalizzata") },
        text = {
            Column {
                Text("Quanti giorni prima del compleanno vuoi essere avvisato?")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(3) },
                    label = { Text("Giorni prima") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !valid,
                    supportingText = { Text("Da 0 (il giorno stesso) a 365") },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { value?.let(onConfirm) }) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
