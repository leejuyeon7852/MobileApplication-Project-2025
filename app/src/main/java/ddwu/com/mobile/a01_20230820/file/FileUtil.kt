package ddwu.com.mobile.a01_20230820.file

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class FileUtil {
    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private val TAG = "FileUtilTAG"

        private fun getFileName(context: Context): String {
            val timeStamp = SimpleDateFormat(FILENAME_FORMAT).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return "${storageDir?.path}/${timeStamp}.jpg"
        }

        @Throws(IOException::class)
        fun createNewFile(context: Context): File {
            return File(getFileName(context))
        }

        fun deleteFile(filePath: String?): Boolean {
            if (filePath.isNullOrEmpty()) return false
            val file = File(filePath)
            return file.exists() && file.delete()
        }

        fun saveFileToExtStorage(context: Context, sourceUri: Uri?): String? {
            if (sourceUri == null) return null

            val targetFile = File(getFileName(context))

            return try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "사진 저장 완료: ${targetFile.absolutePath}")
                targetFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "파일 저장 실패", e)
                null
            }
        }
    }
}
