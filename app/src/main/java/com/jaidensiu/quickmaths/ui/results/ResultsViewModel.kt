package com.jaidensiu.quickmaths.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaidensiu.quickmaths.data.prefs.BestTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ResultsViewModel @Inject constructor(bestTimeRepository: BestTimeRepository) : ViewModel() {
    val bestTimeMs: StateFlow<Long?> = bestTimeRepository.bestTimeMs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = null,
        )
}
