@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.compleanni.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luca.compleanni.data.Person
import dev.luca.compleanni.util.ageOn
import dev.luca.compleanni.util.countdownLabel
import dev.luca.compleanni.util.daysUntil
import dev.luca.compleanni.util.formatDayMonth
import dev.luca.compleanni.util.monthName
import dev.luca.compleanni.util.nextBirthday
import java.time.LocalDate
import java.time.YearMonth

private const val CAROUSEL_SIZE = 10

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
    val searching = query.isNotBlank()
    val filtered = if (!searching) sorted else sorted.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val todayList = if (!searching) filtered.filter { it.daysUntil(today) == 0 } else emptyList()
    val upcoming = filtered.filterNot { it in todayList }
    val carousel = if (!searching) upcoming.take(CAROUSEL_SIZE) else emptyList()
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
                    Text(
                        if (people.isEmpty()) "Nessuno da festeggiare… per ora"
                        else "${people.size} ${if (people.size == 1) "persona" else "persone"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
                }
            }
        }

        if (people.isEmpty()) {
            item(key = "empty") { EmptyState() }
            return@LazyColumn
        }

        if (todayList.isNotEmpty()) {
            item(key = "today") { TodayCard(todayList, today, onOpen) }
        }

        if (carousel.isNotEmpty()) {
            item(key = "carousel-h") { SectionHeader("In arrivo") }
            item(key = "carousel") { UpcomingCarousel(carousel, today, onOpen) }
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
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
            )
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

/** Carosello Material 3 dei prossimi compleanni. */
@Composable
private fun UpcomingCarousel(list: List<Person>, today: LocalDate, onOpen: (Person) -> Unit) {
    val state = rememberCarouselState { list.size }
    HorizontalMultiBrowseCarousel(
        state = state,
        preferredItemWidth = 200.dp,
        itemSpacing = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp),
    ) { index ->
        val person = list[index]
        CarouselCard(
            person = person,
            today = today,
            modifier = Modifier
                .fillMaxHeight()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .clickable { onOpen(person) },
        )
    }
}

@Composable
private fun CarouselCard(person: Person, today: LocalDate, modifier: Modifier) {
    val photo = rememberPhoto(person.photoPath, 700)
    val days = person.daysUntil(today)
    val age = person.ageOn(person.nextBirthday(today))
    val base = avatarColor(person.name)

    Box(modifier) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = person.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.65f).compositeOverBlack()))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(person.name),
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                    softWrap = false,
                )
            }
        }

        // Badge in alto con i giorni mancanti
        Surface(
            shape = RoundedCornerShape(50),
            color = if (days <= 7) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        ) {
            Text(
                countdownLabel(days),
                color = if (days <= 7) MaterialTheme.colorScheme.onPrimary else Color.White,
                style = MaterialTheme.typography.labelLarge,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Nome e data in basso su velo scuro
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 12.dp),
        ) {
            Text(
                person.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatDayMonth(person.day, person.month) + (age?.let { " · $it anni" } ?: ""),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Color.compositeOverBlack(): Color =
    Color(red * alpha, green * alpha, blue * alpha, 1f)

@Composable
private fun TodayCard(list: List<Person>, today: LocalDate, onOpen: (Person) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(20.dp),
    ) {
        Text(
            "🎉 Oggi si festeggia",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
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
