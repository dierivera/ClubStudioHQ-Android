package club.studio.clubstudiohq.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import club.studio.clubstudiohq.R
import club.studio.clubstudiohq.data.Player
import club.studio.clubstudiohq.databinding.FragmentGameSetupBinding
import club.studio.clubstudiohq.repository.FirestoreRepository
import club.studio.clubstudiohq.viewmodel.GameSetupViewModel
import kotlinx.coroutines.launch

class GameSetupFragment : Fragment() {

    private var _binding: FragmentGameSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GameSetupViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GameSetupViewModel(FirestoreRepository()) as T
            }
        }
    }

    private var tournamentId: String = ""
    private var playersList: List<Player> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tournamentId = arguments?.getString("tournamentId") ?: return
        val tournamentName = arguments?.getString("tournamentName") ?: ""

        binding.tvTournamentName.text = tournamentName

        // Disabled by default
        binding.btnStartGame.isEnabled = false

        // Load club name
        lifecycleScope.launch {
            viewModel.loadClubName()

            viewModel.clubName.collect { club ->
                Log.d("GameSetup", "Club loaded: $club")

                binding.tvClubName.text =
                    getString(R.string.our_club) + ": $club"
            }
        }

        // Load players
        lifecycleScope.launch {
            Log.d("GameSetup", "Loading players for tournament: $tournamentId")

            viewModel.loadPlayers(tournamentId)

            viewModel.players.collect { players ->

                Log.d("GameSetup", "Collect triggered")
                Log.d("GameSetup", "Players loaded: ${players.size}")
                Log.d("GameSetup", "Players data: $players")

                players.forEach {
                    Log.d("GameSetup", "Player: ${it.name}")
                }

                playersList = players

                // TEMP DEBUG UI
                binding.tvTournamentName.text =
                    "$tournamentName (${players.size} players)"

                updateStartButtonState()
            }
        }

        binding.etOpponentTeam.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    updateStartButtonState()
                }

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            }
        )

        binding.btnStartGame.setOnClickListener {

            val opponent =
                binding.etOpponentTeam.text.toString().trim()

            Log.d("GameSetup", "Start button clicked")
            Log.d("GameSetup", "Opponent: $opponent")
            Log.d("GameSetup", "Players count: ${playersList.size}")

            if (opponent.isNotEmpty() && playersList.isNotEmpty()) {

                lifecycleScope.launch {

                    val clubName = viewModel.clubName.value

                    Log.d("GameSetup", "Creating game...")
                    Log.d("GameSetup", "Club: $clubName")

                    val gameId = viewModel.createGame(
                        tournamentId,
                        opponent,
                        clubName
                    )

                    Log.d("GameSetup", "Game created with ID: $gameId")

                    val action =
                        GameSetupFragmentDirections.actionToLiveGame(
                            gameId,
                            playersList.toTypedArray()
                        )

                    findNavController().navigate(action)
                }

            } else {
                Log.d(
                    "GameSetup",
                    "Cannot start game. Opponent empty or players empty."
                )
            }
        }
    }

    private fun updateStartButtonState() {

        val opponent =
            binding.etOpponentTeam.text.toString().trim()

        val shouldEnable =
            opponent.isNotEmpty() && playersList.isNotEmpty()

        Log.d(
            "GameSetup",
            "Button enabled: $shouldEnable | Opponent: ${opponent.isNotEmpty()} | Players: ${playersList.isNotEmpty()}"
        )

        binding.btnStartGame.isEnabled = shouldEnable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}