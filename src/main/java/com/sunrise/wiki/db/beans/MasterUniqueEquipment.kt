package com.sunrise.wiki.db.beans

import com.sunrise.wiki.common.I18N
import com.sunrise.wiki.data.*
import com.sunrise.wiki.db.DBHelper
import com.sunrise.wiki.utils.Utils

class MasterUniqueEquipment {
    fun getCharaUniqueEquipment(chara: Chara): Equipment? {
        return DBHelper.get().getUniqueEquipment(chara.unitId)?.let {
            it.getCharaUniqueEquipment(chara).apply {
                val map = mutableMapOf<Item, Int>()
                for (i in 1..10) {
                    val itemId = Utils.getValueFromObject(it, "item_id_$i") as Int
                    if (itemId == 140000) {
                        map[EquipmentPiece(itemId, I18N.getString("princess_heart"))] = Utils.getValueFromObject(it, "consume_num_$i") as Int
                    } else if (itemId in 25000..39999) {
                        map[GeneralItem(itemId, I18N.getString("memory_piece"), ItemType.GENERAL_ITEM)] = Utils.getValueFromObject(it, "consume_num_$i") as Int
                    }
                }
                craftMap = map
            }
        }
    }
}