package ddwu.com.mobile.a01_20230820

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import ddwu.com.mobile.a01_20230820.data.DetailUiModel
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.data.KakaoSearchResponse
import ddwu.com.mobile.a01_20230820.data.bookmark.Bookmark
import ddwu.com.mobile.a01_20230820.data.review.ReviewDao
import ddwu.com.mobile.a01_20230820.data.review.ReviewDatabase
import ddwu.com.mobile.a01_20230820.databinding.ActivityMainBinding
import ddwu.com.mobile.a01_20230820.network.KakaoRetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import java.util.Locale
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivityTAG"
    lateinit var binding : ActivityMainBinding
    // 구글맵
    lateinit var googleMap: GoogleMap
    // 위치 서비스
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    // 마커
    private var myLocationMarker: Marker? = null
    private val placeMarkers = mutableListOf<Marker>()
    private val bookmarkMarkers = mutableListOf<Marker>()

    // 데이터
    private lateinit var reviewDao: ReviewDao
    private lateinit var db: ReviewDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 툴바
        setSupportActionBar(binding.toolbar5)

        // 데이터
        db = ReviewDatabase.getDatabase(this)
        reviewDao = db.reviewDao()

        // 검색 -> API 호출
        binding.btnSearch.setOnClickListener {
            placeMarkers.forEach { it.remove() }
            placeMarkers.clear()

            val keyword = binding.etSearchKeyword.text.toString()

            if (keyword.isEmpty()) {
                Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val baseText = binding.etBaseLocation.text.toString().trim()

            if (baseText.isNotEmpty()) {
                searchFromAddress(baseText, keyword)
            } else {
                searchFromCurrentLocation(keyword)
            }
        }

        // 구글 지도 객체 로딩
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(mapReadyCallback)

        // 실행 시 위치서비스 관련 권한 확인
        checkPermissions()

        //위치 서비스 구현
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(5000)
            .setMinUpdateIntervalMillis(3000)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val currentLoc = locationResult.locations[0]
                val target = LatLng(currentLoc.latitude, currentLoc.longitude)

                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target, 17f)
                )

                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        // 현재 위치 확인 & 표시
        binding.btnMyLocation.setOnClickListener {
            if (checkSelfPermission(ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
                showMyLocation()
            } else {
                checkPermissions()
            }
        }

    }

    // 메뉴
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_review_list -> {
                startActivity(
                    Intent(this, ReviewListActivity::class.java)
                )
                true
            }
            R.id.menu_bookmark_list -> {
                startActivity(
                    Intent(this, BookmarkListActivity::class.java)
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // 주소 기준 위치 검색
    private fun searchFromAddress(address: String, keyword: String) {
        val geocoder = Geocoder(this, Locale.KOREA)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // API 33 이상
            geocoder.getFromLocationName(address, 1) { list ->
                if (list.isEmpty()) {
                    Toast.makeText(this, "출발 위치를 찾을 수 없음", Toast.LENGTH_SHORT).show()
                    return@getFromLocationName
                }
                val baseLatLng = LatLng(
                    list[0].latitude,
                    list[0].longitude
                )
                searchKakaoWithBase(keyword, baseLatLng)
            }
        } else {
            // API 32 이하
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocationName(address, 1)

            if (list.isNullOrEmpty()) {
                Toast.makeText(this, "출발 위치를 찾을 수 없음", Toast.LENGTH_SHORT).show()
                return
            }
            val baseLatLng = LatLng(
                list[0].latitude,
                list[0].longitude
            )
            searchKakaoWithBase(keyword, baseLatLng)
        }
    }

    // 현위치 기준 검색
    private fun searchFromCurrentLocation(keyword: String) {
        val marker = myLocationMarker
        if (marker == null) {
            Toast.makeText(this, "현재 위치 확인 불가", Toast.LENGTH_SHORT).show()
            return
        }
        searchKakaoWithBase(keyword, marker.position)
    }
    // 카카오 검색
    private fun searchKakaoWithBase(keyword: String, baseLatLng: LatLng) {
        KakaoRetrofitClient.service
            .searchKeyword(
                keyword,
                baseLatLng.longitude.toString(),
                baseLatLng.latitude.toString(),
                2000,
                "FD6"
            )
            .enqueue(object : Callback<KakaoSearchResponse> {
                override fun onResponse(
                    call: Call<KakaoSearchResponse>,
                    response: Response<KakaoSearchResponse>
                ) {
                    if (!response.isSuccessful) return

                    val places = response.body()?.documents ?: emptyList()

                    if (places.isEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            "검색 결과가 없습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    val intent = Intent(this@MainActivity, SearchResultActivity::class.java)
                    intent.putExtra("placeList", ArrayList(places))

                    // 기준 위치 넘기기
                    intent.putExtra("baseLat", baseLatLng.latitude)
                    intent.putExtra("baseLng", baseLatLng.longitude)

                    searchLauncher.launch(intent)
                }

                override fun onFailure(call: Call<KakaoSearchResponse>, t: Throwable) {
                    Log.e(TAG, "검색 실패", t)
                }
            })
    }

    // activity
    private val searchLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val x = result.data!!.getStringExtra("x") ?: return@registerForActivityResult
                val y = result.data!!.getStringExtra("y") ?: return@registerForActivityResult
                val name = result.data!!.getStringExtra("name") ?: ""
                val address = result.data!!.getStringExtra("address") ?: ""
                val phone = result.data!!.getStringExtra("phone")

                val target = LatLng(y.toDouble(), x.toDouble())

                placeMarkers.forEach { it.remove() }
                placeMarkers.clear()

                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target, 17f)
                )

                val uiModel = DetailUiModel(
                    x = x,
                    y = y,
                    placeName = name,
                    address = address,
                    phone = phone,
                    reviewText = null,
                    imagePath = null
                )

                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(target)
                        .title(name)
                        .snippet(address)
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_BLUE
                            )
                        )
                )

                marker?.tag = uiModel
                marker?.showInfoWindow()
                marker?.let { placeMarkers.add(it) }
            }
        }

    override fun onResume() {
        super.onResume()

        if (::googleMap.isInitialized) {
            lifecycleScope.launch {
                val bookmarks = db.bookmarkDao().getAllBookmarksOnce()
                showBookmarkMarkers(bookmarks)
            }
        }
    }

    /*Google Map 설정*/
    val mapReadyCallback = object : OnMapReadyCallback {
        override fun onMapReady(map: GoogleMap) {
            googleMap = map
            Log.d(TAG, "GoogleMap is ready")

            googleMap.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    Log.d(TAG, "사용자 조작 → 위치 추적 중단")
                }
            }

            showMyLocation()

            lifecycleScope.launch {
                showBookmarkMarkers(
                    db.bookmarkDao().getAllBookmarksOnce()
                )
            }

            // 마커 클릭 시
            googleMap.setOnMarkerClickListener { marker ->
                val uiModel = marker.tag as? DetailUiModel
                    ?: return@setOnMarkerClickListener true

                lifecycleScope.launch {
                    val review = reviewDao.getReviewOnce(uiModel.x, uiModel.y)

                    val finalUiModel =
                        if (review != null) {
                            uiModel.copy(phone = review.phone)
                        } else {
                            uiModel
                        }

                    val intent = Intent(this@MainActivity, ReviewDetailActivity::class.java)
                    intent.putExtra("uiModel", finalUiModel)

                    if (review != null) {
                        intent.putExtra("review", review)
                    }

                    startActivity(intent)
                }
                true
            }

        }
    }

    // 현재 위치 마커로 보여주기
    private fun showMyLocation() {
        if (checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val target = LatLng(location.latitude, location.longitude)
                // 기존 마커 제거
                myLocationMarker?.remove()
                // 새 현재 위치 마커
                myLocationMarker = googleMap.addMarker(
                    MarkerOptions()
                        .position(target)
                        .title("현재 위치")
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )
                        )
                )
                // 지도 이동
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target, 17f)
                )
            } else {
                // lastLocation이 null이면 한 번만 요청
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }
    }

    // 북마크한 곳 마커 추가
    private suspend fun showBookmarkMarkers(bookmarks: List<Bookmark>) {
        bookmarkMarkers.forEach { it.remove() }
        bookmarkMarkers.clear()

        for (bm in bookmarks) {
            // 해당 북마크 위치의 리뷰 조회
            val review = reviewDao.getReviewOnce(bm.x, bm.y)

            val latLng = LatLng(bm.y.toDouble(), bm.x.toDouble())

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(bm.placeName)
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_YELLOW
                        )
                    )
            )

            val uiModel = DetailUiModel(
                x = bm.x,
                y = bm.y,
                placeName = bm.placeName,
                address = bm.address,
                phone = review?.phone,
                reviewText = review?.reviewText,
                imagePath = review?.imagePath
            )

            marker?.tag = uiModel
            marker?.let { bookmarkMarkers.add(it) }
        }
    }

    /*위치 정보 권한 처리*/
    private fun checkPermissions() {    // 권한 확인이 필요한 곳에서 호출
        if (checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "필요 권한 있음")
            // 권한이 이미 있을 경우 필요한 기능 실행
        } else {
            // 권한이 없을 경우 권한 요청
            locationPermissionRequest.launch(
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
            )
        }
    }

    val locationPermissionRequest =
        registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions(), {
                permissions ->
            when {
                permissions.getOrDefault(ACCESS_FINE_LOCATION, false) -> {
                    Log.d(TAG, "정확한 위치 사용") // 정확한 위치 접근 권한 승인거부 후 해야할 작업
                }
                permissions.getOrDefault(ACCESS_COARSE_LOCATION, false) -> {
                    Log.d(TAG, "근사 위치 사용") // 근사 위치 접근 권한 승인 후 해야할 작업
                }
                else -> {
                    Log.d(TAG, "권한 미승인") // 권한 미승인 시 해야 할 작업
                }
            }
        } )
}