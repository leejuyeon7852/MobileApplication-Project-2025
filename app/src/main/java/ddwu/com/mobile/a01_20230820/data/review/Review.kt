package ddwu.com.mobile.a01_20230820.data.review

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "review_table",
    indices = [
        Index(value = ["x", "y"], unique = true)   // 장소 중복 방지 -> 1리뷰 1장소
    ]
)
data class Review(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val x: String,              // KakaoPlace.x (경도)
    val y: String,              // KakaoPlace.y (위도)

    val placeName: String,
    val address: String,
    val phone: String?,

    val reviewText: String,

    val imagePath: String? = null,

    val hasReview: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
) : Serializable