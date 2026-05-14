package club.studio.clubstudiohq.data

data class GameEvent(
    val id: String = "",
    val type: String, // "goal", "pass", "turnover", "defense", "opposition_goal", "opposition_drop", "timeout", "halftime"
    val fromPlayerId: String? = null,   // para pases (pasador)
    val toPlayerId: String? = null,     // para pases (receptor)
    val scorerId: String? = null,       // para goles
    val assisterId: String? = null,     // para goles
    val blamedPlayerIds: List<String>? = null, // para pérdidas (1 o 2 jugadores)
    val playerId: String? = null,       // para defensa
    val timestamp: Long = System.currentTimeMillis()
)