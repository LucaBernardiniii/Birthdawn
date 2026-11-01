package dev.luca.compleanni

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import dev.luca.compleanni.data.PeopleRepository
import dev.luca.compleanni.notify.Notifier
import dev.luca.compleanni.notify.ReminderScheduler
import dev.luca.compleanni.ui.App
import dev.luca.compleanni.ui.theme.CompleanniTheme

class MainActivity : ComponentActivity() {

    /** Persona da aprire quando si tocca una notifica. */
    private val personToOpen = mutableStateOf<String?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PeopleRepository.load(this)
        Notifier.ensureChannel(this)
        ReminderScheduler.schedule(this)
        ReminderScheduler.runCheckIfDue(this)

        if (savedInstanceState == null) {
            personToOpen.value = intent?.getStringExtra(Notifier.EXTRA_PERSON_ID)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            CompleanniTheme {
                App(
                    personToOpen = personToOpen.value,
                    onPersonOpened = { personToOpen.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifier.EXTRA_PERSON_ID)?.let { personToOpen.value = it }
    }
}
