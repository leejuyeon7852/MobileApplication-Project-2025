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
import ddwu.com.mobile.a01_20230820.adapter.BookmarkListAdapter
import ddwu.com.mobile.a01_20230820.data.bookmark.Bookmark
import ddwu.com.mobile.a01_20230820.data.review.ReviewDatabase
import ddwu.com.mobile.a01_20230820.databinding.ActivityBookmarkListBinding
import kotlinx.coroutines.launch

class BookmarkListActivity : AppCompatActivity() {
    lateinit var bmBinding: ActivityBookmarkListBinding
    lateinit var adapter: BookmarkListAdapter
    lateinit var db: ReviewDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bmBinding = ActivityBookmarkListBinding.inflate(layoutInflater)
        setContentView(bmBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //툴바
        setSupportActionBar(bmBinding.toolbar4)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 데이터
        db = ReviewDatabase.getDatabase(this)

        // 어댑터
        adapter = BookmarkListAdapter()
        bmBinding.rvBookmark.layoutManager = LinearLayoutManager(this)
        bmBinding.rvBookmark.adapter = adapter
        bmBinding.rvBookmark.addItemDecoration(
            DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        )

        // 북마크 로드
        loadBookmarks()

        adapter.listener = object : BookmarkListAdapter.OnItemClickListener {

            override fun onItemClick(bookmark: Bookmark) {
                // Detail 화면으로 이동
                val intent = Intent(this@BookmarkListActivity, ReviewDetailActivity::class.java)
                intent.putExtra("bookmark", bookmark)
                startActivity(intent)
            }

            override fun onItemLongClick(bookmark: Bookmark) {
                AlertDialog.Builder(this@BookmarkListActivity)
                    .setTitle("북마크 삭제")
                    .setMessage("이 북마크를 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        lifecycleScope.launch {
                            db.bookmarkDao().deleteBookmark(bookmark)
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }

    private fun loadBookmarks() {
        lifecycleScope.launch {
            db.bookmarkDao().getAllBookmarks().collect { list ->
                adapter.setList(list)
            }
        }
    }

    // 툴바 뒤로가기 처리
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}