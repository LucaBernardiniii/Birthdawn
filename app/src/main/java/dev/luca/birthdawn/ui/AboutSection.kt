package dev.luca.birthdawn.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.luca.birthdawn.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Link pubblici del progetto. */
object AppLinks {
    const val PROFILE = "https://github.com/LucaBernardiniii"
    const val REPO = "https://github.com/LucaBernardiniii/Birthdawn"
    const val ISSUES = "$REPO/issues"
    const val LICENSE = "$REPO/blob/main/LICENSE"
    const val DEVELOPER = "Luca Bernardini"
}

/** Forma "a biscotto" (cerchio ondulato) per l'avatar dello sviluppatore. */
private val CookieShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = min(cx, cy)
    val lobes = 9
    val steps = 180
    for (i in 0..steps) {
        val a = 2.0 * PI * i / steps
        val rr = r * (0.91f + 0.09f * cos(lobes * a).toFloat())
        val x = cx + rr * cos(a).toFloat()
        val y = cy + rr * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val DawnAvatar = Brush.linearGradient(
    listOf(Color(0xFF3B6FB6), Color(0xFF9C5A8E), Color(0xFFF28C50)),
)

/** Sezione "Informazioni" delle impostazioni: app, sviluppatore e link. */
@Composable
fun AboutSection() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()
    }

    fun openLink(url: String) {
        runCatching { uriHandler.openUri(url) }.onFailure {
            Toast.makeText(context, context.getString(R.string.open_link_failed), Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---------- App ----------
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(width = 66.dp, height = 48.dp),
                )
                Spacer(Modifier.width(20.dp))
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (version.isNotEmpty()) InfoChip(version)
                        InfoChip("GPL-3.0")
                    }
                }
            }
        }

        // ---------- Sviluppatore ----------
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(84.dp)
                            .clip(CookieShape)
                            .background(DawnAvatar),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            AppLinks.DEVELOPER.take(1),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(
                            AppLinks.DEVELOPER,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.developer_role),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(R.drawable.ic_github, stringResource(R.string.github_profile), Modifier.weight(1f)) {
                        openLink(AppLinks.PROFILE)
                    }
                    PillButton(R.drawable.ic_code, stringResource(R.string.source_code), Modifier.weight(1f)) {
                        openLink(AppLinks.REPO)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { openLink(AppLinks.REPO) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.star_on_github),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ---------- Link utili ----------
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                LinkRow(R.drawable.ic_bug, stringResource(R.string.report_bug), stringResource(R.string.report_bug_sub)) {
                    openLink(AppLinks.ISSUES)
                }
                LinkRow(R.drawable.ic_license, stringResource(R.string.license), stringResource(R.string.license_sub)) {
                    openLink(AppLinks.LICENSE)
                }
            }
        }

        Text(
            stringResource(R.string.made_with_love),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors().surfaceContainerHighest,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colors().onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PillButton(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(56.dp)) {
        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun LinkRow(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null, tint = colors().primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun colors() = MaterialTheme.colorScheme
