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
import android.view.View
import androidx.lifecycle.lifecycleScope
import ddwu.com.mobile.a01_20230820.data.bookmark.Bookmark
import ddwu.com.mobile.a01_20230820.data.review.Review
import ddwu.com.mobile.a01_20230820.data.review.ReviewDao
import ddwu.com.mobile.a01_20230820.data.review.ReviewDatabase
import kotlinx.coroutines.launch


class ReviewDetailActivity : AppCompatActivity() {
    lateinit var detailBinding: ActivityPlaceDetailBinding
    //사진
    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null
    // 데이터
    private lateinit var db: ReviewDatabase
    private lateinit var reviewDao: ReviewDao

    // 북마크
    private var isBookmarked = false
    private var currentBookmark: Bookmark? = null

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

        // 툴바
        setSupportActionBar(detailBinding.toolbar3)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 액티비티
        var place: KakaoPlace? = null
        var review: Review? = null

        review = intent.getSerializableExtra("review") as? Review
        place = intent.getSerializableExtra("place") as? KakaoPlace

        // 데이터 베이스
        db = ReviewDatabase.getDatabase(this)
        reviewDao = db.reviewDao()

        // 화면에 정보 보여주기
        if (review != null) {
            // ReviewList → Detail
            detailBinding.tvPName.text = review!!.placeName
            detailBinding.tvPAddress.text = review!!.address
            detailBinding.etReview.setText(review!!.reviewText)
            detailBinding.tvPhoneNumber.text = "전화번호 정보 없음"

            // 사진
            review!!.imagePath?.let { path ->
                Glide.with(this)
                    .load(File(path))
                    .into(detailBinding.imageView)
                currentPhotoPath = path
            }

            detailBinding.btnShowMap.visibility = View.GONE

        }
        else if (place != null) {
            // SearchResult → Detail
            detailBinding.tvPName.text = place.place_name
            detailBinding.tvPAddress.text =
                if (place.road_address_name.isNotEmpty())
                    place.road_address_name
                else
                    place.address_name

            detailBinding.tvPhoneNumber.text = place.phone ?: "전화번호 정보 없음"

            // 기존 리뷰 있으면 미리 채우기
            lifecycleScope.launch {
                val oldReview = reviewDao.getReviewOnce(place.x!!, place.y!!)
                if (oldReview != null) {
                    detailBinding.etReview.setText(oldReview.reviewText)
                    oldReview.imagePath?.let { path ->
                        Glide.with(this@ReviewDetailActivity)
                            .load(File(path))
                            .into(detailBinding.imageView)
                        currentPhotoPath = path
                    }
                }
            }
        }

        // 지도 이동
        detailBinding.btnShowMap.setOnClickListener {
            place?.let { p ->
                val resultIntent = Intent()
                resultIntent.putExtra("x", p.x)
                resultIntent.putExtra("y", p.y)
                resultIntent.putExtra("name", p.place_name)
                resultIntent.putExtra(
                    "address",
                    if (p.road_address_name.isNotEmpty())
                        p.road_address_name
                    else
                        p.address_name
                )
                setResult(RESULT_OK, resultIntent)
                finish()
            }
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
            val text = detailBinding.etReview.text.toString()
            if (text.isBlank()) {
                Toast.makeText(this, "리뷰를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val x = place?.x ?: review?.x
            val y = place?.y ?: review?.y

            if (x == null || y == null) {
                Toast.makeText(this, "위치 정보 없음", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newReview = Review(
                x = x,
                y = y,
                placeName = review?.placeName ?: place!!.place_name,
                address = review?.address ?: place!!.address_name,
                reviewText = text,
                imagePath = currentPhotoPath
            )

            lifecycleScope.launch {
                reviewDao.upsertReview(newReview)
                runOnUiThread {
                    Toast.makeText(this@ReviewDetailActivity, "저장 완료", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // 북마크
        val bookmarkDao = db.bookmarkDao()

        val x = place?.x ?: review?.x
        val y = place?.y ?: review?.y

        if (x != null && y != null) {
            lifecycleScope.launch {
                val bookmark = bookmarkDao.getBookmark(x, y)
                if (bookmark != null) {
                    isBookmarked = true
                    currentBookmark = bookmark
                    detailBinding.btnBookmark.setImageResource(R.drawable.bookmark_star_24dp_ffdc01_fill0_wght400_grad0_opsz24)
                }
            }
        }

        // 북마크 저장
        detailBinding.btnBookmark.setOnClickListener {

            val bx = place?.x ?: review?.x
            val by = place?.y ?: review?.y
            val name = review?.placeName ?: place?.place_name
            val address = review?.address ?: place?.address_name

            if (bx == null || by == null || name == null || address == null) {
                Toast.makeText(this, "북마크 정보 부족", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (!isBookmarked) {
                    // 저장
                    val bookmark = Bookmark(
                        x = bx,
                        y = by,
                        placeName = name,
                        address = address
                    )
                    bookmarkDao.insertBookmark(bookmark)
                    currentBookmark = bookmark
                    isBookmarked = true

                    runOnUiThread {
                        detailBinding.btnBookmark.setImageResource(
                            R.drawable.bookmark_star_24dp_ffdc01_fill0_wght400_grad0_opsz24
                        )
                        Toast.makeText(this@ReviewDetailActivity, "북마크 저장", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    // 삭제
                    currentBookmark?.let { bookmarkDao.deleteBookmark(it) }
                    currentBookmark = null
                    isBookmarked = false

                    runOnUiThread {
                        detailBinding.btnBookmark.setImageResource(
                            R.drawable.baseline_bookmark_border_24_yellow
                        )
                        Toast.makeText(this@ReviewDetailActivity, "북마크 해제", Toast.LENGTH_SHORT).show()
                    }
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

    // 툴바
    override fun onSupportNavigateUp(): Boolean {
        finish()   // 이전 화면으로 복귀
        return true
    }
}