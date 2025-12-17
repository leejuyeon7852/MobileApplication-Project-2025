package ddwu.com.mobile.a01_20230820.data

import java.io.Serializable

data class DetailUiModel(
    val x: String,
    val y: String,
    val placeName: String,
    val address: String,
    val phone: String?,
    val reviewText: String?,
    val imagePath: String?
): Serializable