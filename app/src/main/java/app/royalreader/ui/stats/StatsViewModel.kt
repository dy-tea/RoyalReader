package app.royalreader.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.royalreader.domain.model.DailyReadingTime
import app.royalreader.domain.repository.NovelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * Reading time per calendar day for the last [DAYS_SHOWN] days.
 **/
data class StatsUiState(
    val daily: List<DailyReadingTime> = emptyList(),
    val rangeStartEpochDay: Long = LocalDate.now().toEpochDay() - (DAYS_SHOWN - 1),
) {
    val totalMs: Long get() = daily.sumOf { it.totalMs }

    companion object {
        const val DAYS_SHOWN = 30L
    }
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    novelRepository: NovelRepository,
) : ViewModel() {

    private val rangeStart = LocalDate.now().toEpochDay() - (StatsUiState.DAYS_SHOWN - 1)

    val uiState: StateFlow<StatsUiState> = novelRepository.observeDailyReadingTime(rangeStart)
        .map { daily -> StatsUiState(daily = daily, rangeStartEpochDay = rangeStart) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatsUiState(),
        )
}
