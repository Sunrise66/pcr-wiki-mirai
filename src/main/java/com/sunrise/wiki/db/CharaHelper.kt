package com.sunrise.wiki.db

import com.sunrise.wiki.common.Statics
import com.sunrise.wiki.data.Chara
import com.sunrise.wiki.data.Equipment
import com.sunrise.wiki.data.Property
import com.sunrise.wiki.db.beans.MasterUniqueEquipment

class CharaHelper {
    var maxCharaLevel: Int = 0
    var maxCharaRank: Int = 0
    var maxUniqueEquipmentLevel: Int = 0
    var maxEnemyLevel: Int = 0

    fun getFinalChara(unitid:Int,equipmentMap: Map<Int, Equipment>):Chara{
        val chara = Chara()
        chara.unitId = unitid
        loadBasic(chara)
        setCharaMaxData(chara)
        setCharaRarity(chara)
        setCharaStoryStatus(chara)
        setCharaPromotionStatus(chara)
        setCharaEquipments(chara, equipmentMap)
        setUniqueEquipment(chara)
        setUnitSkillData(chara)
        setUnitAttackPattern(chara)
        chara.setCharaProperty()
        chara?.apply {
            skills.forEach {
                it.setActionDescriptions(chara.maxCharaLevel, chara.charaProperty)
            }
        }
        return chara
    }

    private fun loadBasic(chara: Chara){
        val rawUnitBasic = DBHelper.get().getCharaInfo(chara.unitId)
        rawUnitBasic?.setCharaBasic(chara)
    }

    private fun setCharaMaxData(chara: Chara) {
        this.maxCharaLevel = DBHelper.get().maxCharaLevel - 1
        chara.maxCharaLevel = this.maxCharaLevel
        this.maxCharaRank = DBHelper.get().maxCharaRank
        chara.maxCharaRank = this.maxCharaRank
        this.maxUniqueEquipmentLevel = DBHelper.get().maxUniqueEquipmentLevel
        chara.maxUniqueEquipmentLevel = this.maxUniqueEquipmentLevel

        maxEnemyLevel = DBHelper.get().maxEnemyLevel
    }

    private fun setCharaRarity(chara: Chara) {
        DBHelper.get().getUnitRarityList(chara.unitId)?.forEach {
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
            DBHelper.get().getCharaStoryStatus(chara.charaId)?.forEach {
                this.plusEqual(it.getCharaStoryStatus(chara))
            }
        }
    }

    private fun setCharaPromotionStatus(chara: Chara) {
        val promotionStatus = mutableMapOf<Int, Property>()
        DBHelper.get().getCharaPromotionStatus(chara.unitId)?.forEach {
            promotionStatus[it.promotion_level] = it.promotionStatus
        }
        chara.promotionStatus = promotionStatus
    }

    private fun setCharaEquipments(chara: Chara, equipmentMap: Map<Int, Equipment>) {
        val rankEquipments = mutableMapOf<Int, List<Equipment>>()
        DBHelper.get().getCharaPromotion(chara.unitId)?.forEach { slots ->
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
        if ("JP" == Statics.USER_LOC) {
            DBHelper.get().getUnitSkillData(chara.unitId)?.setCharaSkillList(chara)
        }else if("CN" == Statics.USER_LOC){
            DBHelper.get().getCNUnitSkillData(chara.unitId)?.setCharaSkillList(chara)
        }
    }

    private fun setUnitAttackPattern(chara: Chara) {
        DBHelper.get().getUnitAttackPattern(chara.unitId)?.forEach {
            chara.attackPatternList.add(
                it.attackPattern.setItems(
                    chara.skills,
                    chara.atkType
                )
            )
        }
    }

    var callBack: MasterCharaCallBack? = null

    interface MasterCharaCallBack {
        fun charaLoadFinished()
    }
}