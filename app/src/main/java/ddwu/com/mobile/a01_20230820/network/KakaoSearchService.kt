package ddwu.com.mobile.a01_20230820.network

import ddwu.com.mobile.a01_20230820.data.KakaoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Call

interface KakaoSearchService {

    @GET("v2/local/search/keyword.json")
    fun searchKeyword(
        @Query("query") query: String,
        @Query("x") x: String?,   // 경도
        @Query("y") y: String?,   // 위도
        @Query("radius") radius: Int = 2000
    ): Call<KakaoSearchResponse>
}

