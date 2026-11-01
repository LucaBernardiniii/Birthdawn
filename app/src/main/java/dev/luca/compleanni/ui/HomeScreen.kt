@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.compleanni.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luca.compleanni.data.Person
import dev.luca.compleanni.util.ageOn
import dev.luca.compleanni.util.countdownLabel
import dev.luca.compleanni.util.daysUntil
import dev.luca.compleanni.util.monthName
import dev.luca.compleanni.util.nextBirthday
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(
    people: List<Person>,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpen: (Person) -> Unit,
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sorted = remember(people, today) {
        people.sortedWith(compareBy<Person>({ it.daysUntil(today) }, { it.name.lowercase() }))
    }
    val filtered = if (query.isBlank()) sorted else sorted.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val todayList = if (query.isBlank()) filtered.filter { it.daysUntil(today) == 0 } else emptyList()
    val upcoming = filtered.filterNot { it in todayList }
    val grouped = upcoming.groupBy { YearMonth.from(it.nextBirthday(today)) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        item(key = "header") {
            Row(
                Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Compleanni",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    val next = sorted.firstOrNull()
                    Text(
                        when {
                            next == null -> "Nessuno da festeggiare… per ora"
                            else -> "${people.size} ${if (people.size == 1) "persona" else "persone"} · " +
                                "prossimo: ${next.name}, ${countdownLabel(next.daysUntil(today)).lowercase()}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Impostazioni notifiche")
                }
            }
        }

        if (people.isEmpty()) {
            item(key = "empty") { EmptyState() }
            return@LazyColumn
        }

        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cerca una persona") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancella")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (todayList.isNotEmpty()) {
            item(key = "today") { TodayCard(todayList, today, onOpen) }
        }

        if (filtered.isEmpty()) {
            item(key = "noresults") {
                Text(
                    "Nessun risultato per \"$query\"",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        grouped.forEach { (ym, list) ->
            item(key = "h-$ym") {
                SectionHeader(
                    if (ym.year == today.year) monthName(ym.monthValue)
                    else "${monthName(ym.monthValue)} ${ym.year}"
                )
            }
            items(list, key = { it.id }) { person ->
                BirthdayRow(person, today) { onOpen(person) }
            }
        }
    }
}

@Composable
private fun TodayCard(list: List<Person>, today: LocalDate, onOpen: (Person) -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
                .padding(20.dp),
        ) {
            Text("🎉 Oggi si festeggia", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            list.forEach { person ->
                Surface(
                    onClick = { onOpen(person) },
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(person, 52.dp)
                        Column {
                            Text(person.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                person.ageOn(today)?.let { "Compie $it anni" } ?: "Fai gli auguri!",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(28.dp),
        ) { Text("🎂", fontSize = 56.sp) }
        Spacer(Modifier.height(24.dp))
        Text(
            "Ancora nessun compleanno",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tocca \"Aggiungi\" per salvare la prima persona. Ti avviserò 7 giorni prima e il giorno stesso.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
