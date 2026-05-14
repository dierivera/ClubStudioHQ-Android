package club.studio.clubstudiohq.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import club.studio.clubstudiohq.R
import club.studio.clubstudiohq.adapters.TournamentAdapter
import club.studio.clubstudiohq.databinding.FragmentTournamentListBinding
import club.studio.clubstudiohq.repository.FirestoreRepository
import club.studio.clubstudiohq.viewmodel.TournamentListViewModel
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class TournamentListFragment : Fragment() {
    private var _binding: FragmentTournamentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentListViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TournamentListViewModel(FirestoreRepository()) as  T
            }
        }
    }
    private lateinit var adapter: TournamentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        viewModel.loadTournaments()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.tournaments.collect { tournaments ->
                adapter.submitList(tournaments)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TournamentAdapter { tournament ->
            val action = TournamentListFragmentDirections.actionToGameSetup(tournament.id, tournament.name)
            view?.findNavController()?.navigate(action)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}