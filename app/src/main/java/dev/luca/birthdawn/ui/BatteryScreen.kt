@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.birthdawn.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.luca.birthdawn.R
import dev.luca.birthdawn.notify.Notifier

/** Funzioni per controllare e aprire le impostazioni di batteria e notifiche. */
object BatteryHelper {

    fun isUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Mostra il dialogo di sistema "Consentire all'app di essere sempre in esecuzione in background?". */
    @SuppressLint("BatteryLife")
    fun requestUnrestricted(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        if (!start(context, direct)) start(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    /** Pagina "Info app" di Birthdawn nelle impostazioni di Android. */
    fun openAppSettings(context: Context) {
        start(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        if (!start(context, intent)) openAppSettings(context)
    }

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    /** Marche note per chiudere le app in background, con il nome usato su dontkillmyapp.com. */
    data class Vendor(val tip: Int, val slug: String, val label: String)

    fun vendor(): Vendor? {
        val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return when {
            listOf("xiaomi", "redmi", "poco").any { it in m } -> Vendor(R.string.battery_tip_xiaomi, "xiaomi", "Xiaomi")
            "samsung" in m -> Vendor(R.string.battery_tip_samsung, "samsung", "Samsung")
            "huawei" in m -> Vendor(R.string.battery_tip_huawei, "huawei", "Huawei")
            "honor" in m -> Vendor(R.string.battery_tip_huawei, "honor", "Honor")
            "oneplus" in m -> Vendor(R.string.battery_tip_oppo, "oneplus", "OnePlus")
            "oppo" in m -> Vendor(R.string.battery_tip_oppo, "oppo", "Oppo")
            "realme" in m -> Vendor(R.string.battery_tip_oppo, "realme", "Realme")
            listOf("vivo", "iqoo").any { it in m } -> Vendor(R.string.battery_tip_vivo, "vivo", "Vivo")
            else -> null
        }
    }
}

/**
 * Schermata "Notifiche puntuali": spiega e aiuta a togliere le restrizioni della batteria,
 * così i promemoria arrivano anche quando l'app non è aperta.
 */
@Composable
fun BatteryScreen(firstRun: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Ricontrolla lo stato ogni volta che si torna nell'app dalle impostazioni di sistema
    var refresh by remember { mutableIntStateOf(0) }
    @Suppress("DEPRECATION")
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationsOk = remember(refresh) { Notifier.canNotify(context) }
    val batteryOk = remember(refresh) { BatteryHelper.isUnrestricted(context) }
    val vendor = remember { BatteryHelper.vendor() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.battery_title)) },
                navigationIcon = {
                    if (!firstRun) IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Introduzione
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text("🔋", fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.battery_headline),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.battery_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StepCard(
                number = 1,
                done = notificationsOk,
                title = stringResource(R.string.step_notifications_title),
                text = stringResource(
                    if (notificationsOk) R.string.step_notifications_ok else R.string.step_notifications_text
                ),
                action = if (notificationsOk) null else stringResource(R.string.open_settings),
                onAction = { BatteryHelper.openNotificationSettings(context) },
            )
            StepCard(
                number = 2,
                done = batteryOk,
                title = stringResource(R.string.step_battery_title),
                text = stringResource(if (batteryOk) R.string.step_battery_ok else R.string.step_battery_text),
                action = if (batteryOk) null else stringResource(R.string.allow),
                onAction = { BatteryHelper.requestUnrestricted(context) },
            )
            StepCard(
                number = 3,
                done = null,
                title = stringResource(R.string.step_unrestricted_title),
                text = stringResource(R.string.step_unrestricted_text),
                action = stringResource(R.string.open_app_info),
                onAction = { BatteryHelper.openAppSettings(context) },
            )
            if (vendor != null) {
                StepCard(
                    number = 4,
                    done = null,
                    title = stringResource(R.string.step_vendor_title, vendor.label),
                    text = stringResource(vendor.tip),
                    action = stringResource(R.string.open_app_info),
                    onAction = { BatteryHelper.openAppSettings(context) },
                    secondaryAction = stringResource(R.string.more_info),
                    onSecondary = {
                        runCatching { uriHandler.openUri("https://dontkillmyapp.com/${vendor.slug}") }
                    },
                )
            }

            OutlinedButton(
                onClick = {
                    if (!Notifier.showTest(context)) {
                        Toast.makeText(context, context.getString(R.string.notifications_disabled), Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.send_test)) }

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    stringResource(if (firstRun) R.string.continue_label else R.string.done),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

/** Un passaggio numerato. [done]: true = fatto, false = da fare, null = da controllare a mano. */
@Composable
private fun StepCard(
    number: Int,
    done: Boolean?,
    title: String,
    text: String,
    action: String?,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondary: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(if (done == true) colors.primary else colors.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (done == true) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(18.dp))
                } else {
                    Text("$number", fontWeight = FontWeight.Bold, color = colors.onSecondaryContainer)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (action != null || secondaryAction != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (action != null) FilledTonalButton(onClick = onAction) { Text(action) }
                        if (secondaryAction != null) TextButton(onClick = onSecondary) { Text(secondaryAction) }
                    }
                }
            }
        }
    }
}
