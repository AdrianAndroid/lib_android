package tv.athena.code.androidcodestyle.demo.imageloader

import android.content.Context
import android.widget.ImageView
import tv.athena.code.androidcodestyle.R
import tv.athena.core.axis.Axis
import tv.athena.imageloader.api.IImageloaderService
import tv.athena.imageloader.api.ImagePriority

/**
 * Created by MoHuaQing on 2018/8/14.
 */
object Imageloader {
    var imageloader: IImageloaderService = Axis.getService(IImageloaderService::class.java)!!

    fun loadImage(context: Context, v: ImageView) {
        Imageloader.imageloader
            .with(context)
            //转换为bitmap
            .asDrawable()
            //加载图片的来源
            .load("https://78.media.tumblr.com/a0c1be3183449f0d207a022c28f4bbf7/tumblr_p1p2cduAiA1wmghc4o1_500.gif")
            //转为圆形图片
//                .toCircle()
            //请求优先级
            .priority(ImagePriority.HIGH)
            //loading占位图
            .placeholder(R.drawable.ic_launcher_background)
            //加载失败占位图
            .error(R.drawable.ic_launcher_background)
            //缩略图 区间[0,1]
//                .thumbnail(0.5f)
            //指定宽高
//                .override(200,200)
            //超时时间
//                .timeout(5 * 1000)
//                //忽略内存缓存
//                .skipMemoryCache(true)
//                //忽略磁盘缓存
//                .skipDiskCache(true)
//                //formatDecode
//                .format(Bitmap.Config.RGB_565)
            //目标控件
            .into(v)
    }
}