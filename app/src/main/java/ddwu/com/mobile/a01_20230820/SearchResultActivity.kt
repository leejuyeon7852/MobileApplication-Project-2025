package ddwu.com.mobile.a01_20230820

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ddwu.com.mobile.a01_20230820.adapter.SearchResultAdapter
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.databinding.ActivitySearchResultBinding

class SearchResultActivity : AppCompatActivity() {
    lateinit var searchBinding : ActivitySearchResultBinding
    lateinit var adapter: SearchResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        searchBinding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(searchBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = SearchResultAdapter()

        searchBinding.rvSearchResult.layoutManager = LinearLayoutManager(this)
        searchBinding.rvSearchResult.adapter = adapter

        val divider = DividerItemDecoration(
            this,
            LinearLayoutManager.VERTICAL
        )
        searchBinding.rvSearchResult.addItemDecoration(divider)

        val placeList = intent.getSerializableExtra("placeList") as ArrayList<KakaoPlace>

        adapter.setList(placeList)

        adapter.clickListener = object : SearchResultAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val place = placeList[position]

                val resultIntent = Intent()
                resultIntent.putExtra("x", place.x)
                resultIntent.putExtra("y", place.y)

                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}