package com.sunrise.wiki.db

import com.sunrise.wiki.data.Quest
import com.sunrise.wiki.db.beans.MasterQuest
import kotlin.concurrent.thread

class QuestStarter {
    lateinit var questList : MutableList<Quest>
    var includeNormal = false
    var includeHard = false

    /***
     * 从数据库读取所有任务数据。
     */
    fun loadData() {
        if (questList.isNullOrEmpty()) {
            thread(start = true) {
                questList.addAll(MasterQuest().quest)
            }
        }
    }
}