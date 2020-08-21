package com.sunrise.wiki.data

import com.sunrise.wiki.common.Statics
import com.sunrise.wiki.common.I18N

class RewardData(
    val rewardType: Int,
    val rewardId: Int,
    val rewardNum: Int,
    val odds: Int
) {
    val rewardIcon: String = Statics.EQUIPMENT_ICON_URL.format(rewardId)
    val oddsString: String = I18N.getString("percent_modifier").format(odds)

}