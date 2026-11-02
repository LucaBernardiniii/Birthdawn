@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.birthdawn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.util.Photos
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.daysUntil
import dev.luca.birthdawn.util.formatDayMonth
import dev.luca.birthdawn.util.nextBirthday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val AvatarColors = listOf(
    Color(0xFFE8505B), Color(0xFFF2A93B), Color(0xFF4FA3A5), Color(0xFF6C63B5),
    Color(0xFF3D8BD9), Color(0xFFD96BA0), Color(0xFF5BAE6A), Color(0xFFB5734A),
)

fun avatarColor(name: String): Color =
    AvatarColors[(name.hashCode() and 0x7fffffff) % AvatarColors.size]

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
fun rememberPhoto(name: String?, maxSizePx: Int): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxSizePx) {
        value = if (name == null) null
        else withContext(Dispatchers.IO) { Photos.load(context, name, maxSizePx) }
    }
    return bitmap
}

@Composable
fun Avatar(person: Person, size: Dp, modifier: Modifier = Modifier) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val photo = rememberPhoto(person.photoPath, px * 2)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor(person.name)),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                initials(person.name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
}

/** Riga di una persona con conto alla rovescia. */
@Composable
fun BirthdayRow(person: Person, today: LocalDate, onClick: () -> Unit) {
    val days = person.daysUntil(today)
    val age = person.ageOn(person.nextBirthday(today))
    val soon = days <= 7
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Avatar(person, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatDayMonth(person.day, person.month) +
                        (age?.let { " · compie $it anni" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DaysBadge(days, highlight = soon)
        }
    }
}

@Composable
fun DaysBadge(days: Int, highlight: Boolean) {
    val bg = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (days) {
            0 -> Text("🎂", fontSize = 20.sp)
            1 -> Text("Domani", color = fg, style = MaterialTheme.typography.labelLarge)
            else -> {
                Text("$days", color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp)
                Text("giorni", color = fg, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 6.dp),
    )
}

/** Riga compatta cliccabile (usata nel calendario). */
@Composable
fun SimplePersonRow(person: Person, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(person, 40.dp)
        Column(Modifier.weight(1f)) {
            Text(person.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
