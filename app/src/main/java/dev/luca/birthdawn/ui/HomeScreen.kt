@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.birthdawn.ui

import dev.luca.birthdawn.util.turnsLabel
import dev.luca.birthdawn.util.plural
import dev.luca.birthdawn.R
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.util.birthdayIn
import dev.luca.birthdawn.notify.Prefs
import androidx.compose.ui.platform.LocalContext
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.countdownLabel
import dev.luca.birthdawn.util.daysUntil
import dev.luca.birthdawn.util.formatDayMonth
import dev.luca.birthdawn.util.monthName
import dev.luca.birthdawn.util.nextBirthday
import java.time.LocalDate
import java.time.YearMonth

private const val CAROUSEL_SIZE = 10

/** Ordinamento della lista di persone in Home. */
private enum class HomeSort(@StringRes val label: Int) {
    UPCOMING(R.string.sort_upcoming),
    ALPHABETICAL(R.string.sort_az),
    CALENDAR(R.string.sort_calendar),
}

@Composable
fun HomeScreen(
    people: List<Person>,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpen: (Person) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    var sort by rememberSaveable {
        mutableStateOf(runCatching { HomeSort.valueOf(Prefs.homeSort(context)) }.getOrDefault(HomeSort.UPCOMING))
    }
    val sorted = remember(people, today) {
        people.sortedWith(compareBy<Person>({ it.daysUntil(today) }, { it.name.lowercase() }))
    }
    val searching = query.isNotBlank()
    val filtered = if (!searching) sorted else sorted.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val todayList = if (!searching) filtered.filter { it.daysUntil(today) == 0 } else emptyList()
    val upcoming = filtered.filterNot { it in todayList }
    val carousel = if (!searching) upcoming.take(CAROUSEL_SIZE) else emptyList()

    // Sezioni della lista (titolo -> persone) in base all'ordinamento scelto
    val sections: List<Pair<String, List<Person>>> = remember(filtered, upcoming, sort, today) {
        when (sort) {
            HomeSort.UPCOMING -> upcoming
                .groupBy { YearMonth.from(it.nextBirthday(today)) }
                .map { (ym, list) ->
                    (if (ym.year == today.year) monthName(ym.monthValue)
                    else "${monthName(ym.monthValue)} ${ym.year}") to list
                }
            HomeSort.ALPHABETICAL -> filtered
                .sortedBy { it.name.lowercase() }
                .groupBy { it.name.trim().firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() } ?: '#' }
                .map { (letter, list) -> letter.toString() to list }
            HomeSort.CALENDAR -> filtered
                .sortedWith(compareBy<Person>({ it.month }, { it.day }, { it.name.lowercase() }))
                .groupBy { it.month }
                .map { (month, list) -> monthName(month) to list }
        }
    }

    // (nome, età) se oggi è il compleanno dell'utente
    val myBirthdayToday = remember(today) {
        Prefs.myBirthday(context)
            ?.takeIf { birthdayIn(today.year, it.month, it.day) == today }
            ?.let { it.name.trim() to it.year?.let { y -> today.year - y } }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        val me = myBirthdayToday
        if (me != null && !searching) {
            item(key = "me") { OwnBirthdayCard(me.first, me.second) }
        }

        if (people.isEmpty()) {
            item(key = "empty") { EmptyState() }
            return@LazyColumn
        }

        if (todayList.isNotEmpty()) {
            item(key = "today") { TodayCard(todayList, today, onOpen) }
        }

        if (carousel.isNotEmpty()) {
            item(key = "carousel-h") { SectionHeader(stringResource(R.string.section_upcoming), Modifier.padding(top = 0.dp)) }
            item(key = "carousel") { UpcomingCarousel(carousel, today, onOpen) }
        }

        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
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

        item(key = "sort") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.sort_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HomeSort.entries.forEach { option ->
                    val selected = sort == option
                    FilterChip(
                        selected = selected,
                        onClick = {
                            sort = option
                            Prefs.setHomeSort(context, option.name)
                        },
                        label = { Text(stringResource(option.label)) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else null,
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "noresults") {
                Text(
                    stringResource(R.string.no_results, query),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        sections.forEachIndexed { index, (title, list) ->
            item(key = "h-${sort.name}-$index-$title") { SectionHeader(title) }
            items(list, key = { "${sort.name}-${it.id}" }) { person ->
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
            FocusedImage(photo, person.photoFocus, Modifier.fillMaxSize())
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
                countdownLabel(LocalContext.current, days),
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
                formatDayMonth(person.day, person.month) + (age?.let { " · " + plural(LocalContext.current, R.plurals.age_years, it) } ?: ""),
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
    // Testo leggibile sul gradiente primary → tertiary con qualsiasi tema (Alba, Material You, personalizzato)
    val onCard = MaterialTheme.colorScheme.onPrimary
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
            stringResource(R.string.today_celebrating),
            color = onCard,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        list.forEach { person ->
            Surface(
                onClick = { onOpen(person) },
                color = onCard.copy(alpha = 0.14f),
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
                        Text(person.name, color = onCard, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            person.ageOn(today)?.let { turnsLabel(LocalContext.current, it, capitalized = true) }
                                ?: stringResource(R.string.say_happy_birthday),
                            color = onCard.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnBirthdayCard(name: String, age: Int?) {
    // Testo leggibile sul gradiente primary → tertiary con qualsiasi tema (Alba, Material You, personalizzato)
    val onCard = MaterialTheme.colorScheme.onPrimary
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🥳🎂🎉", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            if (name.isNotEmpty()) stringResource(R.string.own_birthday_title_named, name)
            else stringResource(R.string.own_birthday_title),
            color = onCard,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            (age?.takeIf { it > 0 }?.let { plural(LocalContext.current, R.plurals.own_birthday_age, it) } ?: "") +
                stringResource(R.string.own_birthday_wishes),
            color = onCard.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
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
            stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_text),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
