package com.sunrise.wiki;

import com.alibaba.fastjson.JSON;
import com.sunrise.wiki.common.Commands;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.db.*;
import com.sunrise.wiki.messages.CharaMessageHelper;
import com.sunrise.wiki.messages.impls.CharaMessageHelperImpl;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

class PluginMain extends PluginBase {

    private Config setting;
    private String location;
    private Map<String, List<String>> charaNameMap;
    private boolean autoUpdate;
    private boolean isReady = false;
    private DBDownloader dbDownloader;
    private EquipmentStarter equipmentStarter;
    private CharaMessageHelper charaMessageHelper;

    public void onLoad() {

        this.setting = loadConfig("setting.yml");
        this.setting.setIfAbsent("location", "CN");
        this.setting.setIfAbsent("autoUpdate", true);
        this.setting.setIfAbsent("clanBattle", false);

        this.location = this.setting.getString("location");
        this.autoUpdate = this.setting.getBoolean("autoUpdate");

        this.equipmentStarter = new EquipmentStarter();
        this.charaMessageHelper = new CharaMessageHelperImpl(getDataFolder().getAbsolutePath(),equipmentStarter);

        if ("JP".equals(this.location)) {
            Statics.DB_FILE_URL = Statics.DB_FILE_URL_JP;
            Statics.LATEST_VERSION_URL = Statics.LATEST_VERSION_URL_JP;
            Statics.DB_FILE_NAME_COMPRESSED = Statics.DB_FILE_NAME_COMPRESSED_JP;
            Statics.DB_FILE_NAME = Statics.DB_FILE_NAME_JP;
            Statics.setUserLoc("JP");
        } else if ("CN".equals(this.location)) {
            Statics.DB_FILE_URL = Statics.DB_FILE_URL_CN;
            Statics.LATEST_VERSION_URL = Statics.LATEST_VERSION_URL_CN;
            Statics.DB_FILE_NAME_COMPRESSED = Statics.DB_FILE_NAME_COMPRESSED_CN;
            Statics.DB_FILE_NAME = Statics.DB_FILE_NAME_CN;
            Statics.setUserLoc("CN");
        }
        //一定要在加载完配置之后再初始化此类
        dbDownloader = new DBDownloader(getDataFolder().getPath(), out -> {
            getLogger().info(out);
        });
        dbDownloader.setCallback(() -> {
            equipmentStarter.loadData();
            equipmentStarter.setCallBack(() -> {
                isReady = true;
                getLogger().info("装备数据加载完成");
            });
        });
        Statics.setDbFilePath(getDataFolder().getPath() + File.separator + Statics.DB_FILE_NAME);

        //如果用户设置了自动升级，则每隔24小时检查一次版本，否则只在加载插件时运行一次
        if (autoUpdate) {
            getScheduler().repeat(() -> {
                dbDownloader.checkDBVersion();
            }, 1000 * 60 * 60 * 24);
        } else {
            getScheduler().async(() -> {
                dbDownloader.checkDBVersion();
            });
        }

        //读取花名册，需提前配置到资源文件夹，花名册由来详见README
        File nicknameFile = new File(getDataFolder() + File.separator + "_pcr_data.json");
        try {
            charaNameMap = new HashMap<>();
            charaNameMap = JSON.parseObject(new FileInputStream(nicknameFile), charaNameMap.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }

        getLogger().info("Plugin loaded!");
    }

    public void onEnable() {
        this.getEventListener().subscribeAlways(GroupMessageEvent.class, (GroupMessageEvent event) -> {
//            [mirai:source:2523,-1507788872][mirai:at:2928703159,@testbot] 查询角色 水吃
            String commandStr = "";
//            System.out.println(event.getMessage().toString());
            if (event.getMessage().toString().contains("at:" + event.getBot().getId())) {
                try {
                    commandStr = event.getMessage().get(2).contentToString().trim();
                } catch (IndexOutOfBoundsException e) {
                    commandStr = "";
                }
            }
            getLogger().info("commandStr:" + commandStr);
            Matcher searchCharaPrfMatcher = Commands.searchCharaPrf.matcher(commandStr);
            Matcher searchCharaDetailMatcher = Commands.searchCharaDetail.matcher(commandStr);
            Matcher searchCharaSkillMatcher = Commands.searchCharaSkill.matcher(commandStr);
            if (searchCharaPrfMatcher.find()) {
                if(!checkEnable(event)){
                    return;
                }
                String charaName = searchCharaPrfMatcher.group("name").trim();
                if ("".equals(charaName)) {
                    event.getGroup().sendMessage("请输入角色名");
                    return;
                }
                int charaId = getIdByName(charaName);
                if (charaId == 100001) {
                    At at = new At(event.getSender());
                    event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
                } else {
                    event.getGroup().sendMessage(charaMessageHelper.getCharaInfo(charaId,event));
                }
            }
            if (searchCharaDetailMatcher.find()) {
                if(!checkEnable(event)){
                    return;
                }
                String charaName = searchCharaDetailMatcher.group("name").trim();
                if ("".equals(charaName)) {
                    event.getGroup().sendMessage("请输入角色名");
                    return;
                }
                int charaId = getIdByName(charaName);
                if (charaId == 100001) {
                    At at = new At(event.getSender());
                    event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
                } else {
                    event.getGroup().sendMessage(charaMessageHelper.getCharaDetails(charaId,event));
                }
            }
            if (searchCharaSkillMatcher.find()) {
                if(!checkEnable(event)){
                    return;
                }
                String charaName = "";
                int lv = 0;
                int rank = 0;
                if (null == searchCharaSkillMatcher.group("name1") && null == searchCharaSkillMatcher.group("name2")) {
                    event.getGroup().sendMessage("请输入正确的指令");
                    return;
                }
                if (null == searchCharaSkillMatcher.group("name1") && null != searchCharaSkillMatcher.group("name2")) {
                    charaName = searchCharaSkillMatcher.group("name2").trim();
                    if ("".equals(charaName)) {
                        event.getGroup().sendMessage("请输入角色名");
                        return;
                    }
                }
                if (null != searchCharaSkillMatcher.group("name1")) {
                    charaName = searchCharaSkillMatcher.group("name1").trim();
                    lv = Integer.parseInt(searchCharaSkillMatcher.group("lv").replace("l", "").trim());
                    rank = Integer.parseInt(searchCharaSkillMatcher.group("rank").replace("r", "").trim());
                }
                int charaId = getIdByName(charaName);
                if (charaId == 100001) {
                    At at = new At(event.getSender());
                    event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
                } else {
                    event.getGroup().sendMessage(charaMessageHelper.getCharaSkills(charaId,event));
                }
            }
        });
        getLogger().info("Plugin enabled!");
    }

    /**
     * 根据名称查询角色id
     *
     * @param name 名称
     * @return 角色id
     */
    public int getIdByName(String name) {
        Set<String> keySet = charaNameMap.keySet();
        //此处采用嵌套遍历来搜寻id，如果有更优的算法请大佬指出
        for (String key : keySet) {
            for (String s : charaNameMap.get(key)) {
                if (s.equals(name)) {
                    return Integer.parseInt(key + "01");
                }
            }
        }
        return 100001;
    }

    /**
     * 核验数据库文件是否准备完成
     *
     * @param event
     * @return
     */
    private boolean checkEnable(GroupMessageEvent event) {
        if (!isReady) {
            event.getGroup().sendMessage("数据库文件还未准备完成，请稍后再试！");
            return false;
        }
        return true;
    }

}