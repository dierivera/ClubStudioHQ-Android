package club.studio.clubstudiohq.repository

import club.studio.clubstudiohq.data.GameEvent
import club.studio.clubstudiohq.data.Player
import club.studio.clubstudiohq.data.Tournament
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // get tourneys
    suspend fun getTournaments(): List<Tournament> {

        return try {

            db.collection("tournaments")
                .get()
                .await()
                .documents
                .map { doc ->

                    Tournament(
                        doc.id,
                        doc.getString("name") ?: "Sin nombre"
                    )
                }

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    // Obtener los jugadores desde club_players
    // TODO: show only players listed on the tourney's roster
    suspend fun getPlayersForTournament(
        tournamentId: String
    ): List<Player> {
        return try {
            val snapshot = db.collection("club_players").whereEqualTo("isActive", true).get().await()
            snapshot.documents.map { doc ->
                val fullName = doc.getString("fullName") ?: "Desconocido"
                val role = doc.getString("role") ?: "player"
                Player(
                    id = doc.id,
                    name = fullName,
                    type = role
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Obtener nombre del club desde configuración
    suspend fun getClubName(): String {

        return try {

            val doc = db.collection("club_config")
                .document("club_info")
                .get()
                .await()

            doc.getString("name")
                ?: "Mi Club"

        } catch (e: Exception) {

            e.printStackTrace()
            "Mi Club"
        }
    }

    // Crear partido nuevo
    suspend fun createLiveGame(
        tournamentId: String,
        opponentTeam: String,
        clubName: String
    ): String {

        val gameId =
            db.collection("live_games")
                .document()
                .id

        val gameData = hashMapOf(
            "tournamentId" to tournamentId,
            "opponentTeam" to opponentTeam,
            "clubName" to clubName,
            "startTime" to System.currentTimeMillis(),
            "status" to "active"
        )

        db.collection("live_games")
            .document(gameId)
            .set(gameData)
            .await()

        return gameId
    }

    // Agregar evento al partido
    suspend fun addEvent(
        gameId: String,
        event: GameEvent
    ) {

        val eventMap: MutableMap<String, Any> =
            mutableMapOf(
                "type" to event.type,
                "timestamp" to event.timestamp
            )

        event.fromPlayerId?.let {
            eventMap["fromPlayerId"] = it
        }

        event.toPlayerId?.let {
            eventMap["toPlayerId"] = it
        }

        event.scorerId?.let {
            eventMap["scorerId"] = it
        }

        event.assisterId?.let {
            eventMap["assisterId"] = it
        }

        event.blamedPlayerIds?.let {
            eventMap["blamedPlayerIds"] = it
        }

        event.playerId?.let {
            eventMap["playerId"] = it
        }

        db.collection("live_games")
            .document(gameId)
            .collection("events")
            .add(eventMap)
            .await()
    }

    // Escuchar eventos en tiempo real
    fun observeGameEvents(
        gameId: String
    ): Flow<List<GameEvent>> = callbackFlow {

        val listener =
            db.collection("live_games")
                .document(gameId)
                .collection("events")
                .orderBy(
                    "timestamp",
                    Query.Direction.ASCENDING
                )
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val events =
                        snapshot?.documents?.mapNotNull { doc ->

                            val type =
                                doc.getString("type")
                                    ?: return@mapNotNull null

                            GameEvent(
                                id = doc.id,
                                type = type,
                                fromPlayerId =
                                    doc.getString("fromPlayerId"),
                                toPlayerId =
                                    doc.getString("toPlayerId"),
                                scorerId =
                                    doc.getString("scorerId"),
                                assisterId =
                                    doc.getString("assisterId"),
                                blamedPlayerIds =
                                    doc.get("blamedPlayerIds")
                                            as? List<String>,
                                playerId =
                                    doc.getString("playerId"),
                                timestamp =
                                    doc.getLong("timestamp")
                                        ?: 0L
                            )

                        } ?: emptyList()

                    trySend(events)
                }

        awaitClose {
            listener.remove()
        }
    }

    // Finalizar partido
    suspend fun endGame(gameId: String) {

        db.collection("live_games")
            .document(gameId)
            .update(
                mapOf(
                    "status" to "finished",
                    "endTime" to System.currentTimeMillis()
                )
            )
            .await()
    }
}