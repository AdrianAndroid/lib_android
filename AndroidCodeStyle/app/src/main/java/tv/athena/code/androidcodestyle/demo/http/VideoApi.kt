package tv.athena.code.androidcodestyle.demo.http

import kotlinx.coroutines.experimental.Deferred
import tv.athena.code.androidcodestyle.demo.http.bean.ResponseContainer
import tv.athena.code.androidcodestyle.demo.http.repository.VideoRepository
import tv.athena.http.api.IRequest
import tv.athena.http.api.annotations.Get
import tv.athena.http.api.annotations.GetParam
import tv.athena.http.api.annotations.GetParamMap
import tv.athena.http.api.annotations.Post
import tv.athena.http.api.annotations.PostParam
import tv.athena.http.api.annotations.PostParamMap
import tv.athena.http.api.annotations.Url

/**
 * Created by MoHuaQing on 2018/8/14.
 */
interface VideoApi {

    /**
     * GET请求,两种写法效果一样
     */
    @Get(baseUrlMapping = VideoRepository.VIDEO_BASE_URL_KET, url = "/1.0/topic/index")
    fun requestGetFunVideoData1(@GetParam("data") data: String): IRequest<String>

    @Get(url = VideoRepository.VIDEO_URL)
    fun requestGetFunVideoData2(@GetParamMap getParamMap: MutableMap<String, String>): Deferred<ResponseContainer>

    //======================================================

    /**
     * POST请求,两种写法效果一样
     */
    @Post(url = VideoRepository.VIDEO_URL)
    fun requestPostFunVideoData1(@PostParam("data") data: String): IRequest<String>

    @Post
    fun requestPostFunVideoData2(@Url url: String, @PostParamMap getParamMap: MutableMap<String, String>): Deferred<ResponseContainer>
}