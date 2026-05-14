package club.studio.clubstudiohq.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import club.studio.clubstudiohq.data.Tournament
import club.studio.clubstudiohq.databinding.ItemTournamentBinding

class TournamentAdapter(private val onItemClick: (Tournament) -> Unit) :
    RecyclerView.Adapter<TournamentAdapter.ViewHolder>() {

    private var items = listOf<Tournament>()

    fun submitList(list: List<Tournament>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTournamentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tournament = items[position]
        holder.binding.tvTournamentName.text = tournament.name
        holder.itemView.setOnClickListener { onItemClick(tournament) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val binding: ItemTournamentBinding) : RecyclerView.ViewHolder(binding.root)
}