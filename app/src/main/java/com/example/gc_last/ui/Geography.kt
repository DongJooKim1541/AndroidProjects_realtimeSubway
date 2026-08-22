package com.example.gc_last.ui

import android.content.Context
import com.example.gc_last.R

/**
 * 노선도 배경에 옅게 깔 시·도 경계.
 *
 * 노선만 떠 있으면 어디가 육지이고 어느 쪽이 바다인지 알 수 없다. 처음에는 한강과 서해안을
 * 손으로 몇 점만 찍어 넣었는데 해안선이 직선처럼 보여 어색했다. 지금은 공개된 실제 경계
 * 자료를 단순화해서 쓴다.
 *
 * **시·도 경계만 쓴다.** 서울 자치구 경계도 함께 그려 봤더니 도심에서 선이 겹쳐 노선을
 * 읽기 어려웠다.
 *
 * 출처: `southkorea/southkorea-maps` (통계청 2018 시·도 경계, 공개 자료).
 * 노선이 있는 범위(위도 36.3~38.5, 경도 125.9~128.2)만 남기고 좌표를 솎아
 * `res/raw/korea_provinces.txt` 에 `위도,경도;...` 형태로 담았다.
 */
object Geography {

    fun provinces(context: Context): List<List<Pair<Double, Double>>> =
        context.resources.openRawResource(R.raw.korea_provinces).use { input ->
            input.bufferedReader().readLines()
                .filter { it.isNotBlank() }
                .map { line ->
                    line.split(';').mapNotNull { pair ->
                        val parts = pair.split(',')
                        val lat = parts.getOrNull(0)?.toDoubleOrNull()
                        val lon = parts.getOrNull(1)?.toDoubleOrNull()
                        if (lat != null && lon != null) lat to lon else null
                    }
                }
                .filter { it.size >= 8 }
        }
}
