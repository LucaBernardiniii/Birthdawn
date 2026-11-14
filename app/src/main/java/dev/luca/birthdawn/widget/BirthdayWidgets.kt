package dev.luca.birthdawn.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import dev.luca.birthdawn.MainActivity
import dev.luca.birthdawn.R
import dev.luca.birthdawn.data.PeopleRepository
import dev.luca.birthdawn.data.Person
import dev.luca.birthdawn.notify.Notifier
import dev.luca.birthdawn.ui.avatarColor
import dev.luca.birthdawn.ui.initials
import dev.luca.birthdawn.ui.visibleRect
import dev.luca.birthdawn.util.AppLanguage
import dev.luca.birthdawn.util.Photos
import dev.luca.birthdawn.util.ageOn
import dev.luca.birthdawn.util.countdownLabel
import dev.luca.birthdawn.util.daysUntil
import dev.luca.birthdawn.util.formatDayMonth
import dev.luca.birthdawn.util.formatLongDate
import dev.luca.birthdawn.util.nextBirthday
import dev.luca.birthdawn.util.turnsLabel
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class WidgetStyle { DAWN, YOU }

/** Widget "Prossimi compleanni" con il cielo dell'alba. */
class DawnWidgetProvider : BirthdayWidgetProvider(WidgetStyle.DAWN)

/** Widget "Prossimi compleanni" con i colori Material You dello sfondo. */
class YouWidgetProvider : BirthdayWidgetProvider(WidgetStyle.YOU)

abstract class BirthdayWidgetProvider(private val style: WidgetStyle) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { BirthdayWidgets.update(context, manager, it, style) }
        BirthdayWidgets.scheduleMidnight(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        BirthdayWidgets.update(context, manager, id, style)
    }

    override fun onEnabled(context: Context) = BirthdayWidgets.scheduleMidnight(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BirthdayWidgets.ACTION_REFRESH) {
            BirthdayWidgets.updateAllNow(context)
            BirthdayWidgets.scheduleMidnight(context)
        }
    }
}

/**
 * Disegna i widget: una fila di card come il carosello della Home,
 * con la prima persona più grande. Il numero di card dipende dalla larghezza del widget.
 */
object BirthdayWidgets {
    const val ACTION_REFRESH = "dev.luca.birthdawn.widget.REFRESH"

    private val executor = Executors.newSingleThreadExecutor()

    private val providers = listOf(
        DawnWidgetProvider::class.java to WidgetStyle.DAWN,
        YouWidgetProvider::class.java to WidgetStyle.YOU,
    )

    /** Aggiorna tutti i widget in background (dopo ogni modifica alle persone o alla lingua). */
    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        executor.execute { runCatching { updateAllNow(app) } }
    }

    fun updateAllNow(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        providers.forEach { (cls, style) ->
            manager.getAppWidgetIds(ComponentName(context, cls)).forEach { update(context, manager, it, style) }
        }
    }

    /** Aggiorna i conti alla rovescia poco dopo mezzanotte (allarme non esatto: non consuma batteria). */
    fun scheduleMidnight(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val next = LocalDate.now().plusDays(1).atTime(0, 1)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, DawnWidgetProvider::class.java).setAction(ACTION_REFRESH)
        val pending = PendingIntent.getBroadcast(
            context, 2001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.set(AlarmManager.RTC, next, pending)
    }

    fun update(context: Context, manager: AppWidgetManager, widgetId: Int, style: WidgetStyle) {
        val ctx = AppLanguage.wrap(context)
        val today = LocalDate.now()
        val density = context.resources.displayMetrics.density

        // Dimensioni del widget (in verticale: larghezza minima, altezza massima)
        val opts = manager.getAppWidgetOptions(widgetId)
        val widthDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 250
        val heightDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 } ?: 110

        val maxCards = when {
            widthDp < 170 -> 1
            widthDp < 260 -> 2
            widthDp < 360 -> 3
            else -> 4
        }
        val upcoming = PeopleRepository.load(context)
            .sortedWith(compareBy<Person> { it.daysUntil(today) }.thenBy { it.name.lowercase() })
            .take(maxCards)

        val layout = if (style == WidgetStyle.DAWN) R.layout.widget_birthdays_dawn else R.layout.widget_birthdays_you
        val views = RemoteViews(context.packageName, layout)
        views.setTextViewText(R.id.widget_today, formatDayMonth(today.dayOfMonth, today.monthValue))
        views.setOnClickPendingIntent(android.R.id.background, openApp(context, null, widgetId * 10))
        views.removeAllViews(R.id.widget_cards)
        views.setViewVisibility(R.id.widget_empty, if (upcoming.isEmpty()) View.VISIBLE else View.GONE)

        if (upcoming.isNotEmpty()) {
            // Larghezza delle card: la prima "pesa" 3, le altre 2 (come i layout XML)
            val n = upcoming.size
            val padding = 12f
            val gap = 8f
            val usableW = widthDp - padding * 2 - gap * (n - 1)
            val units = 3f + 2f * (n - 1)
            val cardH = (heightDp - padding * 2 - 28f - 8f).coerceAtLeast(56f)

            upcoming.forEachIndexed { i, person ->
                val large = i == 0
                val cardW = usableW * (if (large) 3f else 2f) / units
                val card = RemoteViews(
                    context.packageName,
                    if (large) R.layout.widget_card_large else R.layout.widget_card,
                )
                val days = person.daysUntil(today)
                val next = person.nextBirthday(today)

                card.setImageViewBitmap(
                    R.id.card_image,
                    cardBitmap(context, person, cardW * density, cardH * density, 18f * density),
                )
                card.setTextViewText(R.id.card_name, person.name)
                val age = person.ageOn(next)
                val date = if (days == 0) formatLongDate(next) else formatDayMonth(person.day, person.month)
                card.setTextViewText(
                    R.id.card_date,
                    if (large && age != null) "$date · ${turnsLabel(ctx, age)}" else date,
                )
                val chip = if (style == WidgetStyle.DAWN) R.id.card_chip_dawn else R.id.card_chip_you
                card.setViewVisibility(chip, View.VISIBLE)
                card.setTextViewText(chip, (if (days == 0) "🎂 " else "") + countdownLabel(ctx, days))
                card.setOnClickPendingIntent(R.id.card_root, openApp(context, person.id, widgetId * 10 + i + 1))
                card.setContentDescription(R.id.card_root, "${person.name}, ${countdownLabel(ctx, days)}")
                views.addView(R.id.widget_cards, card)
            }
        }

        manager.updateAppWidget(widgetId, views)
    }

    private fun openApp(context: Context, personId: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (personId != null) {
                putExtra(Notifier.EXTRA_PERSON_ID, personId)
                // Rende ogni intent diverso, così ogni card apre la sua persona
                data = Uri.parse("birthdawn://person/$personId")
            }
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Immagine della card: foto (con l'inquadratura scelta) o iniziali, angoli arrotondati e sfumatura scura in basso. */
    private fun cardBitmap(context: Context, person: Person, wPx: Float, hPx: Float, radius: Float): Bitmap {
        // Limita la dimensione: i widget hanno un tetto di memoria per le immagini
        val scale = min(1f, 420f / max(wPx, hPx))
        val w = (wPx * scale).roundToInt().coerceAtLeast(32)
        val h = (hPx * scale).roundToInt().coerceAtLeast(32)
        val r = radius * scale

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val clip = Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, Path.Direction.CW) }
        canvas.clipPath(clip)

        val photo = person.photoPath?.let {
            runCatching { Photos.load(context, it, max(w, h) * 2)?.asAndroidBitmap() }.getOrNull()
        }
        if (photo != null) {
            val src = visibleRect(
                photo.width.toFloat(), photo.height.toFloat(), w.toFloat(), h.toFloat(), person.photoFocus,
            )
            canvas.drawBitmap(
                photo,
                Rect(src.left.roundToInt(), src.top.roundToInt(), src.right.roundToInt(), src.bottom.roundToInt()),
                Rect(0, 0, w, h),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG),
            )
        } else {
            val base = avatarColor(person.name).toArgb()
            val dark = android.graphics.Color.rgb(
                (android.graphics.Color.red(base) * 0.45f).roundToInt(),
                (android.graphics.Color.green(base) * 0.45f).roundToInt(),
                (android.graphics.Color.blue(base) * 0.45f).roundToInt(),
            )
            canvas.drawPaint(Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, h.toFloat(), base, dark, Shader.TileMode.CLAMP)
            })
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xE6FFFFFF.toInt()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                textSize = min(w, h) * 0.34f
            }
            val y = h * 0.44f - (text.descent() + text.ascent()) / 2f
            canvas.drawText(initials(person.name), w / 2f, y, text)
        }

        // Sfumatura in basso per leggere nome e data
        canvas.drawRect(0f, h * 0.4f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, h * 0.4f, 0f, h.toFloat(),
                0x00000000, 0xB3000000.toInt(), Shader.TileMode.CLAMP,
            )
        })
        return bmp
    }
}
