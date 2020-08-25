package com.sunrise.wiki.db

import com.sunrise.wiki.data.Equipment
import com.sunrise.wiki.data.EquipmentPiece
import com.sunrise.wiki.db.beans.MasterEquipment
import kotlin.concurrent.thread

class EquipmentStarter {
    val equipmentFragmentMap = mutableMapOf<Int, EquipmentPiece>()
    lateinit var equipmentMap : MutableMap<Int, Equipment>

    var selectedEquipment: Equipment? = null

    /***
     * 从数据库读取所有装备数据。
     */
    fun loadData() {
        if (equipmentMap.isNullOrEmpty()) {
            thread(start = true) {
                equipmentMap.putAll(MasterEquipment().getEquipmentMap())
                callBack?.equipmentLoadFinished()
            }
        }
    }

    var callBack: MasterEquipmentCallBack? = null
    interface MasterEquipmentCallBack {
        fun equipmentLoadFinished()
    }
}