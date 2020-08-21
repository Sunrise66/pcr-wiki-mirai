package com.sunrise.wiki.db.beans

import com.sunrise.wiki.common.I18N
import com.sunrise.wiki.common.Statics
import com.sunrise.wiki.data.HatsuneStage
import com.sunrise.wiki.db.DBHelper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MasterHatsune {
    fun getHatsune(): MutableList<HatsuneStage> {
        val hatsuneStageList = mutableListOf<HatsuneStage>()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
        DBHelper.get().getHatsuneSchedule(null)?.forEach { schedule ->
            val hatsuneStage = HatsuneStage(
                schedule.event_id,
                LocalDateTime.parse(schedule.start_time, formatter),
                LocalDateTime.parse(schedule.end_time, formatter),
                schedule.title
            )
            DBHelper.get().getHatsuneBattle(schedule.event_id)?.forEach { battle ->
                DBHelper.get().getWaveGroupData(battle.wave_group_id_1)?.let {
                    hatsuneStage.battleWaveGroupMap[battle.quest_name] = it.getWaveGroup(true).also { w ->
                        if (hatsuneStage.enemyIcon == Statics.UNKNOWN_ICON) {
                            hatsuneStage.enemyIcon = w.enemyList[0].iconUrl
                        }
                    }
                }
            }
            DBHelper.get().getHatsuneSP(schedule.event_id)?.forEach { sp ->
                DBHelper.get().getWaveGroupData(sp.wave_group_id)?.let {
                    hatsuneStage.battleWaveGroupMap[I18N.getString("sp_mode_d", sp.mode)] = it.getWaveGroup(true)
                }
            }
            hatsuneStageList.add(hatsuneStage)
        }
        return hatsuneStageList
    }
}