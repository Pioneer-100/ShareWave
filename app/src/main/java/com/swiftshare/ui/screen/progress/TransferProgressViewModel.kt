package com.swiftshare.ui.screen.progress

import androidx.lifecycle.ViewModel
import com.swiftshare.domain.model.TransferProgress
import com.swiftshare.domain.model.TransferSessionState
import com.swiftshare.network.transfer.TransferEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TransferProgressViewModel @Inject constructor(
    private val transferEngine: TransferEngine,
) : ViewModel() {

    val progress: StateFlow<TransferProgress> = transferEngine.transferProgress
    val sessionState: StateFlow<TransferSessionState> = transferEngine.sessionState

    fun cancelTransfer() {
        transferEngine.cancelTransfer()
    }
}
