package com.example.gc_last.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
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

    /** 노선색을 배경으로 쓸 때 곱하는 비율. 낮을수록 어둡다. */
    private const val BACKGROUND_TINT = 0.32f

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
                setColor(Color.WHITE)
                setStroke(ring, color)
            }
        )
    }

    /** 화면 배경을 노선색을 어둡게 섞은 색으로 칠한다. 글자가 흰색이라 대비가 필요하다. */
    fun applyBackground(root: View, line: SubwayLine?) {
        val color = colorOf(line) ?: return
        root.setBackgroundColor(
            Color.rgb(
                (Color.red(color) * BACKGROUND_TINT).toInt(),
                (Color.green(color) * BACKGROUND_TINT).toInt(),
                (Color.blue(color) * BACKGROUND_TINT).toInt()
            )
        )
    }

    private fun colorOf(line: SubwayLine?): Int? =
        line?.let { runCatching { Color.parseColor(it.color) }.getOrNull() }
}
