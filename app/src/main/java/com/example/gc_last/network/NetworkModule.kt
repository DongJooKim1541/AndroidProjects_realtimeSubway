package com.example.gc_last.network

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

//지하철역 정보에 관한 네트워크 모듈
object NetworkModule {

    private val subwayTimekeyValue = "726d4a625777696c35366176564f64"

    val clinent: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .build()
    }
    fun makeHttpUrl_subwayTime(station_code:String,week_tag:String,inout_tag:String): HttpUrl {
        Log.i("NetworkModule", "station_code: $station_code, week_tag: $week_tag, inout_tag: $inout_tag")
        //http://openapi.seoul.go.kr:8088/726d4a625777696c35366176564f64/Json/SearchSTNTimeTableByIDService/1/5/0151/1/1/

        return HttpUrl.Builder()
            .scheme("http")
            .host("openapi.seoul.go.kr")
            .addPathSegment(":8088")
            .addPathSegment(subwayTimekeyValue)
            .addPathSegment("xml")
            .addPathSegment("SearchSTNTimeTableByIDService")
            .addPathSegment("1")
            .addPathSegment("250")
            .addPathSegment("0"+station_code)
            .addPathSegment(week_tag)
            .addPathSegment(inout_tag)
            .build()
    }

    fun makeHttprequest_subwayTime(httpUrl: HttpUrl): Request {
        return Request.Builder()
            .url(httpUrl.toString().replace("/:",":"))
            .get().build();
    }
}