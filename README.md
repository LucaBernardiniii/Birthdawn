# 🎂 Birthdawn

App Android per ricordarsi dei compleanni. Kotlin + Jetpack Compose (Material 3), nessuna libreria esterna oltre ad AndroidX.

## Funzioni

- **Home**: carosello Material 3 con i prossimi compleanni (foto a tutta card), poi l'elenco completo per mese con ricerca, ordinamento (in arrivo, A–Z, da gennaio a dicembre) e card speciale per chi festeggia oggi.
- **Barra in alto**: nome dell'app, data di oggi, numero di persone e pulsante impostazioni.
- **Persona**: pagina di visualizzazione con banner elastico (tirando giù si allunga e mostra la foto intera), foto, conto alla rovescia, età, note, notifiche) con pulsante Modifica; in modifica, banner per aggiungere/cambiare foto e scegliere la parte visibile (trascina e pizzica), nome, data (anche senza anno), note e notifiche personalizzate.
- **Idee regalo**: per ogni persona, lista di idee con link opzionale (si apre nel browser) e spunta "preso". Da browser o app di shopping: *Condividi → Birthdawn* per salvare un articolo al volo. Le notifiche di promemoria ricordano quante idee hai salvato.
- **Il tuo compleanno**: impostabile nelle Impostazioni; il giorno stesso ricevi una notifica di auguri e un messaggio in Home.
- **Calendario**: griglia mensile (swipe o frecce per cambiare mese), giorni con compleanni evidenziati, elenco del giorno e del mese.
- **Notifiche**: di default 7 giorni prima e il giorno stesso. Per ogni persona puoi scegliere 0/1/3/7/14/30 giorni prima o un numero personalizzato (0–365).
- **Impostazioni (⚙️)**: tema chiaro/scuro/dispositivo, barra di navigazione che si rimpicciolisce scorrendo (o fissa), orario delle notifiche, notifica di prova, aspetto (Alba: sfondo sfumato dall'azzurro all'arancione, predefinito; sfondo del telefono/Material You; colore personalizzato) e "Colore dalla foto" per la pagina di ogni persona.

Le notifiche sopravvivono al riavvio del telefono; se il telefono era spento all'orario previsto, l'avviso arriva appena si riaccende o si apre l'app.

## Ottenere l'APK con GitHub (consigliato)

1. Crea un repository (anche privato) e carica questa cartella. Il `.gitignore` esclude già keystore e password.
2. Su GitHub: **Settings → Secrets and variables → Actions** e aggiungi i 4 secret presenti in `FIRMA-SEGRETI.txt`
   (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
3. Ogni push su `main` compila l'app: scarica `Birthdawn-apk` da **Actions → ultima esecuzione → Artifacts**.
4. Per una release pubblica con l'APK allegato:
   ```
   git tag v1.0 && git push --tags
   ```

Senza i secret l'APK viene comunque compilato, ma firmato con una chiave di debug.

> ⚠️ Conserva `birthdawn-release.jks` e `FIRMA-SEGRETI.txt` in un posto sicuro (non su GitHub).
> Gli aggiornamenti dell'app devono essere firmati con la stessa chiave, altrimenti Android non li installa sopra la versione esistente.

## Compilare in locale

Apri la cartella con **Android Studio** (Ladybug o più recente) e premi ▶️ oppure:

```
./gradlew assembleRelease
```

L'APK firmato con la tua chiave sarà in `app/build/outputs/apk/release/`.

## Installare sul telefono

Copia l'APK sul telefono e aprilo; Android chiederà di consentire l'installazione da quella app (file manager / browser). Al primo avvio concedi il permesso per le notifiche.

## Struttura

```
app/src/main/java/dev/luca/birthdawn/
├── MainActivity.kt
├── data/      Person, PeopleRepository (salvataggio JSON locale)
├── notify/    Notifier, ReminderScheduler (+ receiver allarme/riavvio), Prefs
├── ui/        App (navigazione), HomeScreen, CalendarScreen, PersonScreen, Components, theme/
└── util/      Birthdays (calcolo date/età), Photos (import e caricamento foto)
```

Requisiti: Android 8.0 (API 26) o superiore.
