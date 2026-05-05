package com.swiftshare.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshare.data.db.entity.TransferRecord
import com.swiftshare.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferHistoryViewModel @Inject constructor(
    private val transferRepository: TransferRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val records: StateFlow<List<TransferRecord>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) transferRepository.observeAll()
            else transferRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch { transferRepository.deleteById(id) }
    }
}
