package dev.luca.birthdawn.ui

import dev.luca.birthdawn.util.AppLanguage
import dev.luca.birthdawn.util.weekdayInitials
import dev.luca.birthdawn.util.turnsLabel
import dev.luca.birthdawn.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.birthdayIn
import dev.luca.birthdawn.util.countdownLabel
import dev.luca.birthdawn.util.daysUntil
import dev.luca.birthdawn.util.formatDayMonth
import dev.luca.birthdawn.util.formatLongDate
import dev.luca.birthdawn.util.monthName
import dev.luca.birthdawn.util.occursOn
import java.time.LocalDate
import java.time.YearMonth


@Composable
fun CalendarScreen(
    people: List<Person>,
    today: LocalDate,
    contentPadding: PaddingValues,
    onOpen: (Person) -> Unit,
) {
    val context = LocalContext.current
    val weekdays = remember { weekdayInitials() }
    // Mese visualizzato salvato come indice (anno*12 + mese-1) per sopravvivere alle rotazioni.
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    var selectedStr by rememberSaveable { mutableStateOf(today.toString()) }
    val ym = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val selected = LocalDate.parse(selectedStr)

    val byDay: Map<Int, List<Person>> = remember(people, ym) {
        (1..ym.lengthOfMonth()).associateWith { d -> people.filter { it.occursOn(ym.atDay(d)) } }
    }
    val selectedPeople = remember(people, selected) {
        people.filter { it.occursOn(selected) }.sortedBy { it.name.lowercase() }
    }
    val monthPeople = remember(people, ym) {
        people.filter { birthdayIn(ym.year, it.month, it.day).month == ym.month }
            .sortedWith(compareBy<Person>({ birthdayIn(ym.year, it.month, it.day).dayOfMonth }, { it.name.lowercase() }))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        item(key = "top") { Spacer(Modifier.height(8.dp)) }

        item(key = "month") {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    Modifier
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onDragEnd = {
                                    if (total > 120f) monthIndex -= 1
                                    else if (total < -120f) monthIndex += 1
                                },
                            ) { _, delta -> total += delta }
                        },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { monthIndex -= 1 }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.prev_month))
                        }
                        Text(
                            "${monthName(ym.monthValue)} ${ym.year}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        IconButton(onClick = { monthIndex += 1 }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        weekdays.forEach {
                            Text(
                                it,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val offset = ym.atDay(1).dayOfWeek.value - 1
                    val totalCells = ((offset + ym.lengthOfMonth() + 6) / 7) * 7
                    for (week in 0 until totalCells / 7) {
                        Row {
                            for (col in 0 until 7) {
                                val dayNum = week * 7 + col - offset + 1
                                if (dayNum in 1..ym.lengthOfMonth()) {
                                    val date = ym.atDay(dayNum)
                                    DayCell(
                                        day = dayNum,
                                        isToday = date == today,
                                        isSelected = date == selected,
                                        count = byDay[dayNum]?.size ?: 0,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedStr = date.toString() },
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                    if (ym != YearMonth.from(today) || selected != today) {
                        TextButton(
                            onClick = {
                                monthIndex = today.year * 12 + today.monthValue - 1
                                selectedStr = today.toString()
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) { Text(stringResource(R.string.back_to_today)) }
                    }
                }
            }
        }

        item(key = "selected-header") {
            SectionHeader(formatLongDate(selected) + if (selected == today) stringResource(R.string.today_suffix) else "")
        }
        if (selectedPeople.isEmpty()) {
            item(key = "selected-empty") {
                Text(
                    stringResource(R.string.no_birthdays_day),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(selectedPeople, key = { "sel-" + it.id }) { person ->
                val age = person.ageOn(selected)
                SimplePersonRow(
                    person,
                    subtitle = (age?.let { turnsLabel(context, it, capitalized = true) } ?: stringResource(R.string.birthday)) +
                        " · " + countdownLabel(context, person.daysUntil(today)).lowercase(),
                ) { onOpen(person) }
            }
        }

        item(key = "month-header") {
            SectionHeader(stringResource(
                    R.string.all_birthdays_in,
                    monthName(ym.monthValue).let { if (AppLanguage.isItalian) it.lowercase() else it },
                ))
        }
        if (monthPeople.isEmpty()) {
            item(key = "month-empty") {
                Text(
                    stringResource(R.string.no_birthdays_month),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(monthPeople, key = { "m-" + it.id }) { person ->
                SimplePersonRow(person, subtitle = formatDayMonth(person.day, person.month)) {
                    selectedStr = birthdayIn(ym.year, person.month, person.day).toString()
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    count: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val bg = when {
        isSelected -> colors.primary
        count > 0 -> colors.primaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        isSelected -> colors.onPrimary
        count > 0 -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    Box(
        modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (isToday && !isSelected) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                color = fg,
                fontWeight = if (count > 0 || isToday) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (count > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(minOf(count, 3)) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(fg)
                        )
                    }
                }
            }
        }
    }
}
