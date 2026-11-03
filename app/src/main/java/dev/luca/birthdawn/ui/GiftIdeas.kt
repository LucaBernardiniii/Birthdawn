package dev.luca.birthdawn.ui

import dev.luca.birthdawn.util.plural
import dev.luca.birthdawn.R
import androidx.compose.ui.res.stringResource
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.luca.birthdawn.data.GiftIdea
import dev.luca.birthdawn.data.PeopleRepository
import dev.luca.birthdawn.data.Person
import java.util.UUID

private val URL_REGEX = Regex("""(https?://\S+|www\.\S+)""", RegexOption.IGNORE_CASE)

/** Trova il primo link in un testo (es. testo condiviso da un'app di shopping). */
fun findUrl(text: String?): String? =
    text?.let { URL_REGEX.find(it)?.value?.trimEnd('.', ',', ')', ']', '"', '\'') }

/** Aggiunge https:// se manca. */
fun normalizeUrl(raw: String): String? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    return if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
}

/** "www.amazon.it/dp/..." -> "amazon.it" */
fun domainOf(url: String): String =
    runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull() ?: url

fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_link), Toast.LENGTH_SHORT).show()
    }
}

/** Card "Idee regalo" nella pagina della persona: si modifica direttamente da qui. */
@Composable
fun GiftIdeasCard(person: Person) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<GiftIdea?>(null) }
    var adding by remember { mutableStateOf(false) }
    val gifts = person.gifts.sortedBy { it.bought } // quelli già presi in fondo

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Row(
                Modifier.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎁", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.gift_ideas), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val todo = person.gifts.count { !it.bought }
                    Text(
                        when {
                            person.gifts.isEmpty() -> stringResource(R.string.gifts_empty)
                            todo == 0 -> stringResource(R.string.gifts_all_bought)
                            else -> plural(context, R.plurals.gifts_todo, todo)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_gift), tint = MaterialTheme.colorScheme.primary)
                }
            }

            gifts.forEach { gift ->
                GiftRow(
                    gift = gift,
                    onToggle = {
                        PeopleRepository.updateGifts(context, person.id) { list ->
                            list.map { if (it.id == gift.id) it.copy(bought = !it.bought) else it }
                        }
                    },
                    onClick = { editing = gift },
                    onOpen = { gift.url?.let { openLink(context, it) } },
                )
            }

            if (person.gifts.isEmpty()) {
                Text(
                    stringResource(R.string.gifts_share_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                )
            }
        }
    }

    if (adding || editing != null) {
        GiftDialog(
            initial = editing,
            onDismiss = { adding = false; editing = null },
            onSave = { title, url ->
                val current = editing
                PeopleRepository.updateGifts(context, person.id) { list ->
                    if (current == null) list + GiftIdea(UUID.randomUUID().toString(), title, url)
                    else list.map { if (it.id == current.id) it.copy(title = title, url = url) else it }
                }
                adding = false; editing = null
            },
            onDelete = editing?.let { g ->
                {
                    PeopleRepository.updateGifts(context, person.id) { list -> list.filterNot { it.id == g.id } }
                    editing = null
                }
            },
        )
    }
}

@Composable
private fun GiftRow(gift: GiftIdea, onToggle: () -> Unit, onClick: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = gift.bought, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                gift.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (gift.bought) TextDecoration.LineThrough else null,
                color = if (gift.bought) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (gift.url != null) {
                Text(
                    domainOf(gift.url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
        if (gift.url != null) {
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.open_link), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun GiftDialog(
    initial: GiftIdea?,
    onDismiss: () -> Unit,
    onSave: (title: String, url: String?) -> Unit,
    onDelete: (() -> Unit)? = null,
    initialTitle: String = "",
    initialUrl: String = "",
) {
    var title by remember { mutableStateOf(initial?.title ?: initialTitle) }
    var url by remember { mutableStateOf(initial?.url ?: initialUrl) }
    val clipboard = LocalClipboardManager.current
    val normalized = normalizeUrl(url)
    // Se manca il titolo ma c'è un link, usa il nome del sito
    val finalTitle = title.trim().ifEmpty { normalized?.let { domainOf(it) } ?: "" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.new_gift else R.string.edit_gift)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.gift_what)) },
                    placeholder = { Text(stringResource(R.string.gift_example)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.gift_link)) },
                    placeholder = { Text("https://…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (url.isEmpty()) {
                            TextButton(onClick = {
                                findUrl(clipboard.getText()?.text)?.let { url = it }
                            }) { Text(stringResource(R.string.paste)) }
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = finalTitle.isNotEmpty(),
                onClick = { onSave(finalTitle, normalized) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.delete))
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Finestra mostrata quando si condivide un link a Birthdawn: scegli la persona. */
@Composable
fun SharedGiftDialog(
    sharedText: String,
    sharedSubject: String?,
    people: List<Person>,
    onDone: (personId: String?) -> Unit,
) {
    val context = LocalContext.current
    var chosen by remember { mutableStateOf<Person?>(null) }
    val url = findUrl(sharedText)
    // Titolo: l'oggetto condiviso, oppure il testo senza il link
    val guessedTitle = (sharedSubject?.takeIf { it.isNotBlank() }
        ?: sharedText.replace(url ?: "", "").trim().lines().firstOrNull { it.isNotBlank() })
        ?.take(120) ?: ""

    val person = chosen
    if (person == null) {
        AlertDialog(
            onDismissRequest = { onDone(null) },
            title = { Text(stringResource(R.string.gift_for)) },
            text = {
                if (people.isEmpty()) {
                    Text(stringResource(R.string.gift_need_person))
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(people.sortedBy { it.name.lowercase() }, key = { it.id }) { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { chosen = p }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Avatar(p, 36.dp)
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onDone(null) }) { Text(stringResource(R.string.cancel)) } },
        )
    } else {
        GiftDialog(
            initial = null,
            initialTitle = guessedTitle,
            initialUrl = url ?: "",
            onDismiss = { chosen = null },
            onSave = { title, link ->
                PeopleRepository.updateGifts(context, person.id) { list ->
                    list + GiftIdea(UUID.randomUUID().toString(), title, link)
                }
                Toast.makeText(context, context.getString(R.string.gift_saved_for, person.name), Toast.LENGTH_SHORT).show()
                onDone(person.id)
            },
        )
    }
}
