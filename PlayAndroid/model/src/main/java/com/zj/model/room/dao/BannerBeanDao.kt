package com.zj.model.room.dao

import androidx.room.*
import com.zj.model.room.entity.BannerBean


/**
 * 版权：Zhujiang 个人版权
 *
 * @author zhujiang
 * 创建日期：2020/9/10
 * 描述：PlayAndroid
 *
 */
@Dao
interface BannerBeanDao {

    @Query("SELECT * FROM banner_bean order by uid desc")
    suspend fun getBannerBeanList(): List<BannerBean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertList(BannerBeanList: List<BannerBean>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(BannerBean: BannerBean)

    @Update
    suspend fun update(BannerBean: BannerBean): Int

    @Delete
    suspend fun delete(BannerBean: BannerBean): Int

    @Delete
    suspend fun deleteList(BannerBeanList: List<BannerBean>): Int

    @Query("DELETE FROM banner_bean")
    suspend fun deleteAll()
}
