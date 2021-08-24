package tv.athena.code.androidcodestyle.demo.http.repository

import kotlinx.coroutines.experimental.launch
import tv.athena.code.androidcodestyle.demo.http.HttpInitializer
import tv.athena.code.androidcodestyle.demo.http.VideoApi
import tv.athena.http.api.IRequest
import tv.athena.http.api.IResponse
import tv.athena.http.api.callback.ICallback
import tv.athena.httpadapter.httpEnqueue
import tv.athena.klog.api.KLog
import java.net.URLEncoder

/**
 * Created by MoHuaQing on 2018/8/14.
 */
class VideoRepository {
    companion object {
        const val TAG = "VideoRepository"
        const val VIDEO_BASE_URL_KET = "video_url_key"
        const val VIDEO_URL = "http://sv-topic.yy.com/1.0/topic/index"
    }

    var api: VideoApi = HttpInitializer.httpService.create(VideoApi::class.java)

    /**===========================================HTTP GET REQUEST START=============================================**/
    /**===========================================以下三种GET REQUEST调用效果一样=============================================**/
    /**
     * kotlin扩展函数调用方式
     */
    fun kotlinRequestGet() {
        var httpUrl = VIDEO_URL + "/${URLEncoder.encode("{\"startIndex\":0,\"pageSize\":\"20\" }", "utf-8")}"
        httpEnqueue {
            method = "GET"
            url = httpUrl
            onSuccess {
                // success do Something
                KLog.d(TAG, "kotlinRequestGet()->  success : $it")
            }
            onFail { _, throwable ->
                KLog.d(TAG, "kotlinRequestGet()->  fail : $throwable")
            }
        }
    }

    /**
     * HTTP GET普通调用方式
     */
    fun requestGetVideo1() {
        val params = URLEncoder.encode("{\"startIndex\":0,\"pageSize\":\"20\"}", "utf-8")
        api.requestGetFunVideoData1(params).enqueue(object : ICallback<String> {
            override fun onFailure(request: IRequest<String>, t: Throwable?) {
                KLog.d(TAG, "onFailure()->   : $t")
            }

            override fun onResponse(response: IResponse<String>) {
                KLog.d(TAG, "onResponse()->   : $response")
            }
        })
    }

    /**
     * HTTP GET 协程调用方式
     */
    fun requestGetVideo2() {
        val params = mutableMapOf("data" to URLEncoder.encode("{\"startIndex\":0,\"pageSize\":\"20\"}", "utf-8"))
        launch {
            try {
                val result = api.requestGetFunVideoData2(params).await()
                //成功
                KLog.d(TAG, "requestGetVideo2 success $result ")
            } catch (e: Exception) {
                KLog.e(TAG, "requestGetVideo2 error  ", e)
            }
        }
    }

    /**===========================================HTTP GET REQUEST END=============================================**/


    /**===========================================HTTP POST REQUEST START=============================================**/
    /**===========================================以下三种POST REQUEST调用效果一样=============================================**/

    /**
     * kotlin扩展函数调用方式
     */
    fun kotlinPostVideo() {
        httpEnqueue {
            url = VideoRepository.VIDEO_URL
            method = "POST"
            headers = mutableMapOf("appid" to "4156465464")
            params = mutableMapOf("data" to URLEncoder.encode("{\"startIndex\":0,\"pageSize\":\"20\"}", "utf-8"))
            onSuccess {
                //成功
                KLog.d(TAG, "kotlinRequestGet()->  success : $it")
            }
            onFail { iRequest, throwable ->
                KLog.d(TAG, "kotlinRequestGet()->  fail : $throwable")
            }
        }
    }

    /**
     * HTTP POST 普通调用方式
     */
    fun requestPostVideo1() {
        val params = URLEncoder.encode("{\"startIndex\":0,\"pageSize\":\"20\"}", "utf-8")
        api.requestPostFunVideoData1(params).enqueue(object : ICallback<String> {
            override fun onFailure(request: IRequest<String>, t: Throwable?) {
                KLog.d(TAG, "onFailure()->   : $t")
            }

            override fun onResponse(response: IResponse<String>) {
                KLog.d(TAG, "onResponse()->   : $response")
            }
        })
    }

    /**
     * HTTP POST 协程调用
     */
    fun requestPostVideo2() {
        val params = mutableMapOf("data" to URLEncoder.encode("{\"startIndex\":0,\"pageSize\":\"20\"}", "utf-8"))
        launch {
            try {
                val response = api.requestPostFunVideoData2(VideoRepository.VIDEO_URL, params).await()
                KLog.d(TAG, "onResponse()->   : $response")
            } catch (e: Exception) {
                KLog.d(TAG, "onFailure()->   : $e")
            }
        }
    }

    /**===========================================HTTP POST REQUEST END=============================================**/
}