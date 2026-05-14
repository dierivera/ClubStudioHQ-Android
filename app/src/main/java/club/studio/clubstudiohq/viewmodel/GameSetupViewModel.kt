package club.studio.clubstudiohq.viewmodel

import androidx.lifecycle.ViewModel
import club.studio.clubstudiohq.data.Player
import club.studio.clubstudiohq.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameSetupViewModel(private val repository: FirestoreRepository) : ViewModel() {
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _clubName = MutableStateFlow("")
    val clubName: StateFlow<String> = _clubName

    suspend fun loadPlayers(tournamentId: String) {
        _players.value = repository.getPlayersForTournament(tournamentId)
    }

    suspend fun loadClubName() {
        _clubName.value = repository.getClubName()
    }

    suspend fun createGame(tournamentId: String, opponent: String, clubName: String): String {
        return repository.createLiveGame(tournamentId, opponent, clubName)
    }
}