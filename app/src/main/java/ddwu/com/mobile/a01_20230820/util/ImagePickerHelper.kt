package ddwu.com.mobile.a01_20230820.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ddwu.com.mobile.a01_20230820.file.FileUtil
import java.io.File
import java.io.IOException

class ImagePickerHelper(
    private val activity: AppCompatActivity,
    private val onImageReady: (uri: Uri?, path: String?) -> Unit
) {

    var currentPhotoPath: String? = null
        private set
    var currentPhotoUri: Uri? = null
        private set

    lateinit var permissionLauncher: ActivityResultLauncher<String>
    lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    lateinit var galleryLauncher: ActivityResultLauncher<String>

    fun register(
        permission: ActivityResultLauncher<String>,
        camera: ActivityResultLauncher<Intent>,
        gallery: ActivityResultLauncher<String>
    ) {
        permissionLauncher = permission
        cameraLauncher = camera
        galleryLauncher = gallery
    }

    fun openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) != null) {
            val file: File? = try {
                FileUtil.createNewFile(activity).also {
                    currentPhotoPath = it.absolutePath
                }
            } catch (e: IOException) {
                null
            }

            file?.let {
                val uri = FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    it
                )
                currentPhotoUri = uri
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                cameraLauncher.launch(intent)
            }
        }
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    fun openCameraDirect() {
        openCamera()
    }

    fun onCameraResult(success: Boolean) {
        if (success) {
            onImageReady(currentPhotoUri, currentPhotoPath)
        } else {
            FileUtil.deleteFile(currentPhotoPath)
            currentPhotoPath = null
            currentPhotoUri = null
            Toast.makeText(activity, "사진 촬영 취소", Toast.LENGTH_SHORT).show()
        }
    }

    fun onGalleryResult(uri: Uri?) {
        if (uri != null) {
            val path = FileUtil.saveFileToExtStorage(activity, uri)
            currentPhotoUri = uri
            currentPhotoPath = path
            onImageReady(uri, path)
        } else {
            Toast.makeText(activity, "사진 선택 취소", Toast.LENGTH_SHORT).show()
        }
    }

    fun setExistingImage(path: String) {
        currentPhotoPath = path
    }
}