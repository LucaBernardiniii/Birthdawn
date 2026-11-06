@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.luca.birthdawn.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.luca.birthdawn.R
import dev.luca.birthdawn.data.ContactsImport
import dev.luca.birthdawn.data.ContactsImport.ContactBirthday
import dev.luca.birthdawn.util.formatBirthday
import dev.luca.birthdawn.util.plural
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Restituisce una funzione che, se serve, chiede il permesso per i contatti
 * e poi chiama [onGranted]. Se l'utente rifiuta chiama [onDenied].
 */
@Composable
fun rememberContactsPermission(onGranted: () -> Unit, onDenied: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted() else {
            Toast.makeText(context, context.getString(R.string.contacts_denied), Toast.LENGTH_LONG).show()
            onDenied()
        }
    }
    return {
        if (ContactsImport.hasPermission(context)) onGranted()
        else launcher.launch(Manifest.permission.READ_CONTACTS)
    }
}

/** Dialogo del primo avvio: "Vuoi importare i compleanni dalla rubrica?" */
@Composable
fun ContactsWelcomeDialog(onImport: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Text("📇", fontSize = 32.sp) },
        title = { Text(stringResource(R.string.contacts_welcome_title), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.contacts_welcome_text)) },
        confirmButton = { Button(onClick = onImport) { Text(stringResource(R.string.contacts_allow)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.not_now)) } },
    )
}

/** Elenco dei compleanni trovati in rubrica, da selezionare e importare. */
@Composable
fun ContactsImportScreen(onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var contacts by remember { mutableStateOf<List<ContactBirthday>?>(null) }
    val selected = remember { mutableStateListOf<String>() }
    var withPhotos by remember { mutableStateOf(true) }
    var importing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val found = withContext(Dispatchers.IO) {
            runCatching { ContactsImport.findBirthdays(context) }.getOrDefault(emptyList())
        }
        contacts = found
        selected.clear()
        selected.addAll(found.filterNot { it.alreadyAdded }.map { it.lookupKey })
    }

    val selectable = contacts.orEmpty().filterNot { it.alreadyAdded }
    val allSelected = selectable.isNotEmpty() && selected.size == selectable.size

    fun runImport() {
        val chosen = contacts.orEmpty().filter { it.lookupKey in selected }
        if (chosen.isEmpty()) return
        importing = true
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                runCatching { ContactsImport.importSelected(context, chosen, withPhotos) }.getOrDefault(0)
            }
            importing = false
            Toast.makeText(context, plural(context, R.plurals.contacts_imported, count), Toast.LENGTH_LONG).show()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_contacts)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (selectable.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.import_photos_too),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = withPhotos, onCheckedChange = { withPhotos = it })
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { runImport() },
                            enabled = selected.isNotEmpty() && !importing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(
                                if (selected.isEmpty()) stringResource(R.string.select_someone)
                                else plural(context, R.plurals.import_n_people, selected.size),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val list = contacts
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            list.isEmpty() -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📭", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.no_contact_birthdays),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.no_contact_birthdays_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBack) { Text(stringResource(R.string.done)) }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                item {
                    Text(
                        plural(context, R.plurals.contacts_found, list.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                if (selectable.isNotEmpty()) item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (allSelected) selected.clear()
                                else {
                                    selected.clear()
                                    selected.addAll(selectable.map { it.lookupKey })
                                }
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = allSelected, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                        Text(
                            stringResource(R.string.select_all),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                items(list, key = { it.lookupKey }) { c ->
                    ContactRow(
                        contact = c,
                        checked = c.lookupKey in selected,
                        onToggle = {
                            if (c.lookupKey in selected) selected.remove(c.lookupKey) else selected.add(c.lookupKey)
                        },
                    )
                }
            }
        }
    }

    if (importing) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(16.dp))
                    Text(stringResource(R.string.importing_contacts))
                }
            },
        )
    }
}

@Composable
private fun ContactRow(contact: ContactBirthday, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(null, contact.contactId) {
        if (contact.hasPhoto) value = withContext(Dispatchers.IO) { ContactsImport.thumbnail(context, contact.contactId) }
    }
    val enabled = !contact.alreadyAdded
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked || contact.alreadyAdded,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.padding(12.dp),
        )
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor(contact.name)),
            contentAlignment = Alignment.Center,
        ) {
            val p = photo
            if (p != null) {
                Image(p, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(initials(contact.name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                if (contact.alreadyAdded) stringResource(R.string.already_in_birthdawn)
                else formatBirthday(contact.day, contact.month, contact.year),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
