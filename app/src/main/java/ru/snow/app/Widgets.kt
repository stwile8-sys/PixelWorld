package ru.snow.app

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Палитра виджетов: следует теме приложения; в режиме «Системная» — системной теме. */
private class Pal(c: Context) {
    private val mode = Prefs.theme(c)

    private fun p(day: Long, night: Long) = when (mode) {
        "light" -> ColorProvider(Color(day), Color(day))
        "dark" -> ColorProvider(Color(night), Color(night))
        else -> ColorProvider(Color(day), Color(night))
    }

    val bg = p(0xFFF5F7FA, 0xFF1A1F26)
    val text = p(0xFF101318, 0xFFFFFFFF)
    val sec = p(0xFF5C6673, 0xFFA7B0BC)
    val accent = p(0xFF3A78AD, 0xFFB8D9F5)
    val line = p(0xFFD5DBE3, 0xFF2A313B)
}

@Composable
private fun Ico(res: Int, dp: Int, tint: androidx.glance.unit.ColorProvider) {
    Image(
        provider = ImageProvider(res),
        contentDescription = null,
        modifier = GlanceModifier.size(dp.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

private fun open(c: Context) = actionStartActivity(Intent(c, MainActivity::class.java))

@Composable
private fun SmallContent(c: Context, w: Weather?, city: String?) {
    val p = Pal(c)
    val st = w?.status()
    Column(
        GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(24.dp).padding(14.dp).clickable(open(c))
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(city ?: "Снег", GlanceModifier.defaultWeight(), style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
            if (w != null) Ico(iconRes(w.code, st.snowing()), 22, p.accent)
        }
        Spacer(GlanceModifier.defaultWeight())
        val t = fmtTemp(w?.temp)
        if (t != null) {
            Text(t, style = TextStyle(color = p.text, fontSize = 38.sp, fontWeight = FontWeight.Normal))
            Text(st?.title ?: "Нет данных", style = TextStyle(color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 2)
        } else {
            Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
private fun MediumContent(c: Context, w: Weather?, city: String?) {
    val p = Pal(c)
    val st = w?.status()
    Row(
        GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(24.dp).padding(16.dp).clickable(open(c))
    ) {
        Column(GlanceModifier.defaultWeight().fillMaxSize()) {
            Text(city ?: "Снег", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
            Spacer(GlanceModifier.defaultWeight())
            val t = fmtTemp(w?.temp)
            if (t != null) {
                Text(t, style = TextStyle(color = p.text, fontSize = 42.sp, fontWeight = FontWeight.Normal))
                Text(st?.title ?: "Нет данных", style = TextStyle(color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            } else {
                Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
            }
        }
        if (w != null) {
            Column(GlanceModifier.fillMaxHeight(), horizontalAlignment = Alignment.Horizontal.End) {
                Ico(iconRes(w.code, st.snowing()), 30, p.accent)
                Spacer(GlanceModifier.defaultWeight())
                Text("Ощущается ${fmtTemp(w.feels) ?: "Нет данных"}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                Spacer(GlanceModifier.height(2.dp))
                Text("Ветер ${fmtWind(w.wind)}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun LargeContent(c: Context, w: Weather?, city: String?) {
    val p = Pal(c)
    val st = w?.status()
    Column(
        GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(24.dp).padding(16.dp).clickable(open(c))
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.Top) {
            Column(GlanceModifier.defaultWeight()) {
                Text(city ?: "Снег", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                Spacer(GlanceModifier.height(6.dp))
                val t = fmtTemp(w?.temp)
                if (t != null) {
                    Text(t, style = TextStyle(color = p.text, fontSize = 42.sp, fontWeight = FontWeight.Normal))
                    Text(st?.title ?: "Нет данных", style = TextStyle(color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                } else {
                    Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
                }
            }
            if (w != null) {
                Column(horizontalAlignment = Alignment.Horizontal.End) {
                    Ico(iconRes(w.code, st.snowing()), 30, p.accent)
                    Spacer(GlanceModifier.height(10.dp))
                    Text("Ощущается ${fmtTemp(w.feels) ?: "Нет данных"}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                    Spacer(GlanceModifier.height(2.dp))
                    Text("Ветер ${fmtWind(w.wind)}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                }
            }
        }
        if (w != null) {
            Spacer(GlanceModifier.height(14.dp))
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(p.line)) {}
            Spacer(GlanceModifier.height(12.dp))
            Text("Почасовой прогноз", style = TextStyle(color = p.sec, fontSize = 13.sp))
            Spacer(GlanceModifier.height(10.dp))
            if (w.hours.isEmpty()) {
                Text("Нет данных", style = TextStyle(color = p.text, fontSize = 14.sp))
            } else {
                Row(GlanceModifier.fillMaxWidth()) {
                    w.hours.take(6).forEachIndexed { i, h ->
                        Column(GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Text(w.hourLabel(h, i), style = TextStyle(color = p.sec, fontSize = 11.sp), maxLines = 1)
                            Spacer(GlanceModifier.height(8.dp))
                            Ico(iconRes(h.code, h.snowy, flake = true), 22, if (h.snowy) p.accent else p.sec)
                            Spacer(GlanceModifier.height(8.dp))
                            Text(fmtTemp(h.temp) ?: "—", style = TextStyle(color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                        }
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text("Обновлено в ${fmtLocalTime(w.updatedAt)}", style = TextStyle(color = p.sec, fontSize = 11.sp))
        }
    }
}

class SmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val w = Prefs.snapshot(context)
        val city = Prefs.place(context)?.name ?: w?.city
        provideContent { SmallContent(context, w, city) }
    }
}

class MediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val w = Prefs.snapshot(context)
        val city = Prefs.place(context)?.name ?: w?.city
        provideContent { MediumContent(context, w, city) }
    }
}

class LargeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val w = Prefs.snapshot(context)
        val city = Prefs.place(context)?.name ?: w?.city
        provideContent { LargeContent(context, w, city) }
    }
}

abstract class BaseReceiver : GlanceAppWidgetReceiver() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        RefreshWorker.schedule(context, force = true)
        RefreshWorker.runNow(context)
    }
}

class SmallReceiver : BaseReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallWidget()
}

class MediumReceiver : BaseReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumWidget()
}

class LargeReceiver : BaseReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeWidget()
}
