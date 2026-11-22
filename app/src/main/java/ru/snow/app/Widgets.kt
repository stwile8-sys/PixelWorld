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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import kotlinx.coroutines.withTimeoutOrNull

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

private val KEY = ActionParameters.Key<String>("w")

private fun open(c: Context) = actionStartActivity(Intent(c, MainActivity::class.java))
private fun flip(k: String) = actionRunCallback<PageAction>(actionParametersOf(KEY to k))
private fun refresh() = actionRunCallback<RefreshAction>()

@Composable
private fun Ico(res: Int, dp: Int, tint: androidx.glance.unit.ColorProvider, mod: GlanceModifier = GlanceModifier) {
    Image(
        provider = ImageProvider(res),
        contentDescription = null,
        modifier = mod.size(dp.dp),
        colorFilter = ColorFilter.tint(tint),
    )
}

/** Кнопка обновления: во время загрузки подсвечивается акцентом. */
@Composable
private fun RefreshBtn(p: Pal, busy: Boolean) {
    Ico(R.drawable.ic_refresh, 22, if (busy) p.accent else p.sec, GlanceModifier.clickable(refresh()))
}

@Composable
private fun SmallContent(c: Context, w: Weather?, city: String?) {
    val p = Pal(c)
    val st = w?.status()
    val page = Prefs.page(c, "s") % 2
    Column(
        GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(24.dp).padding(14.dp).clickable(flip("s"))
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                city ?: "Снег",
                GlanceModifier.defaultWeight().clickable(open(c)),
                style = TextStyle(color = p.sec, fontSize = 13.sp),
                maxLines = 1,
            )
            if (w != null && page == 0) Ico(iconRes(w.code, st.snowing()), 22, p.accent)
        }
        Spacer(GlanceModifier.defaultWeight())
        if (w == null) {
            Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
        } else if (page == 0) {
            val t = fmtTemp(w.temp)
            if (t != null) {
                Text(t, style = TextStyle(color = p.text, fontSize = 38.sp, fontWeight = FontWeight.Normal))
                Text(st?.title ?: "Нет данных", style = TextStyle(color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Spacer(GlanceModifier.height(2.dp))
                Text(w.nextSnowShort(), style = TextStyle(color = p.accent, fontSize = 12.sp), maxLines = 2)
            } else {
                Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
            }
        } else {
            Text(w.nextSnowTitle(), style = TextStyle(color = p.sec, fontSize = 12.sp), maxLines = 1)
            Spacer(GlanceModifier.height(4.dp))
            Text(w.nextSnowBody().cap(), style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium), maxLines = 4)
        }
    }
}

@Composable
private fun MediumContent(c: Context, w: Weather?, city: String?) {
    val p = Pal(c)
    val st = w?.status()
    val page = Prefs.page(c, "m") % 2
    val busy = Prefs.busy(c)
    Row(
        GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(24.dp).padding(16.dp).clickable(flip("m"))
    ) {
        Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
            Text(city ?: "Снег", GlanceModifier.clickable(open(c)), style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
            Spacer(GlanceModifier.defaultWeight())
            if (w == null) {
                Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
            } else if (page == 0) {
                val t = fmtTemp(w.temp)
                if (t != null) {
                    Text(t, style = TextStyle(color = p.text, fontSize = 36.sp, fontWeight = FontWeight.Normal))
                    Text(st?.title ?: "Нет данных", style = TextStyle(color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                    Text(w.nextSnowShort(), style = TextStyle(color = p.accent, fontSize = 12.sp), maxLines = 1)
                } else {
                    Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
                }
            } else {
                Text(w.nextSnowTitle(), style = TextStyle(color = p.sec, fontSize = 12.sp), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                Text(w.nextSnowBody().cap(), style = TextStyle(color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Medium), maxLines = 3)
            }
        }
        Column(GlanceModifier.fillMaxHeight(), horizontalAlignment = Alignment.Horizontal.End) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                if (w != null && page == 0) {
                    Ico(iconRes(w.code, st.snowing()), 30, p.accent)
                    Spacer(GlanceModifier.size(8.dp))
                }
                RefreshBtn(p, busy)
            }
            Spacer(GlanceModifier.defaultWeight())
            if (busy) {
                Text("Обновление…", style = TextStyle(color = p.accent, fontSize = 13.sp), maxLines = 1)
            } else if (w != null) {
                if (page == 0) {
                    Text("Ощущается ${fmtTemp(w.feels) ?: "Нет данных"}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                    Spacer(GlanceModifier.height(2.dp))
                    Text("Ветер ${fmtWind(w.wind)}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                } else {
                    val a = w.nextSnowAmount()
                    if (a != null) Text(a, style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                    Text("Обновлено в ${fmtLocalTime(w.updatedAt)}", style = TextStyle(color = p.sec, fontSize = 11.sp), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun LargeContent(c: Context, w: Weather?, city: String?) {
    val p = Pal(c)
    val st = w?.status()
    val page = Prefs.page(c, "l") % 2
    val busy = Prefs.busy(c)
    Column(
        GlanceModifier.fillMaxSize().background(p.bg).cornerRadius(24.dp).padding(16.dp).clickable(flip("l"))
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.Top) {
            Column(GlanceModifier.defaultWeight()) {
                Text(city ?: "Снег", GlanceModifier.clickable(open(c)), style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                Spacer(GlanceModifier.height(4.dp))
                val t = fmtTemp(w?.temp)
                if (t != null) {
                    Text(t, style = TextStyle(color = p.text, fontSize = 38.sp, fontWeight = FontWeight.Normal))
                    Text(st?.title ?: "Нет данных", style = TextStyle(color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                } else {
                    Text("Нет данных", style = TextStyle(color = p.text, fontSize = 16.sp, fontWeight = FontWeight.Medium))
                }
            }
            Column(horizontalAlignment = Alignment.Horizontal.End) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    if (w != null) {
                        Ico(iconRes(w.code, st.snowing()), 30, p.accent)
                        Spacer(GlanceModifier.size(8.dp))
                    }
                    RefreshBtn(p, busy)
                }
                if (w != null) {
                    Spacer(GlanceModifier.height(8.dp))
                    Text("Ощущается ${fmtTemp(w.feels) ?: "Нет данных"}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                    Spacer(GlanceModifier.height(2.dp))
                    Text("Ветер ${fmtWind(w.wind)}", style = TextStyle(color = p.sec, fontSize = 13.sp), maxLines = 1)
                }
            }
        }
        if (w != null) {
            Spacer(GlanceModifier.height(10.dp))
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(p.line)) {}
            Spacer(GlanceModifier.height(10.dp))
            if (page == 0) {
                Text(
                    "${w.nextSnowTitle()}: ${w.nextSnowBody()}",
                    style = TextStyle(color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 2,
                )
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
            } else {
                Text(w.nextSnowTitle(), style = TextStyle(color = p.sec, fontSize = 12.sp))
                Spacer(GlanceModifier.height(2.dp))
                Text(w.nextSnowBody().cap(), style = TextStyle(color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Medium), maxLines = 2)
                val a = w.nextSnowAmount()
                if (a != null) Text(a, style = TextStyle(color = p.sec, fontSize = 12.sp))
                Spacer(GlanceModifier.height(10.dp))
                w.forecastDays().take(4).forEach { d ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(
                            d.label.substringBefore(","),
                            GlanceModifier.defaultWeight(),
                            style = TextStyle(color = p.text, fontSize = 13.sp),
                            maxLines = 1,
                        )
                        Text(
                            d.snow,
                            style = TextStyle(color = if (d.snow == "Без снега") p.sec else p.accent, fontSize = 13.sp),
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                if (busy) "Обновление…" else "Обновлено в ${fmtLocalTime(w.updatedAt)}",
                style = TextStyle(color = if (busy) p.accent else p.sec, fontSize = 11.sp),
            )
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

/** Нажатие на виджет: переключение страницы (сейчас ↔ ближайший снег / прогноз). */
class PageAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val k = parameters[KEY] ?: return
        Prefs.setPage(context, k, (Prefs.page(context, k) + 1) % 2)
        when (k) {
            "s" -> SmallWidget().updateAll(context)
            "m" -> MediumWidget().updateAll(context)
            else -> LargeWidget().updateAll(context)
        }
    }
}

/** Кнопка обновления на виджете. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Prefs.setBusy(context, true)
        Repo.updateWidgets(context)
        try {
            withTimeoutOrNull(8000L) { Repo.refresh(context) }
        } finally {
            Prefs.setBusy(context, false)
            Repo.updateWidgets(context)
        }
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
