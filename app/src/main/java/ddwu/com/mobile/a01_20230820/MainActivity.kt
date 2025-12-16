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
import ddwu.com.mobile.a01_20230820.data.KakaoPlace
import ddwu.com.mobile.a01_20230820.data.KakaoSearchResponse
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

    //geocoder
    val geocoder: Geocoder by lazy {
        Geocoder(this, Locale.getDefault())
    }

    // 현재 위치 마커
    private var myLocationMarker: Marker? = null

    // 리뷰 마커들만 관리
    private val reviewMarkers = mutableListOf<Marker>()

    // 기존 검색 결과 단일 마커
    private var searchResultMarker: Marker? = null

    // 데이터
    private lateinit var reviewDao: ReviewDao
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
        val db = ReviewDatabase.getDatabase(this)
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

                        Log.d(TAG, "검색 결과 개수: ${places.size}")

//                        places.forEach { place ->
//                            Log.d(TAG, "이름=${place.place_name}, 전화=${place.phone}, 좌표=(${place.y}, ${place.x})")
//                        }

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
            if (result.resultCode == RESULT_OK) {
                val x = result.data?.getStringExtra("x")
                val y = result.data?.getStringExtra("y")

                if (x != null && y != null) {
                    val target = LatLng(y.toDouble(), x.toDouble())

                    val name = result.data?.getStringExtra("name") ?: "선택한 장소"
                    val address = result.data?.getStringExtra("address") ?: ""

                    // 지도 이동
                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(target, 17f)
                    )
                    // 기존 검색 마커 제거
                    searchResultMarker?.remove()
                    // 새 마커 추가
                    searchResultMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(target)
                            .title(name)
                            .snippet(address)
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_BLUE
                            ))
                    )
                    searchResultMarker?.showInfoWindow()
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

            observeReviewMarkers()

            showMyLocation()

            // 마커 클릭 시
            googleMap.setOnMarkerClickListener { marker ->

                val x = marker.position.longitude.toString()
                val y = marker.position.latitude.toString()

                lifecycleScope.launch {
                    val review = reviewDao.getReviewOnce(x, y)

                    if (review != null) {
                        val intent = Intent(this@MainActivity, ReviewDetailActivity::class.java)
                        intent.putExtra("review", review)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "이 장소에는 저장된 리뷰가 없습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                true // 기본 InfoWindow 클릭 동작 막기
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

    // 리뷰 마커
    private fun observeReviewMarkers() {
        lifecycleScope.launch {
            reviewDao.getAllReviews().collect { reviews ->

                reviewMarkers.forEach { it.remove() }
                reviewMarkers.clear()

                // 리뷰 있는 좌표들
                val reviewedLocations = reviews.map {
                    Pair(it.x, it.y)
                }.toSet()

                // 검색 결과 기준으로 리뷰 마커만 다시 추가
                for (place in searchResults) {

                    if (place.x.isNullOrBlank() || place.y.isNullOrBlank()) {
                        continue   // 좌표 없는 장소는 스킵
                    }

                    val hasReview = reviewedLocations.contains(
                        Pair(place.x, place.y)
                    )
                    val markerColor =
                        if (hasReview)
                            BitmapDescriptorFactory.HUE_ORANGE   // 리뷰 있음
                        else
                            BitmapDescriptorFactory.HUE_BLUE     // 기본

                    val lat = place.y!!.toDouble()
                    val lng = place.x!!.toDouble()

                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(lat, lng))
                            .title(place.place_name)
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                    )
                    // Flow 관리 대상은 리뷰 마커만
                    marker?.let { reviewMarkers.add(it) }
                }
            }
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