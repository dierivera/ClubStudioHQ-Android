package club.studio.clubstudiohq.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.studio.clubstudiohq.data.GameEvent
import club.studio.clubstudiohq.repository.FirestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LiveGameState(
    val ourGoals: Int = 0,
    val oppositionGoals: Int = 0,
    val events: List<GameEvent> = emptyList(),
    val isGameActive: Boolean = true
)

class LiveGameViewModel(private val repository: FirestoreRepository) : ViewModel() {
    private val _gameState = MutableStateFlow(LiveGameState())
    val gameState: StateFlow<LiveGameState> = _gameState

    private var currentGameId: String? = null

    fun startObserving(gameId: String) {
        currentGameId = gameId
        viewModelScope.launch {
            repository.observeGameEvents(gameId).collect { events ->
                // Recalcular marcador a partir de los eventos
                var ourG = 0
                var oppG = 0
                for (event in events) {
                    when (event.type) {
                        "goal" -> ourG++
                        "opposition_goal" -> oppG++
                    }
                }
                _gameState.value = LiveGameState(
                    ourGoals = ourG,
                    oppositionGoals = oppG,
                    events = events,
                    isGameActive = true
                )
            }
        }
    }

    fun sendEvent(event: GameEvent) {
        currentGameId?.let { gameId ->
            viewModelScope.launch {
                repository.addEvent(gameId, event)
            }
        }
    }

    fun endGame() {
        currentGameId?.let { gameId ->
            viewModelScope.launch {
                repository.endGame(gameId)
                _gameState.update { it.copy(isGameActive = false) }
            }
        }
    }
}