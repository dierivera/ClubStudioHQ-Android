package club.studio.clubstudiohq.openai

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIService {

    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer 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"
    )
    @POST("v1/chat/completions")
    suspend fun generateInsights(
        @Body request: OpenAIRequest
    ): OpenAIResponse
}