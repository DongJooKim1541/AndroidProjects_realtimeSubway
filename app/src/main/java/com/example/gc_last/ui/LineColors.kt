package com.example.gc_last.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.gc_last.R
import com.example.gc_last.model.SubwayLine

/**
 * 화면을 노선색으로 물들이는 공통 처리.
 *
 * 결과·역 정보·시간표 화면이 모두 같은 방식으로 칠해야 하고, 화면마다 따로 두면 한쪽만
 * 고치는 일이 생긴다(저장 목록 배지가 계속 2호선 초록이던 것이 그 예다).
 */
object LineColors {

    /** 역 이름을 감싸는 원의 테두리 두께(dp). */
    private const val RING_WIDTH_DP = 14f

    /** 배경에 노선색을 섞는 비율. 나머지는 바탕색이다. 낮을수록 옅다. */
    private const val BACKGROUND_MIX = 0.18f

    /**
     * 역 이름을 감싸는 원을 노선색 테두리로 그린다.
     *
     * 그림 자원을 덧칠하면 가운데 흰 부분까지 물들기 때문에 [GradientDrawable] 로 새로 그린다.
     */
    fun applyRing(target: ImageView, line: SubwayLine?) {
        val color = colorOf(line) ?: return
        val ring = (RING_WIDTH_DP * target.resources.displayMetrics.density).toInt()
        target.setImageDrawable(
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(target.context, R.color.map_canvas))
                setStroke(ring, color)
            }
        )
    }

    /**
     * 화면 배경을 노선색이 살짝 섞인 아이보리로 칠한다.
     *
     * 노선색을 그대로 깔면 글자가 묻히고, 어둡게 깔면 밝은 앱 색과 어긋난다.
     * 바탕색에 노선색을 조금만 섞어 어느 노선인지 알 수 있을 만큼만 물들인다.
     */
    fun applyBackground(root: View, line: SubwayLine?) {
        val color = colorOf(line) ?: return
        val base = ContextCompat.getColor(root.context, R.color.app_background)
        root.setBackgroundColor(mix(base, color, BACKGROUND_MIX))
    }

    private fun mix(base: Int, tint: Int, ratio: Float): Int = Color.rgb(
        (Color.red(base) * (1 - ratio) + Color.red(tint) * ratio).toInt(),
        (Color.green(base) * (1 - ratio) + Color.green(tint) * ratio).toInt(),
        (Color.blue(base) * (1 - ratio) + Color.blue(tint) * ratio).toInt()
    )

    private fun colorOf(line: SubwayLine?): Int? =
        line?.let { runCatching { Color.parseColor(it.color) }.getOrNull() }
}
