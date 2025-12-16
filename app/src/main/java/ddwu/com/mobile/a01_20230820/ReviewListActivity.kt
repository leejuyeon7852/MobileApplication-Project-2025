package ddwu.com.mobile.a01_20230820

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ddwu.com.mobile.a01_20230820.adapter.ReviewListAdapter
import ddwu.com.mobile.a01_20230820.data.review.Review
import ddwu.com.mobile.a01_20230820.data.review.ReviewDatabase
import ddwu.com.mobile.a01_20230820.databinding.ActivityReviewListBinding
import kotlinx.coroutines.launch
import java.io.File

class ReviewListActivity : AppCompatActivity() {
    lateinit var listBinding: ActivityReviewListBinding
    lateinit var adapter: ReviewListAdapter
    lateinit var db: ReviewDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        listBinding = ActivityReviewListBinding.inflate(layoutInflater)
        setContentView(listBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 툴바
        setSupportActionBar(listBinding.toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // DB
        db = ReviewDatabase.getDatabase(this)

        // RecyclerView
        adapter = ReviewListAdapter()
        listBinding.rvReview.layoutManager = LinearLayoutManager(this)
        listBinding.rvReview.adapter = adapter
        listBinding.rvReview.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        // 데이터 로드
        loadReviews()

        // 클릭 / 롱클릭 처리
        adapter.listener = object : ReviewListAdapter.OnItemClickListener {

            override fun onItemClick(review: Review) {
                val intent = Intent(this@ReviewListActivity, ReviewDetailActivity::class.java)
                intent.putExtra("review", review)
                startActivity(intent)
            }

            override fun onItemLongClick(review: Review) {
                AlertDialog.Builder(this@ReviewListActivity)
                    .setTitle("리뷰 삭제")
                    .setMessage("이 리뷰를 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        lifecycleScope.launch {
                            // DB 삭제
                            db.reviewDao().deleteReview(review)

                            // 사진 파일도 같이 삭제 (있다면)
                            review.imagePath?.let { path ->
                                val file = File(path)
                                if (file.exists()) file.delete()
                            }

                            runOnUiThread {
                                adapter.removeItem(review)
                            }
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadReviews() {
        lifecycleScope.launch {
            db.reviewDao().getAllReviews().collect { list ->
                adapter.setList(list)
            }
        }
    }
}