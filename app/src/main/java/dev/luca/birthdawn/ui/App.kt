@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.birthdawn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import dev.luca.birthdawn.data.PeopleRepository
import dev.luca.birthdawn.util.formatLongDate
import java.time.LocalDate

// Rotte: "main", "settings", "person:<id>" (visualizza), "edit:<id>" (modifica), "edit:" (nuova persona)
private const val ROUTE_MAIN = "main"
private const val ROUTE_SETTINGS = "settings"
private const val PERSON = "person:"
private const val EDIT = "edit:"

@Composable
fun App(personToOpen: String?, onPersonOpened: () -> Unit) {
    val people by PeopleRepository.people.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf(ROUTE_MAIN) }
    val today = LocalDate.now()

    LaunchedEffect(personToOpen) {
        if (personToOpen != null) {
            if (PeopleRepository.get(personToOpen) != null) route = PERSON + personToOpen
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

            current.startsWith(PERSON) -> {
                BackHandler { route = ROUTE_MAIN }
                val person = people.firstOrNull { it.id == current.removePrefix(PERSON) }
                if (person == null) {
                    LaunchedEffect(Unit) { route = ROUTE_MAIN }
                } else {
                    PersonDetailScreen(
                        person = person,
                        onBack = { route = ROUTE_MAIN },
                        onEdit = { route = EDIT + person.id },
                        onDeleted = { route = ROUTE_MAIN },
                    )
                }
            }

            current.startsWith(EDIT) -> {
                val id = current.removePrefix(EDIT)
                val existing = if (id.isEmpty()) null else people.firstOrNull { it.id == id }
                val back = { route = if (existing != null) PERSON + existing.id else ROUTE_MAIN }
                BackHandler { back() }
                PersonScreen(
                    existing = existing,
                    onSaved = { savedId -> route = PERSON + savedId },
                    onCancel = back,
                    onDeleted = { route = ROUTE_MAIN },
                )
            }

            else -> MainScreen(
                tab = tab,
                onTab = { tab = it },
                today = today,
                peopleCount = people.size,
                onSettings = { route = ROUTE_SETTINGS },
                onAdd = { route = EDIT },
            ) { padding ->
                when (tab) {
                    0 -> HomeScreen(
                        people = people,
                        today = today,
                        contentPadding = padding,
                        onOpen = { route = PERSON + it.id },
                    )
                    else -> CalendarScreen(
                        people = people,
                        today = today,
                        contentPadding = padding,
                        onOpen = { route = PERSON + it.id },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    tab: Int,
    onTab: (Int) -> Unit,
    today: LocalDate,
    peopleCount: Int,
    onSettings: () -> Unit,
    onAdd: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Birthdawn", fontWeight = FontWeight.Bold)
                        Text(
                            formatLongDate(today) + " · " +
                                if (peopleCount == 1) "1 persona" else "$peopleCount persone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { onTab(0) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { onTab(1) },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("Calendario") },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Aggiungi") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        content = content,
    )
}
