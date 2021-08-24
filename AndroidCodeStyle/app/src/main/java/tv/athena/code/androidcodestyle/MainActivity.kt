package tv.athena.code.androidcodestyle

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import tv.athena.annotation.MessageBinding
import tv.athena.annotation.Scheduler
import tv.athena.code.androidcodestyle.api.IAndroidCodeStyleService
import tv.athena.code.androidcodestyle.demo.imageloader.Imageloader
import tv.athena.core.axis.Axis
import tv.athena.core.sly.Sly
import tv.athena.core.sly.SlyMessage
import tv.athena.klog.api.KLog

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Sly.subscribe(this)
        Axis.getService(IAndroidCodeStyleService::class.java)?.provideApi()
        Sly.postMessage(TestEvent())
        Sly.postMessage(TestEvent1())
        Sly.postMessage(TestEvent2())
    }

    fun loadImage(v: View) {
        Imageloader.loadImage(this@MainActivity, iv)
    }


    @MessageBinding(scheduler = Scheduler.sync)
    fun testMessage(event: TestEvent) {
        KLog.e(TAG, "-------------------------testMessage ${event.javaClass.name}------------------------------------")
    }

    @MessageBinding(scheduler = Scheduler.main)
    fun testMessageInMain(event: TestEvent1) {
        KLog.e(TAG, "-------------------------testMessageInMain ${event.javaClass.name}------------------------------------")
    }

    @MessageBinding(scheduler = Scheduler.io, delay = 500)
    fun testMessageInIo(event: TestEvent2) {
        KLog.e(TAG, "-------------------------testMessageInIo ${event.javaClass.name}------------------------------------")
    }

    override fun onDestroy() {
        super.onDestroy()
        Sly.unSubscribe(this)
    }
}

class TestEvent : SlyMessage
class TestEvent1 : SlyMessage
class TestEvent2 : SlyMessage