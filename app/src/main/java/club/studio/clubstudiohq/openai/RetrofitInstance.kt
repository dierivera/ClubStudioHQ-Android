package club.studio.clubstudiohq.openai

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val openAIurl = "https://api.openai.com/"

    private val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
    }
    private val client = OkHttpClient.Builder().addInterceptor(logging).build()

    val api: OpenAIService by lazy {
        Retrofit.Builder()
            .baseUrl(openAIurl)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(OpenAIService::class.java)
    }
}