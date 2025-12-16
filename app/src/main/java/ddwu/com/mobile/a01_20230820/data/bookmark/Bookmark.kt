package ddwu.com.mobile.a01_20230820.data.bookmark

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "bookmark_table")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val x: String,
    val y: String,

    val placeName: String,
    val address: String
): Serializable
