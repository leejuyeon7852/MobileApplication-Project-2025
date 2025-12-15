package ddwu.com.mobile.a01_20230820.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaceReview::class],
    version = 2,
    exportSchema = false
)
abstract class PlaceReviewDatabase : RoomDatabase() {
    abstract fun placeReviewDao(): PlaceReviewDao

    companion object {
        @Volatile
        private var INSTANCE: PlaceReviewDatabase? = null

        fun getDatabase(context: Context): PlaceReviewDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaceReviewDatabase::class.java,
                    "place_review_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}