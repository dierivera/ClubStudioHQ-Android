package club.studio.clubstudiohq.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import club.studio.clubstudiohq.adapters.ActionAdapter
import club.studio.clubstudiohq.data.GameEvent
import club.studio.clubstudiohq.data.Player
import club.studio.clubstudiohq.databinding.FragmentLiveGameBinding
import club.studio.clubstudiohq.openai.Message
import club.studio.clubstudiohq.openai.OpenAIRequest
import club.studio.clubstudiohq.openai.RetrofitInstance
import club.studio.clubstudiohq.repository.FirestoreRepository
import club.studio.clubstudiohq.viewmodel.LiveGameViewModel

class LiveGameFragment : Fragment() {

    private var _binding: FragmentLiveGameBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveGameViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LiveGameViewModel(
                    FirestoreRepository()
                ) as T
            }
        }
    }

    private lateinit var gameId: String
    private lateinit var players: List<Player>
    // TIMER
    private val gameDurationMillis =
        90 * 60 * 1000L
    private var isTimerRunning = false
    private var timeWhenStopped: Long = 0L
    private var currentHolder: Player? = null
    private val recordedEvents = mutableListOf<GameEvent>()
    private val actions = listOf(
        "Pass",
        "Goal",
        "Turnover",
        "Defense",
        "Drop"
    )
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveGameBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameId = arguments?.getString("gameId")?: return
        val playersArray =arguments?.getParcelableArray("players")
        players = playersArray?.filterIsInstance<Player>() ?: emptyList()
        //init uui
        observeGame()
        setupSpinners()
        setupActionsRecycler()
        setupButtons()
        setupTimer()
    }

    private fun observeGame() {
        viewModel.startObserving(gameId)
        lifecycleScope.launchWhenStarted {
            viewModel.gameState.collect { state ->
                binding.tvScore.text = "${state.ourGoals} - ${state.oppositionGoals}" //score
                if (!state.isGameActive) {
                    val action = LiveGameFragmentDirections.actionToSummary(gameId)
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun setupActionsRecycler() {

        val actions = listOf(
            "PASS",
            "GOAL",
            "TURNOVER",
            "DEFENSE",
            "DROP"
        )

        binding.rvActions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.rvActions.adapter = ActionAdapter(actions) { action ->
                handleAction(action)
            }
    }

    private fun handleAction(action: String) {
        val nextPlayerPosition = binding.spinnerNextPlayer.selectedItemPosition
        if (nextPlayerPosition < 0) {

            Toast.makeText(
                requireContext(),
                "Select a player",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nextPlayer =
            players[nextPlayerPosition]

        when (action) {

            "PASS" -> {

                if (currentHolder == null) {

                    currentHolder = nextPlayer

                    binding.tvCurrentHolder.text =
                        nextPlayer.name

                    Toast.makeText(
                        requireContext(),
                        "Possession started with ${nextPlayer.name}",
                        Toast.LENGTH_SHORT
                    ).show()

                    return
                }

                val event =
                    GameEvent(
                        type = "pass",
                        fromPlayerId =
                            currentHolder!!.id,
                        toPlayerId =
                            nextPlayer.id
                    )

                viewModel.sendEvent(event)
                recordedEvents.add(event)

                currentHolder = nextPlayer

                binding.tvCurrentHolder.text =
                    nextPlayer.name
            }

            "GOAL" -> {

                val event =
                    GameEvent(
                        type = "goal",
                        scorerId =
                            nextPlayer.id,
                        assisterId =
                            currentHolder?.id
                    )

                viewModel.sendEvent(event)
                recordedEvents.add(event)

                currentHolder = null

                binding.tvCurrentHolder.text =
                    "No possession selected"
            }

            "TURNOVER" -> {

                val blamedPlayers =
                    mutableListOf<String>()

                currentHolder?.let {
                    blamedPlayers.add(it.id)
                }

                val event =
                    GameEvent(
                        type = "turnover",
                        blamedPlayerIds =
                            blamedPlayers
                    )

                viewModel.sendEvent(event)
                recordedEvents.add(event)

                currentHolder = null

                binding.tvCurrentHolder.text =
                    "No possession selected"
            }

            "DEFENSE" -> {

                val event =
                    GameEvent(
                        type = "defense",
                        playerId =
                            nextPlayer.id
                    )

                viewModel.sendEvent(event)
                recordedEvents.add(event)
            }

            "DROP" -> {

                val blamedPlayers =
                    mutableListOf<String>()

                blamedPlayers.add(nextPlayer.id)

                val event =
                    GameEvent(
                        type = "drop",
                        blamedPlayerIds =
                            blamedPlayers
                    )

                viewModel.sendEvent(event)
                recordedEvents.add(event)

                currentHolder = null

                binding.tvCurrentHolder.text =
                    "No possession selected"
            }
        }

        Toast.makeText(
            requireContext(),
            "$action saved",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun setupSpinners() {

        // PLAYERS

        val playerNames =
            players.map { it.name }

        val playersAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                playerNames
            )

        playersAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spinnerNextPlayer.adapter =
            playersAdapter
    }

    private fun setupButtons() {

        // OPPONENT GOAL

        binding.btnOppositionGoal.setOnClickListener {

            viewModel.sendEvent(
                GameEvent(
                    type = "opposition_goal"
                )
            )

            currentHolder = null

            binding.tvCurrentHolder.text =
                "No possession selected"
        }

        // OPPONENT TURNOVER

        binding.btnOppositionDrop.setOnClickListener {

            viewModel.sendEvent(
                GameEvent(
                    type = "opposition_drop"
                )
            )
        }

        // TIMEOUT

        binding.btnTimeout.setOnClickListener {

            viewModel.sendEvent(
                GameEvent(
                    type = "timeout"
                )
            )
        }

        // HALFTIME

        binding.btnHalftime.setOnClickListener {

            viewModel.sendEvent(
                GameEvent(
                    type = "halftime"
                )
            )
        }

        binding.btnEndGame.setOnClickListener {

            viewModel.endGame()
        }

        binding.btnStartTimer.setOnClickListener {

            if (!isTimerRunning) {

                if (timeWhenStopped != 0L) {

                    binding.chronometer.base =
                        SystemClock.elapsedRealtime() +
                                timeWhenStopped
                }

                binding.chronometer.start()

                isTimerRunning = true
            }
        }

        binding.btnStopTimer.setOnClickListener {

            if (isTimerRunning) {

                timeWhenStopped =
                    binding.chronometer.base -
                            SystemClock.elapsedRealtime()

                binding.chronometer.stop()

                isTimerRunning = false
            }
        }

        binding.fabInsights.setOnClickListener {

            generateInsights()
        }
    }

    private fun generateInsights() {

        binding.cardInsights.visibility =
            View.VISIBLE

        binding.progressInsights.visibility =
            View.VISIBLE

        binding.tvInsights.text = ""

        lifecycleScope.launchWhenStarted {

            try {

                val prompt = buildInsightsPrompt()

                val request =
                    OpenAIRequest(
                        model = "gpt-4.1-mini",
                        messages = listOf(
                            Message(
                                role = "system",
                                content =
                                    "You are an ultimate frisbee analyst."
                            ),
                            Message(
                                role = "user",
                                content = prompt
                            )
                        )
                    )

                val response =
                    RetrofitInstance.api
                        .generateInsights(request)

                val insights =
                    response.choices.firstOrNull()
                        ?.message
                        ?.content
                        ?: "No insights available."

                binding.progressInsights.visibility =
                    View.GONE

                binding.tvInsights.text =
                    insights

            } catch (e: Exception) {

                e.printStackTrace()

                binding.progressInsights.visibility =
                    View.GONE

                binding.tvInsights.text =
                    "Failed to generate insights."
            }
        }
    }

    private fun buildInsightsPrompt(): String {

        val state = viewModel.gameState.value

        val eventsText =
            recordedEvents.joinToString("\n") { event ->

                when (event.type) {

                    "pass" -> {

                        val from =
                            players.find {
                                it.id == event.fromPlayerId
                            }?.name ?: "Unknown"

                        val to =
                            players.find {
                                it.id == event.toPlayerId
                            }?.name ?: "Unknown"

                        "PASS: $from -> $to"
                    }

                    "goal" -> {

                        val scorer =
                            players.find {
                                it.id == event.scorerId
                            }?.name ?: "Unknown"

                        val assister =
                            players.find {
                                it.id == event.assisterId
                            }?.name ?: "Unknown"

                        "GOAL: scorer=$scorer assist=$assister"
                    }

                    "turnover" -> {

                        val blamed =
                            event.blamedPlayerIds
                                ?.map { id ->
                                    players.find {
                                        it.id == id
                                    }?.name ?: "Unknown"
                                }
                                ?.joinToString()

                        "TURNOVER: $blamed"
                    }

                    "defense" -> {

                        val defender =
                            players.find {
                                it.id == event.playerId
                            }?.name ?: "Unknown"

                        "DEFENSE: $defender"
                    }

                    "drop" -> {

                        val blamed =
                            event.blamedPlayerIds
                                ?.map { id ->
                                    players.find {
                                        it.id == id
                                    }?.name ?: "Unknown"
                                }
                                ?.joinToString()

                        "DROP: $blamed"
                    }

                    "timeout" -> {
                        "TIMEOUT"
                    }

                    "halftime" -> {
                        "HALFTIME"
                    }

                    "opposition_goal" -> {
                        "OPPONENT GOAL"
                    }

                    "opposition_drop" -> {
                        "OPPONENT TURNOVER"
                    }

                    else -> {
                        event.type
                    }
                }
            }

        return """
        You are an elite ultimate frisbee tactical analyst.

        Analyze this live game and provide:

        1. Best performing players
        2. Players struggling
        3. Offensive tendencies
        4. Defensive tendencies
        5. Possession flow observations
        6. Which players should receive more touches
        7. Which players are causing problems
        8. Tactical adjustments
        9. Momentum analysis
        10. Short actionable coaching recommendations

        Current score:
        ${state.ourGoals} - ${state.oppositionGoals}

        Current disc holder:
        ${currentHolder?.name ?: "None"}

        Registered events:
        
        $eventsText

        Keep the response concise, tactical, and useful for a live coach during a game.
    """.trimIndent()
    }

    private fun setupTimer() {

        binding.chronometer.base =
            SystemClock.elapsedRealtime() +
                    gameDurationMillis

        binding.chronometer.setCountDown(true)

        binding.chronometer.setOnChronometerTickListener {
                chronometer ->

            val remainingMillis =
                chronometer.base -
                        SystemClock.elapsedRealtime()

            if (remainingMillis <= 0) {

                chronometer.stop()

                binding.chronometer.text = "00:00"

                isTimerRunning = false
            }
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}