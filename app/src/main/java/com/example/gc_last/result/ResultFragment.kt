package com.example.gc_last.result

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gc_last.R
import com.example.gc_last.model.StationCatalog
import com.example.gc_last.model.SubwayLine
import com.example.gc_last.network.SubwayApi
import com.example.gc_last.ui.FreshAdapter
import com.example.gc_last.util.NavKeys
import kotlinx.android.synthetic.main.fragment_result.*
import kotlinx.android.synthetic.main.fragment_result.view.*

/** 검색 결과 화면. 선택한 역의 다가오는 열차를 보여주고 저장할 수 있다. */
class ResultFragment : Fragment() {

    // 이전에는 requireActivity() 스코프라 화면을 벗어나도 상태가 남았다.
    private val resultViewModel by lazy {
        ViewModelProvider(this).get(ResultViewModel::class.java)
    }

    private val resultAdapter = FreshAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_result, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectSubway = arguments?.getString(NavKeys.SELECT_SUBWAY)
        val selectDay = arguments?.getString(NavKeys.SELECT_DAY)
        val resultDirection = arguments?.getString(NavKeys.RESULT_DIRECTION)

        if (selectSubway == null || selectDay == null || resultDirection == null) {
            // 조건 없이 들어온 경우 빈 화면 대신 실패를 알리고 되돌아간다.
            Toast.makeText(requireContext(), R.string.load_failed, Toast.LENGTH_LONG).show()
            return
        }

        view.recycle_result.adapter = resultAdapter
        view.recycle_result.layoutManager = LinearLayoutManager(requireContext())

        val station = StationCatalog.resolve(selectSubway)
        txt_subway_subwayStation.text = station?.name.orEmpty()
        applyLineColor(view, station?.line)

        resultViewModel.timeTable.observe(viewLifecycleOwner) { trains ->
            resultAdapter.freshList = trains
            view.progress_loader.visibility = View.GONE
            view.floting_save.visibility = View.VISIBLE
        }

        resultViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message ?: return@observe
            val text = message.ifBlank { getString(R.string.load_failed) }
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
            resultViewModel.clearErrorMessage()
        }

        view.floting_save.setOnClickListener {
            onSaveClicked(selectSubway, selectDay, resultDirection)
        }

        resultViewModel.load(selectSubway, selectDay, resultDirection)
    }

    /**
     * 화면을 해당 노선색으로 물들인다.
     *
     * 어느 노선의 결과인지 제목만으로는 알기 어렵다. 역 이름을 감싸는 원은 노선색 테두리로,
     * 배경은 노선색을 어둡게 섞은 색으로 칠한다. 원은 그림 자원 대신 [GradientDrawable] 로
     * 그린다. 자원 색을 덧칠하면 가운데 흰 부분까지 물들기 때문이다.
     */
    private fun applyLineColor(view: View, line: SubwayLine?) {
        line ?: return
        val color = Color.parseColor(line.color)

        view.img_subway_linenum.setImageDrawable(
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke((RING_WIDTH_DP * resources.displayMetrics.density).toInt(), color)
            }
        )
        view.setBackgroundColor(darken(color))
    }

    /** 노선색을 배경으로 쓸 수 있을 만큼 어둡게 만든다. 글자가 흰색이라 대비가 필요하다. */
    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * BACKGROUND_TINT).toInt(),
        (Color.green(color) * BACKGROUND_TINT).toInt(),
        (Color.blue(color) * BACKGROUND_TINT).toInt()
    )

    private fun onSaveClicked(selectSubway: String, selectDay: String, resultDirection: String) {
        if (resultViewModel.timeTable.value.isNullOrEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_to_save, Toast.LENGTH_LONG).show()
            return
        }

        val directionLabel = if (resultDirection == SubwayApi.DIRECTION_UP) {
            getString(R.string.direction_up)
        } else {
            getString(R.string.direction_down)
        }

        resultViewModel.save(
            title = getString(
                R.string.station_suffix,
                StationCatalog.resolve(selectSubway)?.name.orEmpty()
            ),
            days = selectDay,
            direction = directionLabel
        )
        Toast.makeText(requireContext(), R.string.save_success, Toast.LENGTH_LONG).show()
    }

    private companion object {
        /** 역 이름을 감싸는 원의 테두리 두께(dp). */
        const val RING_WIDTH_DP = 14f

        /** 노선색을 배경으로 쓸 때 곱하는 비율. 낮을수록 어둡다. */
        const val BACKGROUND_TINT = 0.32f
    }
}
