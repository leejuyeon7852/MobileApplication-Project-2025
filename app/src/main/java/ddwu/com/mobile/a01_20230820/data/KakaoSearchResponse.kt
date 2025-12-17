package ddwu.com.mobile.a01_20230820.data

import java.io.Serializable

data class KakaoSearchResponse(
    val documents: List<KakaoPlace>
)

data class KakaoPlace(
    val id: String,
    val place_name: String,
    val category_group_code: String,
    val category_name: String,
    val phone: String?,
    val address_name: String,
    val road_address_name: String,
    val x: String?,
    val y: String?,
    val place_url: String,
    val distance: String?,
    var calcDistance: Float = -1f,
): Serializable

