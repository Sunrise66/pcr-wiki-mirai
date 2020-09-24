package com.sunrise.wiki

import com.alibaba.fastjson.JSON
import com.google.auto.service.AutoService
import com.sunrise.wiki.common.Orders
import com.sunrise.wiki.common.Statics
import com.sunrise.wiki.config.MainConfig
import com.sunrise.wiki.db.DBDownloader
import com.sunrise.wiki.db.DBDownloader.LogOut
import com.sunrise.wiki.db.EquipmentStarter
import com.sunrise.wiki.db.EquipmentStarter.MasterEquipmentCallBack
import com.sunrise.wiki.messages.CharaMessageHelper
import com.sunrise.wiki.messages.impls.CharaMessageHelperImpl
import kotlinx.coroutines.async

import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.fixedRateTimer
import kotlin.properties.Delegates

@AutoService(JvmPlugin::class)
object WikiMain : KotlinPlugin(JvmPluginDescription("com.sunrise.wiki", "0.2.0")) {
    lateinit var location: String
    lateinit var charaNameMap: Map<String, List<String>>
    var autoUpdate by Delegates.notNull<Boolean>()
    var isReady = false
    lateinit var dbDownloader: DBDownloader
    val equipmentStarter = EquipmentStarter()
    lateinit var charaMessageHelper: CharaMessageHelper
    var timer: Timer? = null

    override fun onEnable() {
        super.onEnable()
        MainConfig.reload()
        init()
        subscribeAlways<GroupMessageEvent>() {
            if (!checkEnable(it)) {
                return@subscribeAlways
            }
            var commandStr = ""
            if (it.message.toString().contains("at:" + it.bot.id, false)) {
                commandStr = try {
                    it.message[2].contentToString().trim()
                } catch (e: IndexOutOfBoundsException) {
                    ""
                }
            }
            logger.info("commandStr:$commandStr")
            //获取指令
            val searchCharaPrfMatcher = Orders.searchCharaPrf.matcher(commandStr)
            val searchCharaDetailMatcher = Orders.searchCharaDetail.matcher(commandStr)
            val searchCharaSkillMatcher = Orders.searchCharaSkill.matcher(commandStr)

            //指令匹配
            if (searchCharaPrfMatcher.find()) {
                val charaName = searchCharaPrfMatcher.group("name").trim()
                if ("" == charaName) {
                    it.group.sendMessage("请输入角色名")
                    return@subscribeAlways
                }
                val charaId = getIdByName(charaName)
                if (charaId == 100001) {
                    val at = At(it.sender)
                    it.group.sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"))
                } else {
                    it.group.sendMessage(charaMessageHelper.getCharaInfo(charaId, it))
                }
            }
            if (searchCharaDetailMatcher.find()) {
                val charaName = searchCharaDetailMatcher.group("name").trim()
                if ("" == charaName) {
                    it.group.sendMessage("请输入角色名")
                    return@subscribeAlways
                }
                val charaId = getIdByName(charaName)
                if (charaId == 100001) {
                    val at = At(it.sender)
                    it.group.sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"))
                } else {
                    it.group.sendMessage(charaMessageHelper.getCharaDetails(charaId, it))
                }
            }
            if (searchCharaSkillMatcher.find()) {
                var charaName = ""
                var lv = 0
                var rank = 0
                if (null == searchCharaSkillMatcher.group("name1") && null == searchCharaSkillMatcher.group("name2")) {
                    it.group.sendMessage("请输入正确的指令")
                    return@subscribeAlways
                }
                if (null == searchCharaSkillMatcher.group("name1") && null != searchCharaSkillMatcher.group("name2")) {
                    charaName = searchCharaSkillMatcher.group("name2").trim()
                    if ("".equals(charaName)) {
                        it.group.sendMessage("请输入角色名")
                        return@subscribeAlways
                    }
                }
                if (null != searchCharaSkillMatcher.group("name1")) {
                    charaName = searchCharaSkillMatcher.group("name1").trim()
                    lv = Integer.parseInt(searchCharaSkillMatcher.group("lv").replace("l", "").trim())
                    rank = Integer.parseInt(searchCharaSkillMatcher.group("rank").replace("r", "").trim())
                }
                val charaId = getIdByName(charaName)
                if (charaId == 100001) {
                    val at = At(it.sender)
                    it.group.sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"))
                } else {
                    it.group.sendMessage(charaMessageHelper.getCharaSkills(charaId, it))
                }
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
    }


    /**
     * 初始化
     */
    private fun init() {
        this.location = MainConfig.location
        this.autoUpdate = MainConfig.autoUpdate
        this.charaMessageHelper = CharaMessageHelperImpl(dataFolder.path, equipmentStarter)

        if ("JP" == this.location) {
            Statics.DB_FILE_URL = Statics.DB_FILE_URL_JP
            Statics.LATEST_VERSION_URL = Statics.LATEST_VERSION_URL_JP
            Statics.DB_FILE_NAME_COMPRESSED = Statics.DB_FILE_NAME_COMPRESSED_JP
            Statics.DB_FILE_NAME = Statics.DB_FILE_NAME_JP
            Statics.setUserLoc("JP")
        } else if ("CN" == location) {
            Statics.DB_FILE_URL = Statics.DB_FILE_URL_CN
            Statics.LATEST_VERSION_URL = Statics.LATEST_VERSION_URL_CN
            Statics.DB_FILE_NAME_COMPRESSED = Statics.DB_FILE_NAME_COMPRESSED_CN
            Statics.DB_FILE_NAME = Statics.DB_FILE_NAME_CN
            Statics.setUserLoc("CN")
        }
        //一定要在加载完配置之后再初始化此类
        this.dbDownloader = DBDownloader.getInstance(dataFolder.path, LogOut { out: String? -> logger.info(out) })
        dbDownloader.setCallback(DBDownloader.FinishCallback {
            equipmentStarter.loadData()
            equipmentStarter.callBack = object : MasterEquipmentCallBack {
                override fun equipmentLoadFinished() {
                    isReady = true
                    logger.info("装备数据加载完成")
                }
            }
        })

        Statics.setDbFilePath(dataFolder.path + File.separator + Statics.DB_FILE_NAME)

        //如果用户设置了自动升级，则每隔24小时检查一次版本，否则只在加载插件时运行一次
        if (autoUpdate) {
            timer?.cancel()
            timer = fixedRateTimer("", true, 0, 1000 * 60 * 60 * 24) {
                dbDownloader.checkDBVersion()
            }
        } else {
            async {
                timer?.cancel()
                dbDownloader.checkDBVersion()
            }
        }

        //读取花名册，需提前配置到资源文件夹，花名册由来详见README
        val nicknameFile = File(dataFolder.toString() + File.separator + "_pcr_data.json")
        try {
            charaNameMap = HashMap()
            charaNameMap = JSON.parseObject(FileInputStream(nicknameFile), charaNameMap.javaClass)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 根据名称查询角色id
     *
     * @param name 名称
     * @return 角色id
     */
    fun getIdByName(name: String): Int {
        val keys = charaNameMap.keys
        for (key in keys) {
            for (s in charaNameMap[key] ?: error("")) {
                if (s == name) {
                    return Integer.parseInt(key + "01")
                }
            }
        }
        return 100001
    }

    private suspend fun checkEnable(event: GroupMessageEvent): Boolean {
        if (!isReady) {
            event.group.sendMessage("数据库文件还未准备完成，请稍后再试！")
            return false
        }
        return true
    }
}