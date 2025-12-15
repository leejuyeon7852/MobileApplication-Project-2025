package ddwu.com.mobile.a01_20230820.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "place_review",
    indices = [
        Index(value = ["x", "y"], unique = true)   // 장소 중복 방지 -> 1리뷰 1장소
    ]
)
data class PlaceReview(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val x: String,              // KakaoPlace.x (경도)
    val y: String,              // KakaoPlace.y (위도)

    val placeName: String,
    val address: String,

    val reviewText: String,

    val imagePath: String? = null,

    val hasReview: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)