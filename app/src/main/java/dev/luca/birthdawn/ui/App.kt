@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.birthdawn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import dev.luca.birthdawn.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import dev.luca.birthdawn.ui.theme.AppearanceSettings
import dev.luca.birthdawn.ui.theme.barColor
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
fun App(
    personToOpen: String?,
    onPersonOpened: () -> Unit,
    shared: Pair<String, String?>? = null,
    onSharedHandled: () -> Unit = {},
) {
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

    if (shared != null) {
        SharedGiftDialog(
            sharedText = shared.first,
            sharedSubject = shared.second,
            people = people,
            onDone = { savedFor ->
                onSharedHandled()
                if (savedFor != null) route = PERSON + savedFor
            },
        )
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
    val appearance by AppearanceSettings.state.collectAsState()
    val shrinkEnabled = appearance.shrinkNavBar

    // Barra in basso compatta quando si scorre verso il basso, normale quando si torna su
    var collapsed by remember { mutableStateOf(false) }
    LaunchedEffect(tab, shrinkEnabled) { collapsed = false }
    val collapseConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -6f) collapsed = true
                else if (available.y > 6f) collapsed = false
                return Offset.Zero
            }
        }
    }
    val collapse by animateFloatAsState(
        targetValue = if (shrinkEnabled && collapsed) 1f else 0f,
        animationSpec = tween(250),
        label = "navCollapse",
    )

    Scaffold(
        modifier = Modifier
            .nestedScroll(collapseConnection)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo: la versione monocromatica dell'icona, nel colore d'accento
                        Icon(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(width = 44.dp, height = 32.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Birthdawn", fontWeight = FontWeight.Bold)
                            Text(
                                formatLongDate(today) + " · " +
                                    if (peopleCount == 1) "1 persona" else "$peopleCount persone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = barColor(top = true),
                ),
            )
        },
        bottomBar = {
            if (shrinkEnabled) {
                ShrinkingNavBar(tab = tab, onTab = onTab, collapse = collapse)
            } else NavigationBar(containerColor = barColor(top = false)) {
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
                expanded = collapse < 0.5f,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Aggiungi") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        content = content,
    )
}

private val NAV_ITEMS = listOf(
    Icons.Filled.Home to "Home",
    Icons.Filled.DateRange to "Calendario",
)

/**
 * Barra di navigazione che passa da normale (80dp, con etichette)
 * a compatta (56dp, solo icone) in base a [collapse] (0 = normale, 1 = compatta).
 */
@Composable
private fun ShrinkingNavBar(tab: Int, onTab: (Int) -> Unit, collapse: Float) {
    val colors = MaterialTheme.colorScheme
    Surface(color = barColor(top = false)) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                .height(lerp(80.dp, 56.dp, collapse)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NAV_ITEMS.forEachIndexed { index, (icon, label) ->
                val selected = tab == index
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            onClick = { onTab(index) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (selected) colors.secondaryContainer else Color.Transparent)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                        )
                    }
                    // L'etichetta sfuma e si restringe man mano che la barra diventa compatta
                    if (collapse < 1f) {
                        Box(
                            Modifier
                                .height(lerp(20.dp, 0.dp, collapse))
                                .graphicsLayer { alpha = 1f - collapse }
                                .padding(top = 4.dp),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
