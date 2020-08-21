package com.sunrise.wiki.db.beans

import com.sunrise.wiki.data.Quest
import com.sunrise.wiki.db.DBHelper
import java.util.*

class MasterQuest {
    val quest: MutableList<Quest>
        get() {
            val questList: MutableList<Quest> = ArrayList()
            DBHelper.get().getQuests()?.forEach {
                questList.add(it.quest)
            }
            return questList
        }
}