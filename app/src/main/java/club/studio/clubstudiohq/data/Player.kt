package club.studio.clubstudiohq.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

//load player from firebase firestore
@Parcelize
data class Player(
    val id: String = "",
    val name: String = "",
    val type: String = "player",
    val isActive: Boolean = true
) : Parcelable