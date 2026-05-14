package club.studio.clubstudiohq.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import club.studio.clubstudiohq.R
import club.studio.clubstudiohq.data.GameEvent
import club.studio.clubstudiohq.databinding.FragmentGameSummaryBinding
import club.studio.clubstudiohq.repository.FirestoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameSummaryFragment : Fragment() {
    private var _binding: FragmentGameSummaryBinding? = null
    private val binding get() = _binding!!
    private lateinit var gameId: String
    private val repository = FirestoreRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gameId = arguments?.getString("gameId") ?: return
        lifecycleScope.launch {
            val events = repository.observeGameEvents(gameId).first()
            val kpis = calculateKPIs(events)
            binding.tvSummary.text = kpis
        }
    }

    private fun calculateKPIs(events: List<GameEvent>): String {
        // TODO: load statistics
        return "MVP: Diego\nGoleador: Diego\nMás pases: Diego"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}