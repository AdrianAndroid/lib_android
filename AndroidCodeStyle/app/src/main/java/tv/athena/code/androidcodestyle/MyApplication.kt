package tv.athena.code.androidcodestyle

import tv.athena.platform.BaseApplication


/**
 * @author huangfan(kael)
 * @time 2018/7/13 15:40
 */
class MyApplication : BaseApplication() {

    /**
     * 需要延迟初始化的任务
     */
    override fun delayTask() {
    }

    /**
     * true KLog才在控制台输出
     */
    override fun isDebugger(): Boolean {
        return true
    }

    /**
     * 崩溃的crash Id
     */
    override fun crashAppId(): String {
        return "crashAppId"
    }

    override fun enableMultiDex(): Boolean {
        return true
    }

    override fun onCreate() {
        super.onCreate()
    }
}
