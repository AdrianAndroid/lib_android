package tv.athena.code.androidcodestyle.demo.http

import tv.athena.code.androidcodestyle.demo.http.repository.VideoRepository
import tv.athena.core.axis.Axis
import tv.athena.http.api.IHttpService
import tv.athena.httpadapter.RxJava2HttpAdapterFactory

/**
 * Created by MoHuaQing on 2018/8/14.
 */
object HttpInitializer {
    lateinit var httpService: IHttpService
    fun initHttp() {
        httpService = Axis.getService(IHttpService::class.java)?.config()?.run {
            //设置连接超时时间,单位：秒
            setConnectTimeout(30)
            //添加rxjava请求转换器
            addRequestAdapterFactory(RxJava2HttpAdapterFactory.create())
            //添加请求拦截器，发送请求前回调
//            addRequestInterceptor(YourRequestInterceptor())
            //添加响应拦截器，网络响应前回调
//            addResponseInterceptor(YourResponseInterceptor())
            //是否使用缓存，默认为false
            useCache(true)
            //设置缓存的有效期，单位：秒
            setCacheMaxAge(10 * 60 * 1000)
            //设置失败重试,默认失败不重试
            setCurrentRetryCount(3)
            //设置dns解析
//            dns(YourDns())
            //添加baseUrlMapping,用于@Get  @Post注解的BaseUrl映射

            /**
             * @see VideoApi
             */
            putBaseUrlMapping(VideoRepository.VIDEO_BASE_URL_KET, "http://sv-topic.yy.com")
            //提交配置
            apply()
        }!!
    }
}