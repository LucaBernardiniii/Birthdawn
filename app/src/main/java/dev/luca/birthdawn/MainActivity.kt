package dev.luca.birthdawn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import dev.luca.birthdawn.data.PeopleRepository
import dev.luca.birthdawn.notify.Notifier
import dev.luca.birthdawn.notify.ReminderScheduler
import dev.luca.birthdawn.ui.App
import dev.luca.birthdawn.util.AppLanguage
import dev.luca.birthdawn.ui.theme.AppearanceSettings
import dev.luca.birthdawn.ui.theme.BirthdawnTheme
import dev.luca.birthdawn.ui.theme.rememberDarkTheme

class MainActivity : ComponentActivity() {

    /** Applica la lingua scelta nelle impostazioni a tutta la schermata. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    /** Persona da aprire quando si tocca una notifica. */
    private val personToOpen = mutableStateOf<String?>(null)

    /** Testo condiviso da un'altra app (link di un articolo), da salvare come idea regalo. */
    private val sharedText = mutableStateOf<Pair<String, String?>?>(null)

    private fun handleShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) sharedText.value = text to intent.getStringExtra(Intent.EXTRA_SUBJECT)
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PeopleRepository.load(this)
        AppearanceSettings.load(this)
        Notifier.ensureChannel(this)
        ReminderScheduler.schedule(this)
        ReminderScheduler.runCheckIfDue(this)

        if (savedInstanceState == null) {
            personToOpen.value = intent?.getStringExtra(Notifier.EXTRA_PERSON_ID)
            handleShare(intent)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val dark = rememberDarkTheme()
            // Icone della barra di stato chiare o scure in base al tema scelto nell'app
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            BirthdawnTheme(dark = dark) {
                App(
                    personToOpen = personToOpen.value,
                    onPersonOpened = { personToOpen.value = null },
                    shared = sharedText.value,
                    onSharedHandled = { sharedText.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifier.EXTRA_PERSON_ID)?.let { personToOpen.value = it }
        handleShare(intent)
    }
}
