package ddwu.com.mobile.a01_20230820.util

import android.location.Location

fun calcDistanceMeter(
    myLat: Double,
    myLng: Double,
    placeLat: Double,
    placeLng: Double
): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        myLat, myLng,
        placeLat, placeLng,
        result
    )
    return result[0] // 미터
}