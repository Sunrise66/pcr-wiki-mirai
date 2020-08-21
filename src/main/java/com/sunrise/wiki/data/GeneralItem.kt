package com.sunrise.wiki.data

import com.sunrise.wiki.common.Statics

class GeneralItem(
    override val itemId: Int,
    override val itemName: String,
    override val itemType: ItemType
) : Item {
    override val iconUrl: String = Statics.ITEM_ICON_URL.format(itemId)
}