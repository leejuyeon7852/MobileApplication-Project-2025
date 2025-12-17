package ddwu.com.mobile.a01_20230820

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ddwu.com.mobile.a01_20230820.databinding.ActivityPlaceDetailBinding
import com.bumptech.glide.Glide
import java.io.File
import android.app.AlertDialog
import android.view.View
import androidx.lifecycle.lifecycleScope
import ddwu.com.mobile.a01_20230820.data.DetailUiModel
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.util.ImagePickerHelper
import ddwu.com.mobile.a01_20230820.data.bookmark.Bookmark
import ddwu.com.mobile.a01_20230820.data.review.Review
import ddwu.com.mobile.a01_20230820.data.review.ReviewDao
import ddwu.com.mobile.a01_20230820.data.review.ReviewDatabase
import kotlinx.coroutines.launch


class ReviewDetailActivity : AppCompatActivity() {
    lateinit var detailBinding: ActivityPlaceDetailBinding
    //사진
    private lateinit var imagePicker: ImagePickerHelper
    // 데이터
    private lateinit var db: ReviewDatabase
    private lateinit var reviewDao: ReviewDao

    // 북마크
    private var isBookmarked = false
    private var currentBookmark: Bookmark? = null

    private var currentUiModel: DetailUiModel? = null

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

        // 데이터
        db = ReviewDatabase.getDatabase(this)
        reviewDao = db.reviewDao()

        // 액티비티
        val review = intent.getSerializableExtra("review") as? Review
        val place = intent.getSerializableExtra("place") as? KakaoPlace
        val bookmark = intent.getSerializableExtra("bookmark") as? Bookmark

        val uiModelFromMap =
            intent.getSerializableExtra("uiModel") as? DetailUiModel

        val fromMap =
            intent.hasExtra("uiModel") || intent.hasExtra("place")

        if (!fromMap) {
            detailBinding.btnShowMap.visibility = View.GONE
        }

        currentUiModel = when {
            uiModelFromMap != null -> uiModelFromMap

            review != null -> DetailUiModel(
                x = review.x,
                y = review.y,
                placeName = review.placeName,
                address = review.address,
                reviewText = review.reviewText,
                imagePath = review.imagePath
            )

            place != null -> DetailUiModel(
                x = place.x!!,
                y = place.y!!,
                placeName = place.place_name,
                address = if (place.road_address_name.isNotEmpty())
                    place.road_address_name else place.address_name,
                reviewText = null,
                imagePath = null
            )

            bookmark != null -> DetailUiModel(
                x = bookmark.x,
                y = bookmark.y,
                placeName = bookmark.placeName,
                address = bookmark.address,
                reviewText = null,
                imagePath = null
            )

            else -> null
        }

        currentUiModel?.let { bindUi(it) }

        currentUiModel?.let { model ->
            lifecycleScope.launch {
                val reviewFromDb = reviewDao.getReviewOnce(model.x, model.y)
                if (reviewFromDb != null) {
                    val merged = DetailUiModel(
                        x = reviewFromDb.x,
                        y = reviewFromDb.y,
                        placeName = reviewFromDb.placeName,
                        address = reviewFromDb.address,
                        reviewText = reviewFromDb.reviewText,
                        imagePath = reviewFromDb.imagePath
                    )

                    currentUiModel = merged
                    runOnUiThread { bindUi(merged) }
                }
            }
        }

        // 지도 이동
        detailBinding.btnShowMap.setOnClickListener {
            val model = currentUiModel ?: return@setOnClickListener
            val intent = Intent().apply {
                putExtra("x", model.x)
                putExtra("y", model.y)
                putExtra("name", model.placeName)
                putExtra("address", model.address)
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        // 취소
        detailBinding.btnCancel.setOnClickListener {
            finish()
        }

        // 이미지
        imagePicker = ImagePickerHelper(this) { uri, path ->
            uri?.let {
                Glide.with(this)
                    .load(it)
                    .into(detailBinding.imageView)
            }
        }
        currentUiModel?.imagePath?.let {
            imagePicker.setExistingImage(it)
        }

        imagePicker.register(
            permission = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) imagePicker.openCameraDirect()
                else Toast.makeText(this, "카메라 권한 필요", Toast.LENGTH_SHORT).show()
            },

            camera = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                imagePicker.onCameraResult(result.resultCode == RESULT_OK)
            },

            gallery = registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                imagePicker.onGalleryResult(uri)
            }
        )

        // 사진 클릭 -> 카메라 or 갤러리
        detailBinding.imageView.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("사진 촬영 / 선택")
                setMessage("카메라 또는 갤러리 사용")
                setPositiveButton("카메라") { _, _ ->
                    imagePicker.openCameraWithPermission()
                }
                setNegativeButton("갤러리") { _, _ ->
                    imagePicker.openGallery()
                }
                setNeutralButton("취소", null)
                show()
            }
        }

        // 저장
        detailBinding.btnSave.setOnClickListener {
            val model = currentUiModel ?: return@setOnClickListener
            val text = detailBinding.etReview.text.toString()

            if (text.isBlank()) {
                Toast.makeText(this, "리뷰를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reviewEntity = Review(
                x = model.x,
                y = model.y,
                placeName = model.placeName,
                address = model.address,
                reviewText = text,
                imagePath = imagePicker.currentPhotoPath
            )

            lifecycleScope.launch {
                reviewDao.upsertReview(reviewEntity)
                Toast.makeText(this@ReviewDetailActivity, "저장 완료", Toast.LENGTH_SHORT).show()
            }
        }

        // 북마크
        val bookmarkDao = db.bookmarkDao()

        currentUiModel?.let { model ->
            lifecycleScope.launch {
                val bm = bookmarkDao.getBookmark(model.x, model.y)
                if (bm != null) {
                    isBookmarked = true
                    currentBookmark = bm
                    detailBinding.btnBookmark.setImageResource(
                        R.drawable.bookmark_star_24dp_ffdc01_fill0_wght400_grad0_opsz24
                    )
                }
            }
        }

        // 북마크 저장
        detailBinding.btnBookmark.setOnClickListener {
            val model = currentUiModel ?: return@setOnClickListener

            lifecycleScope.launch {
                if (!isBookmarked) {
                    // 저장
                    val bm = Bookmark(
                        x = model.x,
                        y = model.y,
                        placeName = model.placeName,
                        address = model.address
                    )
                    bookmarkDao.insertBookmark(bm)

                    isBookmarked = true
                    currentBookmark = bm

                    runOnUiThread {
                        detailBinding.btnBookmark.setImageResource(
                            R.drawable.bookmark_star_24dp_ffdc01_fill0_wght400_grad0_opsz24
                        )
                        Toast.makeText(this@ReviewDetailActivity, "북마크 저장", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    // 삭제
                    currentBookmark?.let { bookmarkDao.deleteBookmark(it) }

                    isBookmarked = false
                    currentBookmark = null

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
    private fun bindUi(model: DetailUiModel) {
        detailBinding.tvPName.text = model.placeName
        detailBinding.tvPAddress.text = model.address
        detailBinding.etReview.setText(model.reviewText ?: "")

        model.imagePath?.let {
            Glide.with(this).load(File(it)).into(detailBinding.imageView)
        }
    }

    // 툴바
    override fun onSupportNavigateUp(): Boolean {
        finish()   // 이전 화면으로 복귀
        return true
    }
}