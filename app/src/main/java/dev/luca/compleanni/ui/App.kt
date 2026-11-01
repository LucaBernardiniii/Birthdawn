package dev.luca.compleanni.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.luca.compleanni.data.PeopleRepository
import dev.luca.compleanni.notify.Notifier
import dev.luca.compleanni.notify.Prefs
import dev.luca.compleanni.notify.ReminderScheduler
import java.time.LocalDate

private const val NEW_PERSON = "__new__"

@Composable
fun App(personToOpen: String?, onPersonOpened: () -> Unit) {
    val people by PeopleRepository.people.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    LaunchedEffect(personToOpen) {
        if (personToOpen != null) {
            if (PeopleRepository.get(personToOpen) != null) editing = personToOpen
            onPersonOpened()
        }
    }

    AnimatedContent(
        targetState = editing,
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut()
        },
        label = "screen",
    ) { current ->
        if (current != null) {
            BackHandler { editing = null }
            PersonScreen(
                existing = if (current == NEW_PERSON) null else people.firstOrNull { it.id == current },
                onClose = { editing = null },
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text("Home") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                            label = { Text("Calendario") },
                        )
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { editing = NEW_PERSON },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("Aggiungi") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            ) { padding ->
                when (tab) {
                    0 -> HomeScreen(
                        people = people,
                        today = today,
                        contentPadding = padding,
                        onOpen = { editing = it.id },
                        onSettings = { showSettings = true },
                    )
                    else -> CalendarScreen(
                        people = people,
                        today = today,
                        contentPadding = padding,
                        onOpen = { editing = it.id },
                    )
                }
            }
        }
    }

    if (showSettings) SettingsDialog(onDismiss = { showSettings = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = Prefs.hour(context),
        initialMinute = Prefs.minute(context),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orario delle notifiche") },
        text = {
            Column {
                Text(
                    "Ogni giorno a quest'ora l'app controlla i compleanni e ti avvisa.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                TimePicker(state = state)
                OutlinedButton(onClick = {
                    val ok = Notifier.showTest(context)
                    if (!ok) {
                        Toast.makeText(
                            context,
                            "Notifiche disattivate: abilitale nelle impostazioni di Android",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }) { Text("Invia notifica di prova") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Prefs.setTime(context, state.hour, state.minute)
                ReminderScheduler.schedule(context)
                ReminderScheduler.runCheckIfDue(context)
                onDismiss()
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}
