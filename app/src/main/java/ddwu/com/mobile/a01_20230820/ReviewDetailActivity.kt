package ddwu.com.mobile.a01_20230820

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.databinding.ActivityPlaceDetailBinding
import ddwu.com.mobile.a01_20230820.file.FileUtil
import com.bumptech.glide.Glide
import java.io.File
import java.io.IOException
import android.app.AlertDialog
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope
import ddwu.com.mobile.a01_20230820.data.Review
import ddwu.com.mobile.a01_20230820.data.ReviewDao
import ddwu.com.mobile.a01_20230820.data.ReviewDatabase
import kotlinx.coroutines.launch


class ReviewDetailActivity : AppCompatActivity() {
    lateinit var detailBinding: ActivityPlaceDetailBinding

    //사진
    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null
    // 데이터
    private lateinit var db: ReviewDatabase
    private lateinit var reviewDao: ReviewDao
    lateinit var place: KakaoPlace
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        detailBinding = ActivityPlaceDetailBinding.inflate(layoutInflater)
        setContentView(detailBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        place = intent.getSerializableExtra("place") as KakaoPlace

        // 데이터 베이스
        db = ReviewDatabase.getDatabase(this)
        reviewDao = db.placeReviewDao()

        // 화면에 정보 보여주기
        detailBinding.tvPName.text = place.place_name

        detailBinding.tvPAddress.text =
            if (place.road_address_name.isNotEmpty())
                place.road_address_name
            else
                place.address_name

        detailBinding.tvPhoneNumber.text = place.phone ?: "전화번호 정보 없음"

        // 지도 이동
        detailBinding.btnShowMap.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("x", place.x)
            resultIntent.putExtra("y", place.y)
            resultIntent.putExtra("name", place.place_name)
            resultIntent.putExtra(
                "address",
                if (place.road_address_name.isNotEmpty())
                    place.road_address_name
                else
                    place.address_name
            )

            setResult(RESULT_OK, resultIntent)
            finish()
        }

        // 취소
        detailBinding.btnCancel.setOnClickListener {
            finish()
        }

        // 사진 클릭 -> 카메라 or 갤러리
        detailBinding.imageView.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("사진 촬영 / 선택")
                setMessage("카메라 또는 갤러리 사용")
                setPositiveButton("카메라") { _, _ ->
                    checkCameraPermissionAndOpen()
                }
                setNegativeButton("갤러리") { _, _ ->
                    openGallery()
                }
                setNeutralButton("취소", null)
                show()
            }
        }

        // 저장
        detailBinding.btnSave.setOnClickListener {

            val reviewText = detailBinding.etReview.text.toString()

            if (reviewText.isBlank()) {
                Toast.makeText(this, "리뷰를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val review = Review(
                x = place.x,
                y = place.y,
                placeName = place.place_name,
                address = if (place.road_address_name.isNotEmpty())
                    place.road_address_name
                else
                    place.address_name,
                reviewText = reviewText,
                imagePath = currentPhotoPath
            )

            lifecycleScope.launch {
                reviewDao.upsertReview(review)
                runOnUiThread {
                    Toast.makeText(this@ReviewDetailActivity, "리뷰 저장 완료", Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch {
            val oldReview = reviewDao.getReviewOnce(place.x, place.y)

            if (oldReview != null) {
                // 텍스트 미리 채우기
                detailBinding.etReview.setText(oldReview.reviewText)

                // 사진 있으면 미리 보여주기
                oldReview.imagePath?.let { path ->
                    Glide.with(this@ReviewDetailActivity)
                        .load(File(path))
                        .into(detailBinding.imageView)

                    // 현재 사진 경로도 갱신 (수정 대비)
                    currentPhotoPath = path
                }
            }
        }

    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 이미 권한 있음
                openCamera()
            }

            else -> {
                // 권한 요청
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // 카메라 open
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                FileUtil.createNewFile(this).also {
                    currentPhotoPath = it.absolutePath
                }
            } catch (e: IOException) {
                null
            }

            photoFile?.let {
                val photoUri = FileProvider.getUriForFile(
                    this,
                    "${application.packageName}.fileprovider",
                    it
                )
                currentPhotoUri = photoUri
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                takePictureLauncher.launch(takePictureIntent)
            }
        }
    }

    // 갤러리 open
    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    // 카메라
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Glide.with(this)
                    .load(currentPhotoUri)
                    .into(detailBinding.imageView)
            } else {
                FileUtil.deleteFile(currentPhotoPath)
                currentPhotoPath = null
                currentPhotoUri = null
                Toast.makeText(this, "사진 촬영 취소", Toast.LENGTH_SHORT).show()
            }
        }

    // 갤러리
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(detailBinding.imageView)

                currentPhotoUri = uri
                currentPhotoPath = FileUtil.saveFileToExtStorage(this, uri)
            } else {
                Toast.makeText(this, "사진 선택 취소", Toast.LENGTH_SHORT).show()
            }
        }

    // 카메라 허용
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
}