package ddwu.com.mobile.a01_20230820

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import ddwu.com.mobile.a01_20230820.adapter.SearchResultAdapter
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.databinding.ActivitySearchResultBinding
import ddwu.com.mobile.a01_20230820.util.calcDistanceMeter

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
        // 액션바
        setSupportActionBar(searchBinding.toolbar)
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

        val myLat = intent.getDoubleExtra("myLat", 0.0)
        val myLng = intent.getDoubleExtra("myLng", 0.0)

        val hasMyLocation = !(myLat == 0.0 && myLng == 0.0)

        placeList.forEach { place ->
            if (hasMyLocation && !place.x.isNullOrBlank() && !place.y.isNullOrBlank()) {
                place.calcDistance = calcDistanceMeter(
                    myLat,
                    myLng,
                    place.y!!.toDouble(),
                    place.x!!.toDouble()
                )
            } else {
                place.calcDistance = -1f
            }
        }

        adapter.setList(placeList)

        placeList.sortWith(
            compareBy<KakaoPlace> {
                if (it.calcDistance < 0) Float.MAX_VALUE else it.calcDistance
            }
        )

        adapter.clickListener = object : SearchResultAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val place = placeList[position]

                val intent = Intent(this@SearchResultActivity, ReviewDetailActivity::class.java)
                intent.putExtra("place", place)

                detailLauncher.launch(intent)
            }
        }
    }

    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val x = result.data?.getStringExtra("x")
                val y = result.data?.getStringExtra("y")

                if (x != null && y != null) {
                    // MainActivity로 그대로 넘기고 종료
                    val intent = Intent()
                    intent.putExtras(result.data!!)
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}