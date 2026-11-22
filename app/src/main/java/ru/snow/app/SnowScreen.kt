package ru.snow.app

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private val DarkScheme = darkColorScheme(
    background = Color(0xFF101318),
    surface = Color(0xFF1A1F26),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA7B0BC),
    primary = Color(0xFFB8D9F5),
    onPrimary = Color(0xFF101318),
    secondaryContainer = Color(0xFF2B3744),
    onSecondaryContainer = Color(0xFFB8D9F5),
    outline = Color(0xFF3A4350),
    outlineVariant = Color(0xFF2A313B),
    surfaceContainerHighest = Color(0xFF2A313B),
)

private val LightScheme = lightColorScheme(
    background = Color(0xFFF2F4F7),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF101318),
    onSurface = Color(0xFF101318),
    onSurfaceVariant = Color(0xFF5C6673),
    primary = Color(0xFF3A78AD),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFD9E8F5),
    onSecondaryContainer = Color(0xFF101318),
    outline = Color(0xFFB5BEC9),
    outlineVariant = Color(0xFFD5DBE3),
    surfaceContainerHighest = Color(0xFFE6EAF0),
)

@Composable
fun SnowApp(vm: SnowViewModel, onLocate: () -> Unit) {
    val theme by vm.theme.collectAsState()
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val activity = LocalContext.current as ComponentActivity
    DisposableEffect(dark) {
        val bar = if (dark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        onDispose { }
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
        SnowScreen(vm, dark, onLocate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnowScreen(vm: SnowViewModel, dark: Boolean, onLocate: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val animOn by vm.anim.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme
    val w = ui.weather
    val status = w?.status()
    val snowing = status.snowing()

    CompositionLocalProvider(LocalContentColor provides cs.onBackground) {
        Box(Modifier.fillMaxSize().background(cs.background)) {
            if (dark) {
                Box(Modifier.fillMaxWidth().height(520.dp)) {
                    Image(
                        painter = painterResource(if (snowing) R.drawable.bg_sky else R.drawable.bg_mountain),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(if (snowing) 0.7f else 0.4f),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier.fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, cs.background)))
                    )
                }
            }
            if (snowing && animOn) {
                SnowFall(status.level(), if (dark) Color.White else cs.primary, Modifier.fillMaxSize())
            }
            PullToRefreshBox(isRefreshing = ui.loading, onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    TopBar(
                        city = ui.place?.name ?: w?.city ?: "Выберите город",
                        onCity = { showCity = true },
                        onRefresh = { vm.refresh() },
                        onSettings = { showSettings = true },
                    )
                    if (w == null) {
                        EmptyState(
                            hasPlace = ui.place != null,
                            busy = ui.loading || ui.locating,
                            onLocate = onLocate,
                            onCity = { showCity = true },
                            onRetry = { vm.refresh() },
                        )
                    } else {
                        MainContent(w, status, snowing)
                    }
                    Spacer(Modifier.height(16.dp))
                    Footer(w, ui)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            vm = vm,
            onClose = { showSettings = false },
            onCity = { showSettings = false; showCity = true },
            onLocate = onLocate,
        )
    }
    if (showCity) {
        CitySheet(vm = vm, onClose = { showCity = false }, onLocate = onLocate)
    }
}

@Composable
private fun TopBar(city: String, onCity: () -> Unit, onRefresh: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.weight(1f, fill = false)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onCity() }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_pin), null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(city, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(4.dp))
            Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(20.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRefresh) { Icon(painterResource(R.drawable.ic_refresh), "Обновить", Modifier.size(24.dp)) }
            IconButton(onClick = onSettings) { Icon(painterResource(R.drawable.ic_tune), "Настройки", Modifier.size(24.dp)) }
        }
    }
}

@Composable
private fun MainContent(w: Weather, status: SnowStatus?, snowing: Boolean) {
    val cs = MaterialTheme.colorScheme
    Spacer(Modifier.height(20.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(iconRes(w.code, snowing)), null, Modifier.size(80.dp), tint = cs.primary)
        Spacer(Modifier.height(8.dp))
        val t = fmtTemp(w.temp)
        if (t != null) {
            Text(t, fontSize = 96.sp, fontWeight = FontWeight.Light)
        } else {
            Text("Нет данных", fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
        Text(status?.title ?: "Нет данных", fontSize = 24.sp, fontWeight = FontWeight.Medium)
        val d = w.detail()
        if (d != null) {
            Spacer(Modifier.height(4.dp))
            Text(d, fontSize = 15.sp, color = cs.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(24.dp))
    Block {
        Text(w.nextSnowTitle(), fontSize = 14.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(w.nextSnowBody().cap(), fontSize = 20.sp, fontWeight = FontWeight.Medium)
        val amount = w.nextSnowAmount()
        if (amount != null) {
            Spacer(Modifier.height(4.dp))
            Text(amount, fontSize = 14.sp, color = cs.onSurfaceVariant)
        }
        val typical = w.typicalLine()
        if (typical != null) {
            Spacer(Modifier.height(8.dp))
            Text(typical, fontSize = 14.sp, color = cs.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(12.dp))
    Block {
        Row(Modifier.fillMaxWidth()) {
            Stat("Ощущается", fmtTemp(w.feels) ?: "Нет данных", Modifier.weight(1f))
            Stat("Ветер", fmtWind(w.wind), Modifier.weight(1f))
            Stat("Влажность", fmtHumidity(w.humidity), Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(12.dp))
    Block {
        Text("Почасовой прогноз", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(14.dp))
        if (w.hours.isEmpty()) {
            Text("Нет данных", color = cs.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(w.hours) { i, h ->
                    Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(w.hourLabel(h, i), fontSize = 13.sp, color = cs.onSurfaceVariant, maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                        Icon(
                            painterResource(iconRes(h.code, h.snowy, flake = true)),
                            null,
                            Modifier.size(26.dp),
                            tint = if (h.snowy) cs.primary else cs.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(fmtTemp(h.temp) ?: "—", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    DayList("Прогноз на 7 дней", w.forecastDays().take(7))
    Spacer(Modifier.height(12.dp))
    DayList("История за 7 дней", w.days())
}

@Composable
private fun DayList(title: String, days: List<DaySummary>) {
    val cs = MaterialTheme.colorScheme
    Block {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        if (days.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Нет данных", color = cs.onSurfaceVariant)
        }
        days.forEach { d ->
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(d.label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    d.snow,
                    fontSize = 14.sp,
                    color = if (d.snow == "Без снега") cs.onSurfaceVariant else cs.primary,
                )
            }
            val sub = listOfNotNull(
                if (d.min != null && d.max != null) "${fmtTemp(d.min)} … ${fmtTemp(d.max)}" else null,
                d.periods,
            ).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun Block(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        content = content,
    )
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyState(hasPlace: Boolean, busy: Boolean, onLocate: () -> Unit, onCity: () -> Unit, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Spacer(Modifier.height(80.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(R.drawable.ic_snow), null, Modifier.size(72.dp), tint = cs.primary)
        Spacer(Modifier.height(20.dp))
        Text(if (busy) "Загрузка…" else "Нет данных", fontSize = 28.sp, fontWeight = FontWeight.Light)
        if (!busy) {
            Spacer(Modifier.height(24.dp))
            if (hasPlace) {
                Button(onClick = onRetry) { Text("Повторить") }
            } else {
                Button(onClick = onLocate) { Text("Определить местоположение") }
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.TextButton(onClick = onCity) { Text("Выбрать город") }
            }
        }
    }
}

@Composable
private fun Footer(w: Weather?, ui: UiState) {
    val sec = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (w != null) Text("Обновлено в ${fmtLocalTime(w.updatedAt)}", fontSize = 13.sp, color = sec)
        if (ui.offline) Text("Нет сети — показаны сохранённые данные", fontSize = 13.sp, color = sec)
        if (ui.message != null) Text(ui.message, fontSize = 13.sp, color = sec)
    }
}

private class Flake(var x: Float, var y: Float, val r: Float, val v: Float, val drift: Float, val a: Float, val phase: Float)

@Composable
private fun SnowFall(level: Int, color: Color, modifier: Modifier) {
    val count = when (level) {
        3 -> 90
        2 -> 55
        else -> 30
    }
    val speed = when (level) {
        3 -> 1.5f
        2 -> 1.15f
        else -> 0.9f
    }
    val flakes = remember(count) {
        List(count) {
            Flake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = 1f + Random.nextFloat() * 2.2f,
                v = 0.05f + Random.nextFloat() * 0.07f,
                drift = 0.006f + Random.nextFloat() * 0.018f,
                a = 0.25f + Random.nextFloat() * 0.5f,
                phase = Random.nextFloat() * 6.28f,
            )
        }
    }
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(count, speed) {
        var prev = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { t ->
                if (prev != 0L) {
                    val dt = (t - prev) / 1_000_000_000f
                    for (f in flakes) {
                        f.y += f.v * dt * speed
                        if (f.y > 1.05f) {
                            f.y = -0.05f
                            f.x = Random.nextFloat()
                        }
                    }
                }
                prev = t
                tick = t
            }
        }
    }
    Canvas(modifier) {
        val t = tick
        if (t >= 0L) {
            for (f in flakes) {
                val x = (f.x + sin(f.phase + f.y * 6f) * f.drift) * size.width
                drawCircle(color.copy(alpha = f.a), f.r.dp.toPx(), Offset(x, f.y * size.height))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(vm: SnowViewModel, onClose: () -> Unit, onCity: () -> Unit, onLocate: () -> Unit) {
    val theme by vm.theme.collectAsState()
    val anim by vm.anim.collectAsState()
    val auto by vm.auto.collectAsState()
    val ui by vm.ui.collectAsState()
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surface,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            Text("Тема", fontSize = 14.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            val items = listOf("system" to "Системная", "light" to "Светлая", "dark" to "Тёмная")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                items.forEachIndexed { i, (k, label) ->
                    SegmentedButton(
                        selected = theme == k,
                        onClick = { vm.setTheme(k) },
                        shape = SegmentedButtonDefaults.itemShape(i, items.size),
                    ) { Text(label, fontSize = 13.sp, maxLines = 1) }
                }
            }
            Spacer(Modifier.height(12.dp))
            SwitchRow("Анимация снега", anim) { vm.setAnim(it) }
            SwitchRow("Определять местоположение", auto) {
                vm.setAuto(it)
                if (it) onLocate()
            }
            Row(
                Modifier.fillMaxWidth().clickable { onCity() }.padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Город", fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(ui.place?.name ?: "Не выбран", fontSize = 16.sp, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CitySheet(vm: SnowViewModel, onClose: () -> Unit, onLocate: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val results by vm.results.collectAsState()
    val msg by vm.searchMsg.collectAsState()
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(q) {
        delay(400)
        vm.search(q)
    }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = cs.surface,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text("Выбор города", fontSize = 22.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                placeholder = { Text("Название города") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().clickable { onLocate(); onClose() }.padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_pin), null, Modifier.size(20.dp), tint = cs.primary)
                Spacer(Modifier.width(12.dp))
                Text("Определить автоматически", fontSize = 16.sp, color = cs.primary)
            }
            val r = results
            val m = msg
            when {
                m != null -> Text(m, color = cs.onSurfaceVariant)
                r == null -> Unit
                r.isEmpty() -> Text("Ничего не найдено", color = cs.onSurfaceVariant)
                else -> r.forEach { p ->
                    Column(
                        Modifier.fillMaxWidth().clickable { vm.chooseCity(p); onClose() }.padding(vertical = 10.dp)
                    ) {
                        Text(p.name, fontSize = 16.sp)
                        if (p.sub.isNotBlank()) Text(p.sub, fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
