package com.sunrise.wiki.db

import com.sunrise.wiki.data.ClanBattlePeriod
import com.sunrise.wiki.data.Dungeon
import com.sunrise.wiki.data.Enemy
import kotlin.concurrent.thread

class ClanBattleStarter {
    val periodList = mutableListOf<ClanBattlePeriod>()
    var selectedPeriod: ClanBattlePeriod? = null
    var selectedEnemyList: List<Enemy>? = null
    var selectedMinion: MutableList<Enemy>? = null

    var dungeonList = mutableListOf<Dungeon>()

    /***
     * 从数据库读取所有会战数据。
     * 此方法应该且仅应该在程序初始化时或数据库更新完成后使用。
     */
    fun loadData() {
        if (periodList.isNullOrEmpty()) {
//            thread(start = true) {
                val innerPeriodList = mutableListOf<ClanBattlePeriod>()
                DBHelper.get().getClanBattlePeriod()?.forEach {
                    innerPeriodList.add(it.transToClanBattlePeriod())
                }
                periodList.addAll(innerPeriodList)
                callBack?.onLoadFinish()
//            }
        }
    }

    fun loadDungeon() {
        if (dungeonList.isNullOrEmpty()) {
            thread(start = true) {
                DBHelper.get().getDungeons()?.forEach {
                    dungeonList.add(it.dungeon)
                }
            }
        }
    }

    fun mSetSelectedBoss(enemy: Enemy) {
        if (enemy.isMultiTarget) {
            enemy.skills.forEach {
                //多目标Boss技能值暂时仅供参考，非准确值
                it.setActionDescriptions(it.enemySkillLevel, enemy.children[0].property)
            }
        } else {
            enemy.skills.forEach {
                it.setActionDescriptions(it.enemySkillLevel, enemy.property)
            }
        }
        this.selectedEnemyList = listOf(enemy)
    }

    fun mSetSelectedBoss(enemyList: List<Enemy>) {
        enemyList.forEach { enemy ->
            if (enemy.isMultiTarget) {
                enemy.skills.forEach {
                    //多目标Boss技能值暂时仅供参考，非准确值
                    it.setActionDescriptions(it.enemySkillLevel, enemy.children[0].property)
                }
            } else {
                enemy.skills.forEach {
                    it.setActionDescriptions(it.enemySkillLevel, enemy.property)
                }
            }
        }
        this.selectedEnemyList = enemyList
    }

    var callBack: ClanBattleStaterInf? = null

    interface ClanBattleStaterInf {
        fun onLoadFinish()
    }
}