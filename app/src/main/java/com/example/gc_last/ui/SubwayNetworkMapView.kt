package com.example.gc_last.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import com.example.gc_last.R
import com.example.gc_last.model.Station
import com.example.gc_last.model.StationCatalog
import com.example.gc_last.model.SubwayLine
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min

/**
 * 수도권 전철 노선도. 역의 실제 위경도로 그린다.
 *
 * 공식 노선도는 사람이 직선과 45도로 정리해 만든 도안이라 데이터만으로는 재현할 수 없다.
 * 대신 `subwayStationMaster` 가 주는 역 좌표를 그대로 찍어 실제 지형에 맞는 형태로 그린다.
 * 2호선이 실제로 순환선 모양이 되고, 노선끼리 교차하는 위치도 실제와 같다.
 *
 * - 노선: 역번호 순서대로 이어 그린다. 지선으로 건너뛰는 구간은 [BREAK_KM] 보다 멀면
 *   잇지 않는다. 그러지 않으면 본선 끝에서 지선 시작까지 엉뚱한 직선이 생긴다.
 * - 순환선: 첫 역과 마지막 역이 [LOOP_CLOSE_KM] 안이면 이어 붙인다(2호선).
 * - 환승역: 같은 이름의 역이 여러 노선에 있으면 흰 원으로 크게 그린다. 좌표가 거의 같아
 *   실제 노선도처럼 겹쳐 보인다.
 * - 시간표 미지원 노선(1~9호선 외)은 흐리게 그린다.
 *
 * 두 손가락으로 확대·축소하고, 끌어서 움직인다. 역 이름은 확대했을 때만 나온다.
 */
class SubwayNetworkMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** 역을 탭했을 때. 시간표 미지원 노선도 전달되므로 호출부가 판단한다. */
    var onStationClick: ((Station) -> Unit)? = null

    /** 강조해서 표시할 역. */
    var selected: Station? = null
        set(value) {
            field = value
            invalidate()
        }

    private class Point(val x: Float, val y: Float)

    private val lines: List<SubwayLine> =
        SubwayLine.values().filter { StationCatalog.of(it).any { s -> s.lat != null } }

    /** 그릴 수 있는 역의 위치. 좌표가 없고 같은 이름의 역도 없으면 빠진다. */
    private val placed: Map<String, Point>

    /** 노선별로 이어 그릴 연결. 역번호가 아니라 좌표에서 계산한다([LineTopology]). */
    private val edgesByLine: Map<SubwayLine, List<LineTopology.Edge>>

    private val worldWidth: Float
    private val worldHeight: Float


    /** 화면에 꽉 차게 맞추는 배율. 확대 배율 1 이 이 상태다. */
    private var fitScale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var zoom = 1f

    init {
        // 좌표가 없는 역이 31개 있다. 같은 이름의 역이 다른 노선에 있으면(= 환승역)
        // 그 좌표를 빌려 쓴다. 그러면 수인분당선 오이도·한대앞처럼 4호선과 같은 자리를
        // 쓰는 구간이 끊기지 않는다. 이름이 같은 역이 아예 없으면 위치를 만들어낼 수
        // 없으므로 그리지 않는다(잇지도 않는다).
        val fallback = StationCatalog.stations
            .filter { it.lat != null && it.lon != null }
            .associateBy({ it.name }, { it.lat!! to it.lon!! })

        val located = StationCatalog.stations.mapNotNull { station ->
            val point = station.lat?.let { lat -> station.lon?.let { lon -> lat to lon } }
                ?: fallback[station.name]
            point?.let { station to it }
        }

        val minLat = located.minOf { it.second.first }
        val maxLat = located.maxOf { it.second.first }
        val minLon = located.minOf { it.second.second }
        val maxLon = located.maxOf { it.second.second }
        // 위도 1도와 경도 1도의 실제 길이가 달라 그냥 쓰면 가로로 늘어난다.
        val lonScale = cos(Math.toRadians((minLat + maxLat) / 2))

        placed = located.associate { (station, p) ->
            station.code to Point(
                ((p.second - minLon) * lonScale).toFloat(),
                (maxLat - p.first).toFloat()
            )
        }
        worldWidth = ((maxLon - minLon) * lonScale).toFloat()
        worldHeight = (maxLat - minLat).toFloat()

        edgesByLine = lines.associateWith { line ->
            val drawable = StationCatalog.of(line).filter { placed.containsKey(it.code) }
            LineTopology.edgesOf(drawable) { station ->
                val p = placed.getValue(station.code)
                p.x to p.y
            }
        }
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val dotStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_station_label)
        textAlign = Paint.Align.CENTER
    }

    /**
     * 글자 테두리.
     *
     * 배경이 노선으로 복잡해 글자만 그리면 선 위에서 읽기 어렵다. 캔버스가 밝은
     * 아이보리라 테두리도 밝은 색을 쓴다(예전 어두운 배경에서는 검정이었다).
     */
    private val nameShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.map_label_halo)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
    }

    /** 종이 노선도처럼 밝은 바탕. 노선색이 선명하게 보인다. */
    private val canvasPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.map_canvas)
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val before = zoom
                zoom = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                // 손가락 사이 지점을 기준으로 확대해야 보던 곳이 유지된다.
                val k = zoom / before
                offsetX = detector.focusX - (detector.focusX - offsetX) * k
                offsetY = detector.focusY - (detector.focusY - offsetY) * k
                invalidate()
                return true
            }
        }
    )

    private var lastX = 0f
    private var lastY = 0f
    private var dragged = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (worldWidth <= 0f || worldHeight <= 0f) return
        fitScale = min(w / worldWidth, h / worldHeight) * FIT_MARGIN

        // 전체를 한 화면에 맞추면 소요산·신창·춘천까지 들어와 서울 중심부가 아주 작아진다.
        // 처음에는 시청 주변을 확대해서 보여주고, 축소는 사용자가 하도록 한다.
        val center = selected ?: StationCatalog.resolve(INITIAL_CENTER_CODE)
        if (center != null) focusOn(center, INITIAL_ZOOM) else resetView(w, h)
    }

    /** 전체가 보이는 상태로 되돌린다. */
    private fun resetView(w: Int = width, h: Int = height) {
        zoom = 1f
        offsetX = (w - worldWidth * fitScale) / 2f
        offsetY = (h - worldHeight * fitScale) / 2f
        invalidate()
    }

    private fun screenX(p: Point) = p.x * fitScale * zoom + offsetX
    private fun screenY(p: Point) = p.y * fitScale * zoom + offsetY


    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), canvasPaint)
        linePaint.strokeWidth = dp(1.6f) * zoom.coerceIn(1f, 2.6f)

        // 미지원 노선을 먼저 그려 지원 노선이 위에 오게 한다.
        val ordered = lines.sortedBy { it.timeTableSupported }
        ordered.forEach { line -> drawLine(canvas, line) }
        ordered.forEach { line -> drawStations(canvas, line) }
        if (zoom >= NAME_VISIBLE_ZOOM) drawLabels(canvas)
        selected?.let { drawSelected(canvas, it) }
    }

    /**
     * 역 이름을 겹치지 않게 그린다.
     *
     * 도심은 역이 촘촘해서 전부 쓰면 글자가 서로 덮여 아무것도 읽을 수 없다. 이미 글자를
     * 놓은 자리를 기억해 두고 겹치는 이름은 건너뛴다. 중요한 것부터 자리를 차지하도록
     * 환승역을 먼저 그린다.
     */
    private fun drawLabels(canvas: Canvas) {
        namePaint.textSize = dp(9.5f) * zoom.coerceIn(1f, 2.1f)
        nameShadowPaint.textSize = namePaint.textSize
        nameShadowPaint.strokeWidth = dp(2f)

        val occupied = mutableListOf<RectF>()
        val half = namePaint.textSize / 2f

        val candidates = StationCatalog.stations
            .filter { placed.containsKey(it.code) }
            // 고른 역은 drawSelected 가 더 크게 따로 쓴다. 여기서 또 쓰면 두 글자가
            // 겹쳐 뭉개진다.
            .filterNot { it.code == selected?.code }
            // 자리를 먼저 차지할 순서. 환승역이 우선이다.
            .sortedByDescending { if (StationCatalog.transfersOf(it).isNotEmpty()) 1 else 0 }

        candidates.forEach { station ->
            val p = placed[station.code] ?: return@forEach
            val x = screenX(p)
            val y = screenY(p) - dp(4f) * zoom.coerceAtMost(2f)

            // 화면 밖은 계산도 하지 않는다. 역이 800개라 헛일이 크다.
            if (x < -MARGIN_PX || x > width + MARGIN_PX || y < -MARGIN_PX || y > height + MARGIN_PX) {
                return@forEach
            }

            val w = namePaint.measureText(station.name)
            val box = RectF(x - w / 2f, y - half * 2f, x + w / 2f, y + half * 0.6f)
            if (occupied.any { RectF.intersects(it, box) }) return@forEach
            occupied.add(box)

            namePaint.alpha = if (station.timeTableSupported) 255 else DIM_ALPHA
            canvas.drawText(station.name, x, y, nameShadowPaint)
            canvas.drawText(station.name, x, y, namePaint)
        }
    }

    private fun drawLine(canvas: Canvas, line: SubwayLine) {
        linePaint.color = Color.parseColor(line.color)
        linePaint.alpha = if (line.timeTableSupported) 255 else DIM_ALPHA

        edgesByLine[line]?.forEach { edge ->
            val a = placed[edge.from.code] ?: return@forEach
            val b = placed[edge.to.code] ?: return@forEach
            canvas.drawLine(screenX(a), screenY(a), screenX(b), screenY(b), linePaint)
        }
    }

    private fun drawStations(canvas: Canvas, line: SubwayLine) {
        val color = Color.parseColor(line.color)
        val alpha = if (line.timeTableSupported) 255 else DIM_ALPHA

        // 환승역을 흰 원으로 크게 그리면 도심에서는 원끼리 붙어 노선이 가려진다.
        // 충분히 확대했을 때만 환승 표시를 쓰고, 그 아래로는 노선색 점으로만 그린다.
        val markTransfers = zoom >= TRANSFER_MARK_ZOOM

        StationCatalog.of(line).forEach { station ->
            val p = placed[station.code] ?: return@forEach
            val transfer = markTransfers && StationCatalog.transfersOf(station).isNotEmpty()
            val radius = (if (transfer) dp(2.4f) else dp(1.4f)) * zoom.coerceIn(1f, 3.2f)

            dotFillPaint.color = if (transfer) canvasPaint.color else color
            dotFillPaint.alpha = alpha
            canvas.drawCircle(screenX(p), screenY(p), radius, dotFillPaint)

            if (transfer) {
                dotStrokePaint.color = Color.parseColor("#333333")
                dotStrokePaint.alpha = alpha
                dotStrokePaint.strokeWidth = dp(0.8f) * zoom.coerceIn(1f, 2.6f)
                canvas.drawCircle(screenX(p), screenY(p), radius, dotStrokePaint)
            }

        }
    }

    /** 고른 역과 그 환승역을 강조한다. */
    private fun drawSelected(canvas: Canvas, station: Station) {
        val p = placed[station.code] ?: return
        val color = Color.parseColor(station.line.color)

        dotStrokePaint.color = color
        dotStrokePaint.alpha = 255
        dotStrokePaint.strokeWidth = dp(3f)
        canvas.drawCircle(screenX(p), screenY(p), dp(11f), dotStrokePaint)

        dotFillPaint.color = color
        dotFillPaint.alpha = 255
        canvas.drawCircle(screenX(p), screenY(p), dp(4.5f), dotFillPaint)

        namePaint.textSize = dp(13f)
        nameShadowPaint.textSize = namePaint.textSize
        nameShadowPaint.strokeWidth = dp(3f)
        val ny = screenY(p) - dp(16f)
        canvas.drawText(station.name, screenX(p), ny, nameShadowPaint)
        namePaint.alpha = 255
        canvas.drawText(station.name, screenX(p), ny, namePaint)
    }

    /** 누른 지점에서 가장 가까운 역. 너무 멀면 null. */
    private fun pick(x: Float, y: Float): Station? {
        var best: Station? = null
        var bestDistance = Float.MAX_VALUE
        StationCatalog.stations.forEach { station ->
            val p = placed[station.code] ?: return@forEach
            val d = hypot((screenX(p) - x).toDouble(), (screenY(p) - y).toDouble()).toFloat()
            if (d < bestDistance) {
                bestDistance = d
                best = station
            }
        }
        return best?.takeIf { bestDistance <= dp(TOUCH_SLOP_DP) }
    }

    /**
     * 확대는 [ScaleGestureDetector] 에 맡기고, 이동과 탭은 직접 처리한다.
     *
     * `GestureDetector.SimpleOnGestureListener` 의 인자 널 허용 여부가 SDK 버전마다 달라
     * (`onScroll` 의 `e1` 이 API 34 에서 nullable 로 바뀜) 오버라이드가 깨지기 쉽다.
     * 필요한 동작이 이동과 탭뿐이라 직접 다루는 편이 간단하다.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (hypot(dx.toDouble(), dy.toDouble()) > dp(DRAG_SLOP_DP)) {
                        dragged = true
                    }
                    offsetX += dx
                    offsetY += dy
                    lastX = event.x
                    lastY = event.y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                // 끌어서 움직인 것이면 역 선택이 아니다.
                if (!dragged && !scaleDetector.isInProgress) {
                    pick(event.x, event.y)?.let { onStationClick?.invoke(it) }
                }
            }
        }
        return true
    }

    /** 고른 역이 화면 가운데 오도록 확대해 이동한다. */
    fun focusOn(station: Station?, targetZoom: Float = FOCUS_ZOOM) {
        val p = placed[station?.code] ?: return
        zoom = targetZoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        offsetX = width / 2f - p.x * fitScale * zoom
        offsetY = height / 2f - p.y * fitScale * zoom
        invalidate()
    }

    /** 전체가 보이는 처음 상태로. */
    fun resetZoom() = resetView()

    /** 화면 가운데를 기준으로 한 단계 확대한다. 버튼용. */
    fun zoomIn() = zoomBy(ZOOM_STEP)

    /** 화면 가운데를 기준으로 한 단계 축소한다. 버튼용. */
    fun zoomOut() = zoomBy(1f / ZOOM_STEP)

    /**
     * 화면 가운데를 고정한 채 배율을 바꾼다.
     *
     * 그냥 배율만 바꾸면 왼쪽 위를 기준으로 커져서 보던 위치가 화면 밖으로 밀린다.
     */
    private fun zoomBy(factor: Float) {
        val before = zoom
        zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val k = zoom / before
        val cx = width / 2f
        val cy = height / 2f
        offsetX = cx - (cx - offsetX) * k
        offsetY = cy - (cy - offsetY) * k
        invalidate()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val MIN_ZOOM = 1f
        /**
         * 최대 배율.
         *
         * 도심은 역 간격이 200m 도 안 되는 곳이 있어, 배율이 낮으면 이름을 놓을 자리가 없어
         * 상당수가 생략된다. 충분히 크게 열어 둬야 원하는 역까지 파고들 수 있다.
         */
        const val MAX_ZOOM = 60f

        /** 이 배율 이상에서만 역 이름을 그린다. 그 아래로는 글자가 겹쳐 읽을 수 없다. */
        const val NAME_VISIBLE_ZOOM = 4.5f

        /** 이 배율 이상에서만 환승역을 흰 원으로 표시한다. */
        const val TRANSFER_MARK_ZOOM = 4f

        /** 역을 눌렀다고 인정하는 최대 거리(dp). */
        const val TOUCH_SLOP_DP = 22f

        /** 이만큼 움직이면 탭이 아니라 이동으로 본다(dp). */
        const val DRAG_SLOP_DP = 6f

        /** 역을 고르고 자동으로 맞추는 배율. */
        const val FOCUS_ZOOM = 6f

        /** 확대·축소 버튼 한 번에 바뀌는 배율. */
        const val ZOOM_STEP = 1.6f

        /** 처음 보여줄 중심(시청)과 배율. */
        const val INITIAL_CENTER_CODE = "0201"
        const val INITIAL_ZOOM = 5.5f

        /** 화면에 맞출 때 남기는 여백 비율. */
        const val FIT_MARGIN = 0.94f

        /** 시간표를 제공하지 않는 노선을 흐리게 하는 정도. */
        const val DIM_ALPHA = 70

        /** 이 여유를 두고 화면 밖 글자는 건너뛴다. */
        const val MARGIN_PX = 120f
    }
}
