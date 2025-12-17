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
    // 현재 검색 결과 장소들
    private var searchResults: List<KakaoPlace> = emptyList()

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
            val keyword = binding.etSearchKeyword.text.toString()

            if (keyword.isEmpty()) {
                Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 음식점만 필터링
            searchKakao(keyword, "FD6")
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

    // 카카오 검색
    private fun searchKakao(keyword: String, category: String) {
        Log.d(TAG, "검색 요청: $keyword")

        KakaoRetrofitClient.service
            .searchKeyword(keyword, null, null, 2000, category)
            .enqueue(object : Callback<KakaoSearchResponse> {
                override fun onResponse(
                    call: Call<KakaoSearchResponse>,
                    response: Response<KakaoSearchResponse>
                ) {
                    if (response.isSuccessful) {
                        val places = response.body()?.documents ?: emptyList()

                        searchResults = places

                        lifecycleScope.launch {
                            val bookmarks = db.bookmarkDao().getAllBookmarksOnce()
                            val bookmarkSet = bookmarks.map { it.x to it.y }.toSet()
                            showPlacesOnMap(searchResults, bookmarkSet)
                            showBookmarkMarkers(bookmarks)
                        }

                        // 검색 결과 리스트로 이동
                        val intent = Intent(this@MainActivity, SearchResultActivity::class.java)
                        intent.putExtra("placeList", ArrayList(places))

                        // 내 위치 넘기기
                        intent.putExtra("myLat", myLocationMarker?.position?.latitude)
                        intent.putExtra("myLng", myLocationMarker?.position?.longitude)

                        searchLauncher.launch(intent)

                    } else {
                        Log.e(TAG, "응답 실패 code=${response.code()}")
                    }
                }

                override fun onFailure(call: Call<KakaoSearchResponse>, t: Throwable) {
                    Log.e(TAG, "통신 실패", t)
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
                val bookmarks = db.bookmarkDao().getAllBookmarksOnce()
                showBookmarkMarkers(bookmarks)
            }

            // 마커 클릭 시
            googleMap.setOnMarkerClickListener { marker ->

                val uiModel = marker.tag as? DetailUiModel
                    ?: return@setOnMarkerClickListener true

                lifecycleScope.launch {
                    val review = reviewDao.getReviewOnce(uiModel.x, uiModel.y)

                    val intent = Intent(this@MainActivity, ReviewDetailActivity::class.java)
                    if (review != null) {
                        intent.putExtra("review", review)
                    } else {
                        intent.putExtra("uiModel", uiModel)
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
                reviewText = review?.reviewText,
                imagePath = review?.imagePath
            )

            marker?.tag = uiModel
            marker?.let { bookmarkMarkers.add(it) }
        }
    }

    private fun showPlacesOnMap(
        places: List<KakaoPlace>,
        bookmarkSet: Set<Pair<String, String>>
    ) {
        // 기존 장소 마커 제거
        placeMarkers.forEach { it.remove() }
        placeMarkers.clear()

        for (place in places) {
            if (place.x.isNullOrBlank() || place.y.isNullOrBlank()) continue

            if (bookmarkSet.contains(place.x to place.y)) continue

            val latLng = LatLng(place.y!!.toDouble(), place.x!!.toDouble())
            val isBookmarked = bookmarkSet.contains(place.x to place.y)

            val color =
                if (isBookmarked)
                    BitmapDescriptorFactory.HUE_YELLOW   // 북마크
                else
                    BitmapDescriptorFactory.HUE_BLUE     // 검색 결과

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(place.place_name)
                    .icon(BitmapDescriptorFactory.defaultMarker(color))
            )

            val uiModel = DetailUiModel(
                x = place.x!!,
                y = place.y!!,
                placeName = place.place_name,
                address = if (place.road_address_name.isNotEmpty())
                    place.road_address_name else place.address_name,
                reviewText = null,
                imagePath = null
            )

            marker?.tag = uiModel
            marker?.let { placeMarkers.add(it) }
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