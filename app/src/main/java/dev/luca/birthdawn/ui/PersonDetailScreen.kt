@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package dev.luca.birthdawn.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlin.math.max
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luca.birthdawn.data.PeopleRepository
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.notify.Prefs
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.countdownLabel
import dev.luca.birthdawn.util.daysUntil
import dev.luca.birthdawn.util.formatDayMonth
import dev.luca.birthdawn.util.formatLongDate
import dev.luca.birthdawn.util.nextBirthday
import dev.luca.birthdawn.util.reminderLabel
import java.time.LocalDate

/** Pagina di visualizzazione di una persona (sola lettura). */
@Composable
fun PersonDetailScreen(
    person: Person,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val next = person.nextBirthday(today)
    val days = person.daysUntil(today)
    val turning = person.ageOn(next)
    val currentAge = person.year?.let { y ->
        val thisYear = today.year - y
        if (days == 0) thisYear else (turning ?: thisYear) - 1
    }?.takeIf { it >= 0 }
    var confirmDelete by remember { mutableStateOf(false) }

    // ---- Banner elastico: tirando giù dall'alto il banner si allunga e mostra più foto ----
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val bannerPx = with(density) { BANNER_HEIGHT.toPx() }
    val bitmap = rememberPhoto(person.photoPath, 1400)
    // Allungamento massimo: quanto serve a vedere la foto intera (almeno 120dp)
    val maxStretch = remember(bitmap, screenWidthPx, bannerPx) {
        val minStretch = with(density) { 120.dp.toPx() }
        if (bitmap == null) minStretch
        else (screenWidthPx * bitmap.height / bitmap.width - bannerPx).coerceAtLeast(minStretch)
    }
    var stretch by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    val stretchConnection = remember(maxStretch) {
        object : NestedScrollConnection {
            // Se il banner è allungato e si scorre verso l'alto, prima si accorcia il banner
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && stretch > 0f) {
                    val used = max(available.y, -stretch)
                    stretch += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            // In cima alla pagina, il trascinamento verso il basso allunga il banner (con resistenza)
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.Drag) {
                    val resistance = 0.55f * (1f - stretch / maxStretch).coerceIn(0.15f, 1f)
                    stretch = (stretch + available.y * resistance).coerceAtMost(maxStretch)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            // Al rilascio del dito il banner torna morbidamente alla sua altezza
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (stretch > 0f) {
                    animate(stretch, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) { v, _ ->
                        stretch = v
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    PhotoAccentTheme(person.photoPath) {
        // Disattiva l'effetto "stiramento" di sistema: lo sostituisce il banner elastico
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(stretchConnection)
                    .verticalScroll(scrollState),
            ) {
                PhotoBanner(
                    photo = person.photoPath,
                    name = person.name,
                    onPick = null,
                    subtitle = formatDayMonth(person.day, person.month) + (person.year?.let { " $it" } ?: ""),
                    focus = person.photoFocus,
                    height = BANNER_HEIGHT + with(density) { stretch.toDp() },
                )

                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CountdownCard(person = person, days = days, nextDate = next, turning = turning)

                    InfoCard {
                        InfoRow(
                            Icons.Filled.DateRange,
                            "Compleanno",
                            formatDayMonth(person.day, person.month) + (person.year?.let { " $it" } ?: ""),
                        )
                        if (currentAge != null) {
                            InfoRow(Icons.Filled.Star, "Età attuale", "$currentAge anni")
                        }
                    }

                    GiftIdeasCard(person)

                    if (person.notes.isNotBlank()) {
                        InfoCard {
                            InfoRow(Icons.Filled.Info, "Note", person.notes)
                        }
                    }

                    InfoCard {
                        Row(
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text("Notifiche", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (person.reminderDays.isEmpty()) "Nessuna notifica attiva"
                                    else "Alle ${Prefs.timeLabel(context)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        if (person.reminderDays.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 8.dp),
                            ) {
                                person.reminderDays.sortedDescending().forEach { d ->
                                    SuggestionChip(onClick = onEdit, label = { Text(reminderLabel(d)) })
                                }
                            }
                        } else {
                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    FilledTonalButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Modifica")
                    }
                    Spacer(Modifier.navigationBarsPadding().height(16.dp))
                }
            }

            // Pulsanti sopra il banner
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                RoundIconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifica", tint = Color.White)
                }
                RoundIconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina", tint = Color.White)
                }
            }
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Eliminare ${person.name}?") },
                text = { Text("Il compleanno e la foto verranno rimossi dall'app.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        PeopleRepository.delete(context, person.id)
                        onDeleted()
                    }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
            )
        }
        } // CompositionLocalProvider
    }
}

@Composable
private fun CountdownCard(person: Person, days: Int, nextDate: LocalDate, turning: Int?) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp)) {
            if (days == 0) {
                Text("🎂 Oggi!", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$days",
                        fontSize = 56.sp,
                        lineHeight = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (days == 1) "giorno" else "giorni",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            Text(
                formatLongDate(nextDate) +
                    (if (nextDate.year != LocalDate.now().year) " ${nextDate.year}" else "") +
                    (turning?.let { " · compie $it anni" } ?: ""),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            if (days in 1..7) {
                Text(
                    countdownLabel(days) + "! " + if (person.gifts.any { !it.bought }) "Dai un'occhiata alle idee regalo 🎁" else "Hai pensato al regalo?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
