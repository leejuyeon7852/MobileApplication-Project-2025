package ddwu.com.mobile.a01_20230820.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Review::class],
    version = 2,
    exportSchema = false
)
abstract class ReviewDatabase : RoomDatabase() {
    abstract fun placeReviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: ReviewDatabase? = null

        fun getDatabase(context: Context): ReviewDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReviewDatabase::class.java,
                    "place_review_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}