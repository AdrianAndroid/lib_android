package tv.athena.code.androidcodestyle.demo.http.bean

import tv.athena.annotation.ProguardKeepClass
import java.io.Serializable

/**
 * Created by MoHuaQing on 2018/8/14.
 */
@ProguardKeepClass
class ResponseContainer {
    var sign: String? = null
    var data: ResponseResult? = null

    @ProguardKeepClass
    class ResponseResult : Serializable {
        var code: Int = 0
        var message: String? = null
        var result: String? = null
    }
}