package com.sunrise.wiki.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object MainConfig : AutoSavePluginConfig("主配置") {
    var location by value("CN")
    var autoUpdate by value(true)
    var clanBattle by value(false)
}