package dev.luca.compleanni.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.luca.compleanni.data.PeopleRepository
import java.time.LocalDate

private const val ROUTE_MAIN = "main"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_NEW_PERSON = "person:"
private fun personRoute(id: String) = "person:$id"

@Composable
fun App(personToOpen: String?, onPersonOpened: () -> Unit) {
    val people by PeopleRepository.people.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf(ROUTE_MAIN) }
    val today = LocalDate.now()

    LaunchedEffect(personToOpen) {
        if (personToOpen != null) {
            if (PeopleRepository.get(personToOpen) != null) route = personRoute(personToOpen)
            onPersonOpened()
        }
    }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut()
        },
        label = "screen",
    ) { current ->
        when {
            current == ROUTE_SETTINGS -> {
                BackHandler { route = ROUTE_MAIN }
                SettingsScreen(onBack = { route = ROUTE_MAIN })
            }

            current.startsWith("person:") -> {
                BackHandler { route = ROUTE_MAIN }
                val id = current.removePrefix("person:")
                PersonScreen(
                    existing = if (id.isEmpty()) null else people.firstOrNull { it.id == id },
                    onClose = { route = ROUTE_MAIN },
                )
            }

            else -> Scaffold(
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
                        onClick = { route = ROUTE_NEW_PERSON },
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
                        onOpen = { route = personRoute(it.id) },
                        onSettings = { route = ROUTE_SETTINGS },
                    )
                    else -> CalendarScreen(
                        people = people,
                        today = today,
                        contentPadding = padding,
                        onOpen = { route = personRoute(it.id) },
                    )
                }
            }
        }
    }
}
