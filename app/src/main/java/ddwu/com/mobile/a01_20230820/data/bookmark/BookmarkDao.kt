package ddwu.com.mobile.a01_20230820.data.bookmark

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmark_table ORDER BY id DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmark_table WHERE x = :x AND y = :y LIMIT 1")
    suspend fun getBookmark(x: String, y: String): Bookmark?
}