package tv.athena.code.androidcodestyle.hide

import tv.athena.annotation.ServiceRegister
import tv.athena.code.androidcodestyle.api.IAndroidCodeStyleService
import tv.athena.klog.api.KLog

/**
 * @author huangfan(kael)
 * @time 2018/7/13 15:35
 */

@ServiceRegister(serviceInterface = IAndroidCodeStyleService::class)
class AndroidcodestyleService : IAndroidCodeStyleService {

    override fun provideApi() {
        KLog.d("AndroidcodestyleService", "-------------------AndroidcodestyleService provideApi------------------------")
    }
}
