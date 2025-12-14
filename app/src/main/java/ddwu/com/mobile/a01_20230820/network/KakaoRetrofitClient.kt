package ddwu.com.mobile.a01_20230820.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object KakaoRetrofitClient {
    private const val BASE_URL = "https://dapi.kakao.com/"
    private const val API_KEY = "a82c577916bdf94bc0a052a94ce474d1"

    val service: KakaoSearchService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Authorization", "KakaoAK $API_KEY")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .create(KakaoSearchService::class.java)
    }
}