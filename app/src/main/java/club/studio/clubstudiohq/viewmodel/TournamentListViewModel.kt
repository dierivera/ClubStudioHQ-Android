package club.studio.clubstudiohq.viewmodel;

// viewmodel/TournamentListViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import club.studio.clubstudiohq.data.Tournament

import club.studio.clubstudiohq.repository.FirestoreRepository;
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TournamentListViewModel(private val repository:FirestoreRepository) : ViewModel() {
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadTournaments() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTournaments()
            _tournaments.value = result
            _isLoading.value = false
        }
    }
}