package com.example.gc_last.ui

import com.example.gc_last.model.Station
import kotlin.math.hypot

/**
 * 노선을 어떤 순서로 이어 그릴지 좌표에서 직접 계산한다.
 *
 * ## 왜 역번호 순서로는 안 되는가
 * 응답에는 "이 역 다음이 저 역"이라는 정보가 없다. 처음에는 공식 역번호(`FR_CODE`) 순서로
 * 이었는데 지선이 있는 노선에서 없는 선로가 그려졌다.
 *
 * - 2호선: 신정지선(`234-1`~`234-4`)이 신도림(`234`)과 문래(`235`) 사이에 끼어
 *   **신정네거리 → 문래** 선이 생겼다.
 * - 5호선: 하남 방면 끝과 마천 방면 시작이 번호상 이웃이라 **하남시청 → 둔촌동** 선이 생겼다.
 *   5호선은 지선인데도 번호에 `-N` 표시가 없어 번호만으로는 갈라낼 수 없다.
 *
 * ## 지선 정보 + 최소 신장 트리
 * 1. `-N` 이 붙은 역은 갈라지는 본선 역에서 차례로 잇는다(확정).
 * 2. 남은 역은 **가까운 쌍부터** 이어 붙인다(크루스칼). 실제 노선은 이웃한 역끼리 가장
 *    가까우므로 이 방법으로 본선과 지선이 실제 모양대로 이어진다. 지선 끝과 다른 지선 끝
 *    처럼 멀리 떨어진 쌍은 연결에 필요하지 않아 자연히 빠진다.
 * 3. 트리는 고리를 만들지 못하므로 순환선(2호선)은 마지막에 한 번 더 잇는다.
 *
 * 1번이 없으면 2호선 성수지선이 용답에서 한양대(0.97km)로 붙는다. 실제로는 성수(1.97km)에
 * 붙어야 하는데, 거리만 보면 더 가까운 쪽을 고르기 때문이다.
 */
object LineTopology {

    /** 이어 그릴 두 역. */
    data class Edge(val from: Station, val to: Station)

    /**
     * @param stations 좌표를 아는 역만 넘긴다.
     * @param position 투영 좌표(도 단위). 거리 비교에만 쓰므로 단위는 상관없다.
     */
    fun edgesOf(
        stations: List<Station>,
        position: (Station) -> Pair<Float, Float>
    ): List<Edge> {
        if (stations.size < 2) return emptyList()

        val pts = stations.map(position)
        fun dist(i: Int, j: Int): Float =
            hypot((pts[i].first - pts[j].first).toDouble(), (pts[i].second - pts[j].second).toDouble())
                .toFloat()

        val parent = IntArray(stations.size) { it }
        fun find(a: Int): Int {
            var x = a
            while (parent[x] != x) {
                parent[x] = parent[parent[x]]
                x = parent[x]
            }
            return x
        }
        fun union(a: Int, b: Int): Boolean {
            val ra = find(a)
            val rb = find(b)
            if (ra == rb) return false
            parent[ra] = rb
            return true
        }

        val edges = mutableListOf<Edge>()
        val lengths = mutableListOf<Float>()
        fun add(i: Int, j: Int) {
            if (union(i, j)) {
                edges += Edge(stations[i], stations[j])
                lengths += dist(i, j)
            }
        }

        // 1) 지선은 갈라지는 본선 역에서 차례로 잇는다.
        val indexOfFr = stations.indices.associateBy { stations[it].frCode }
        stations.indices
            .filter { stations[it].isBranch }
            .groupBy { stations[it].baseCode }
            .forEach { (base, members) ->
                val chain = buildList {
                    indexOfFr[base]?.let { add(it) }
                    addAll(members.sortedBy { branchOrder(stations[it].frCode) })
                }
                chain.zipWithNext { a, b -> add(a, b) }
            }

        // 2) 남은 역은 가까운 쌍부터 이어 붙인다.
        val candidates = ArrayList<Triple<Float, Int, Int>>()
        for (i in stations.indices) {
            for (j in i + 1 until stations.size) candidates += Triple(dist(i, j), i, j)
        }
        candidates.sortBy { it.first }
        candidates.forEach { (_, i, j) -> add(i, j) }

        closingEdge(stations, ::dist, edges, lengths)?.let { edges += it }
        return edges
    }

    private fun branchOrder(frCode: String): Int =
        frCode.substringAfter('-').toIntOrNull() ?: 0

    /**
     * 순환선의 고리를 닫는 연결 하나를 찾는다.
     *
     * 연결이 하나뿐인 역(선의 끝) 두 곳이 보통 역 간격 정도로 가까우면 이어 준다.
     * 순환선이 아니면 양 끝이 멀어서 그냥 넘어간다.
     */
    private fun closingEdge(
        stations: List<Station>,
        dist: (Int, Int) -> Float,
        edges: List<Edge>,
        lengths: List<Float>
    ): Edge? {
        if (lengths.isEmpty()) return null
        val limit = lengths.sorted()[lengths.size / 2] * LOOP_CLOSE_FACTOR

        val degree = HashMap<String, Int>()
        edges.forEach { e ->
            degree[e.from.code] = (degree[e.from.code] ?: 0) + 1
            degree[e.to.code] = (degree[e.to.code] ?: 0) + 1
        }
        val ends = stations.indices.filter { (degree[stations[it].code] ?: 0) == 1 }

        var best: Edge? = null
        var bestDistance = Float.MAX_VALUE
        for (a in ends.indices) {
            for (b in a + 1 until ends.size) {
                val i = ends[a]
                val j = ends[b]
                val d = dist(i, j)
                if (d < bestDistance) {
                    bestDistance = d
                    best = Edge(stations[i], stations[j])
                }
            }
        }
        return best?.takeIf { bestDistance <= limit }
    }

    /** 고리를 닫을 때 허용하는 거리(보통 역 간격의 배수). */
    private const val LOOP_CLOSE_FACTOR = 2.5f
}
