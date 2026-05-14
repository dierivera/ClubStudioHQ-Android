package club.studio.clubstudiohq.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import club.studio.clubstudiohq.R
import com.google.android.material.button.MaterialButton

class ActionAdapter(
    private val actions: List<String>,
    private val onActionClick: (String) -> Unit
) : RecyclerView.Adapter<ActionAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val btnAction: MaterialButton
    ) : RecyclerView.ViewHolder(btnAction) {

        fun setData(action: String) {
            btnAction.text = action

            btnAction.setOnClickListener {
                onActionClick(action)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        val button = inflater.inflate(
            R.layout.item_action,
            parent,
            false
        ) as MaterialButton

        return ViewHolder(button)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = actions[position]
        holder.setData(action)
    }

    override fun getItemCount(): Int {
        return actions.size
    }
}