package ru.snow.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val weather: Weather? = null,
    val place: Place? = null,
    val loading: Boolean = false,
    val locating: Boolean = false,
    val offline: Boolean = false,
    val message: String? = null,
)

class SnowViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app

    private val _ui = MutableStateFlow(UiState(weather = Prefs.snapshot(app), place = Prefs.place(app)))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _theme = MutableStateFlow(Prefs.theme(app))
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _anim = MutableStateFlow(Prefs.anim(app))
    val anim: StateFlow<Boolean> = _anim.asStateFlow()

    private val _auto = MutableStateFlow(Prefs.auto(app))
    val auto: StateFlow<Boolean> = _auto.asStateFlow()

    private val _results = MutableStateFlow<List<Place>?>(null)
    val results: StateFlow<List<Place>?> = _results.asStateFlow()

    private val _searchMsg = MutableStateFlow<String?>(null)
    val searchMsg: StateFlow<String?> = _searchMsg.asStateFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null

    fun setTheme(v: String) {
        Prefs.setTheme(ctx, v)
        _theme.value = v
        viewModelScope.launch { Repo.updateWidgets(ctx) }
    }

    fun setAnim(v: Boolean) {
        Prefs.setAnim(ctx, v)
        _anim.value = v
    }

    fun setAuto(v: Boolean) {
        Prefs.setAuto(ctx, v)
        _auto.value = v
    }

    fun toast(msg: String) {
        _ui.update { it.copy(message = msg) }
    }

    fun refresh() {
        if (Prefs.place(ctx) == null) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, message = null) }
            val ok = Repo.refresh(ctx)
            _ui.update {
                it.copy(weather = Prefs.snapshot(ctx), place = Prefs.place(ctx), loading = false, offline = !ok)
            }
            Repo.updateWidgets(ctx)
        }
    }

    fun refreshIfStale() {
        if (Prefs.place(ctx) == null) return
        val w = _ui.value.weather
        // Сохранённые данные без прогноза на 7 суток (старая версия) тоже считаем устаревшими.
        if (w == null || w.ahead.size < 100 || System.currentTimeMillis() - w.updatedAt > 10 * 60_000L) refresh()
    }

    fun useLocation() {
        if (_ui.value.locating) return
        viewModelScope.launch {
            _ui.update { it.copy(locating = true, message = null) }
            val l = Loc.locate(ctx)
            if (l == null) {
                _ui.update { it.copy(locating = false, message = "Не удалось определить местоположение") }
                refresh()
                return@launch
            }
            val name = Loc.cityName(ctx, l.latitude, l.longitude) ?: "Моё местоположение"
            val p = Place(name, l.latitude, l.longitude)
            Prefs.setPlace(ctx, p, true)
            _auto.value = true
            _ui.update { it.copy(locating = false, place = p, weather = Prefs.snapshot(ctx)) }
            refresh()
        }
    }

    fun chooseCity(p: Place) {
        Prefs.setPlace(ctx, p, false)
        _auto.value = false
        _ui.update { it.copy(place = Place(p.name, p.lat, p.lon), weather = Prefs.snapshot(ctx), offline = false, message = null) }
        refresh()
    }

    fun search(q: String) {
        searchJob?.cancel()
        val query = q.trim()
        if (query.length < 2) {
            _results.value = null
            _searchMsg.value = null
            return
        }
        searchJob = viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { Api.search(query) }
                _results.value = r
                _searchMsg.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _results.value = null
                _searchMsg.value = "Нет подключения к интернету"
            }
        }
    }
}
