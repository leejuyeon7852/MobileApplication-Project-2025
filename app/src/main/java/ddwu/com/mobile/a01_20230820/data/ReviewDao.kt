package ddwu.com.mobile.a01_20230820.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: Review) // 리뷰 있으면 삭제 후 새걸로 교체, 없으면 그대로 저장

    @Query("SELECT * FROM place_review ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<Review>>

    @Query(
        "SELECT * FROM place_review " +
                "WHERE x = :x AND y = :y LIMIT 1"
    )
    suspend fun getReviewByLocation(x: String, y: String): Review?

    @Delete
    suspend fun deleteReview(review: Review)

    @Query("DELETE FROM place_review")
    suspend fun deleteAllReviews()

    @Query(
        "SELECT * FROM place_review " +
                "WHERE x = :x AND y = :y LIMIT 1"
    )
    suspend fun getReviewOnce(x: String, y: String): Review?
}