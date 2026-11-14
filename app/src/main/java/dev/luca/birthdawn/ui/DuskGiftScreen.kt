package dev.luca.birthdawn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luca.birthdawn.R
import dev.luca.birthdawn.ui.theme.duskBackdrop

/**
 * Schermata del regalo di compleanno: annuncia che l'aspetto "Dusk" (tramonto) è sbloccato.
 * Usa sempre il gradiente del tramonto scuro, così si legge bene in qualunque tema.
 */
@Composable
fun DuskGiftScreen(name: String?, onUseNow: () -> Unit, onLater: () -> Unit) {
    BackHandler(onBack = onLater)

    // Entrata: il regalo "salta" dentro, poi il testo compare
    val pop = remember { Animatable(0.3f) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        reveal.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }
    // Il regalo ondeggia piano
    val wobble by rememberInfiniteTransition(label = "gift").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "wobble",
    )

    val onDusk = Color(0xFFFFF1F4)
    Box(
        Modifier
            .fillMaxSize()
            .background(duskBackdrop(dark = true).brush)
            // Intercetta i tocchi, così non arrivano alla schermata sotto
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                "🎁",
                fontSize = 96.sp,
                modifier = Modifier
                    .scale(pop.value)
                    .graphicsLayer { rotationZ = wobble },
            )
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier
                    .alpha(reveal.value)
                    .graphicsLayer { translationY = (1f - reveal.value) * 40f },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (name.isNullOrBlank()) stringResource(R.string.gift_title)
                    else stringResource(R.string.gift_title_named, name),
                    color = onDusk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.gift_text),
                    color = onDusk.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(28.dp))
                // Anteprima: il tramonto chiaro e scuro
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(false, true).forEach { dark ->
                        Box(
                            Modifier
                                .size(width = 96.dp, height = 150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(duskBackdrop(dark).brush)
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.gift_where),
                    color = onDusk.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(Modifier.alpha(reveal.value), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onUseNow,
                    colors = ButtonDefaults.buttonColors(containerColor = onDusk, contentColor = Color(0xFF5E2150)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(stringResource(R.string.gift_use_now), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp).height(8.dp))
                TextButton(onClick = onLater) {
                    Text(stringResource(R.string.gift_later), color = onDusk)
                }
            }
        }
    }
}
