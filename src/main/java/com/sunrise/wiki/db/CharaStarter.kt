package com.sunrise.wiki.db

import com.sunrise.wiki.common.Statics
import com.sunrise.wiki.data.Chara
import com.sunrise.wiki.data.Equipment
import com.sunrise.wiki.data.Minion
import com.sunrise.wiki.data.Property
import com.sunrise.wiki.db.DBHelper.Companion.get
import com.sunrise.wiki.db.beans.MasterUniqueEquipment
import kotlin.concurrent.thread

class CharaStarter {

    lateinit var charaList : MutableList<Chara>

    var maxCharaLevel: Int = 0
    var maxCharaRank: Int = 0
    var maxUniqueEquipmentLevel: Int = 0
    var maxEnemyLevel: Int = 0

    var selectedChara: Chara? = null
    var selectedMinion: MutableList<Minion>? = null
    var backFlag = false

//    var rankComparisonFrom: Int = 0
//    var rankComparisonTo: Int = 0

    /***
     * 从数据库读取所有角色数据。
     * 此方法应该且仅应该在程序初始化时或数据库更新完成后使用。
     */
    fun loadData(equipmentMap: Map<Int, Equipment>) {
        if (charaList.isNullOrEmpty()) {
            thread(start = true) {
                val innerCharaList = mutableListOf<Chara>()
                loadBasic(innerCharaList)
                innerCharaList.forEach {
                    setCharaMaxData(it)
                    setCharaRarity(it)
                    setCharaStoryStatus(it)
                    setCharaPromotionStatus(it)
                    setCharaEquipments(it, equipmentMap)
                    setUniqueEquipment(it)
                    setUnitSkillData(it)
                    setUnitAttackPattern(it)
                    it.setCharaProperty()
                }
                charaList.addAll(innerCharaList)
                callBack?.charaLoadFinished()
            }
        }
    }

    private fun loadBasic(innerCharaList: MutableList<Chara>) {
        get().getCharaBase()?.forEach {
            val chara = Chara()
            it.setCharaBasic(chara)
            innerCharaList.add(chara)
        }
    }

    private fun setCharaMaxData(chara: Chara) {
        this.maxCharaLevel = get().maxCharaLevel - 1
        chara.maxCharaLevel = this.maxCharaLevel
        this.maxCharaRank = get().maxCharaRank
        chara.maxCharaRank = this.maxCharaRank
        this.maxUniqueEquipmentLevel = get().maxUniqueEquipmentLevel
        chara.maxUniqueEquipmentLevel = this.maxUniqueEquipmentLevel

        maxEnemyLevel = get().maxEnemyLevel
    }

    private fun setCharaRarity(chara: Chara) {
        get().getUnitRarityList(chara.unitId)?.forEach {
            if (it.rarity == 6) {
                chara.maxRarity = 6
                chara.rarity = 6
                chara.iconUrl = Statics.ICON_URL.format(chara.prefabId + 60)
                chara.imageUrl = Statics.IMAGE_URL.format(chara.prefabId + 60)
            }
            chara.rarityProperty[it.rarity] = it.property
            chara.rarityPropertyGrowth[it.rarity] = it.propertyGrowth
        }
    }

    private fun setCharaStoryStatus(chara: Chara) {
        chara.storyProperty = Property().apply {
            get().getCharaStoryStatus(chara.charaId)?.forEach {
                this.plusEqual(it.getCharaStoryStatus(chara))
            }
        }
    }

    private fun setCharaPromotionStatus(chara: Chara) {
        val promotionStatus = mutableMapOf<Int, Property>()
        get().getCharaPromotionStatus(chara.unitId)?.forEach {
            promotionStatus[it.promotion_level] = it.promotionStatus
        }
        chara.promotionStatus = promotionStatus
    }

    private fun setCharaEquipments(chara: Chara, equipmentMap: Map<Int, Equipment>) {
        val rankEquipments = mutableMapOf<Int, List<Equipment>>()
        get().getCharaPromotion(chara.unitId)?.forEach { slots ->
            val equipmentList = mutableListOf<Equipment>()
            slots.charaSlots.forEach { id ->
                equipmentMap[id]?.let {
                    equipmentList.add(it)
                }
            }
            rankEquipments[slots.promotion_level] = equipmentList
        }
        chara.rankEquipments = rankEquipments
    }

    private fun setUniqueEquipment(chara: Chara) {
        chara.uniqueEquipment = MasterUniqueEquipment().getCharaUniqueEquipment(chara)
    }

    private fun setUnitSkillData(chara: Chara) {
        get().getUnitSkillData(chara.unitId)?.setCharaSkillList(chara)
    }

    private fun setUnitAttackPattern(chara: Chara) {
        get().getUnitAttackPattern(chara.unitId)?.forEach {
            chara.attackPatternList.add(
                it.attackPattern.setItems(
                    chara.skills,
                    chara.atkType
                )
            )
        }
    }

    fun mSetSelectedChara(chara: Chara?){
        chara?.apply {
            skills.forEach {
                it.setActionDescriptions(chara.maxCharaLevel, chara.charaProperty)
            }
        }
        this.selectedChara = chara
    }

    var callBack: MasterCharaCallBack? = null
    interface MasterCharaCallBack {
        fun charaLoadFinished()
    }
}