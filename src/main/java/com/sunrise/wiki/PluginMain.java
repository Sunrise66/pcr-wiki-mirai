package com.sunrise.wiki;

import com.alibaba.fastjson.JSON;
import com.sunrise.wiki.common.Commands;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.data.AttackPattern;
import com.sunrise.wiki.data.Chara;
import com.sunrise.wiki.data.Property;
import com.sunrise.wiki.data.Skill;
import com.sunrise.wiki.db.*;
import com.sunrise.wiki.res.values.StringsCN;
import com.sunrise.wiki.utils.ImageUtils;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.message.data.Image;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

import static com.sunrise.wiki.utils.ImageUtils.getIconWithPng;
import static com.sunrise.wiki.utils.ImageUtils.mergeSkillImages;

class PluginMain extends PluginBase {

    private Config setting;
    private String location;
    private Map<String, List<String>> charaNameMap;
    private boolean autoUpdate;
    private boolean clanBattle;
    private boolean isReady = false;
    private List<Chara> charaList;
    private DBDownloader dbDownloader;
    private CharaStarter charaStarter;
    private ClanBattleStarter clanBattleStarter;
    private EquipmentStarter equipmentStarter;
    private QuestStarter questStarter;

    public void onLoad() {
        this.setting = loadConfig("setting.yml");
        this.setting.setIfAbsent("location", "CN");
        this.setting.setIfAbsent("autoUpdate", true);
        this.setting.setIfAbsent("clanBattle", false);

        this.location = this.setting.getString("location");
        this.autoUpdate = this.setting.getBoolean("autoUpdate");
        this.clanBattle = this.setting.getBoolean("clanBattle");

        this.charaStarter = new CharaStarter();
        this.equipmentStarter = new EquipmentStarter();
        this.clanBattleStarter = new ClanBattleStarter();
        this.questStarter = new QuestStarter();

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
            equipmentStarter.setCallBack(() -> charaStarter.loadData(equipmentStarter.getEquipmentMap()));
            charaStarter.setCallBack(() -> {
                charaList = charaStarter.getCharaList();
                getLogger().info("角色数据加载完毕");
                isReady = true;
                if (clanBattle) {
                    getScheduler().delay(() -> {
                        clanBattleStarter.loadData();
                    }, 5000);
                } else {
                    questStarter.loadData();
                }
            });
            if (clanBattle) {
                clanBattleStarter.setCallBack(() -> {
                    getLogger().info("会战数据加载完成");
                    questStarter.loadData();
                });
            }
        });
        Statics.setDbFilePath(getDataFolder().getPath() + "\\" + Statics.DB_FILE_NAME);

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
        File nicknameFile = new File(getDataFolder() + "\\" + "_pcr_data.json");
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
            String nick = event.getBot().getNick();
            String commandStr = "";
            if (event.getMessage().get(1).contentToString().contains("@" + nick)) {
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
                String charaName = searchCharaPrfMatcher.group("name").trim();
                if ("".equals(charaName)) {
                    event.getGroup().sendMessage("请输入角色名");
                    return;
                }
                int charaId = getIdByName(charaName);
                if (charaId == 100001) {
                    event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                } else {
                    getCharaInfo(charaId, event);
                }
            }
            if (searchCharaDetailMatcher.find()) {
                String charaName = searchCharaDetailMatcher.group("name").trim();
                if ("".equals(charaName)) {
                    event.getGroup().sendMessage("请输入角色名");
                    return;
                }
                int charaId = getIdByName(charaName);
                if (charaId == 100001) {
                    event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                } else {
                    getCharaDetails(charaId, event);
                }
            }
            if (searchCharaSkillMatcher.find()) {
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
                    event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                } else {
                    getCharaSkills(charaId, event);
                }
            }
        });
        getLogger().info("Plugin enabled!");
    }

    /*************************指令方法区*************************************/

    public void getCharaDetails(int charaId, GroupMessageEvent event) {
        if (!checkEnable(event)) {
            return;
        } else {
            event.getGroup().sendMessage("正在查询...");
        }
        Chara chara = null;
        for (Chara it :
                charaList) {
            if (charaId == it.getUnitId()) {
                chara = it;
                break;
            }
        }
        charaStarter.mSetSelectedChara(chara);
        At at = new At(event.getSender());
        if (null == chara) {
            event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
            return;
        }
        Image charaIcon = getCharaIcon(charaId, event);
        Property charaProperty = chara.getCharaProperty();
        List<Message> messages = new ArrayList<>();
        messages.add(at);
        messages.add(charaIcon);
        messages.add(new PlainText(chara.getUnitName() + "\n"));
        messages.add(new PlainText("角色状态:-->\n"));
        messages.add(new PlainText("rank：" + chara.getMaxCharaRank() + "  " + "Level：" + chara.getMaxCharaLevel() + "\n"));
        messages.add(new PlainText("======================\n"));
        messages.add(new PlainText(String.format(StringsCN.text_normal_attack_cast_time, chara.getNormalAtkCastTime()) + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_atk + " " + charaProperty.getAtk() + "\n" + StringsCN.text_magical_atk + " " + charaProperty.getMagicStr() + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_crt + " " + charaProperty.getPhysicalCritical() + "\n" + StringsCN.text_magical_crt + " " + charaProperty.getMagicCritical() + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_def + " " + charaProperty.getDef() + "\n" + StringsCN.text_magical_def + " " + charaProperty.getMagicDef() + "\n"));
        messages.add(new PlainText(StringsCN.text_hp + " " + charaProperty.getHp() + "\n" + StringsCN.text_life_steal + " " + charaProperty.getLifeSteal() + "\n"));
        messages.add(new PlainText(StringsCN.text_energy_recovery + " " + charaProperty.getEnergyRecoveryRate() + "\n" + StringsCN.text_energy_reduce + " " + charaProperty.getEnergyReduceRate() + "\n"));
        messages.add(new PlainText(StringsCN.text_accuracy + " " + charaProperty.getAccuracy() + "\n" + StringsCN.text_dodge + " " + charaProperty.getDodge() + "\n"));
        messages.add(new PlainText(StringsCN.text_wave_hp_recovery + " " + charaProperty.getWaveHpRecovery() + "\n" + StringsCN.text_wave_energy_recovery + " " + charaProperty.getWaveEnergyRecovery() + "\n"));
        messages.add(new PlainText(StringsCN.text_physical_penetrate + " " + charaProperty.getPhysicalPenetrate() + "\n" + StringsCN.text_magical_penetrate + " " + charaProperty.getMagicPenetrate() + "\n"));
        messages.add(new PlainText(StringsCN.text_hp_recovery + " " + charaProperty.getHpRecovery() + "\n"));
        messages.add(new PlainText("======================\n"));

        event.getGroup().sendMessage(MessageUtils.newChain(messages));
    }

    /**
     * 获取角色信息
     *
     * @param charaId 角色id
     * @param event
     */
    public void getCharaInfo(int charaId, GroupMessageEvent event) {
        if (!checkEnable(event)) {
            return;
        } else {
            event.getGroup().sendMessage("正在查询...");
        }
        Chara chara = null;
        for (Chara it :
                charaList) {
            if (charaId == it.getUnitId()) {
                chara = it;
                break;
            }
        }
        charaStarter.mSetSelectedChara(chara);
        At at = new At(event.getSender());
        StringBuffer sb = new StringBuffer();
        if (null == chara) {
            event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
            return;
        }
        Image image = getCharaIcon(chara.getPrefabId(), event);
        sb.append(chara.getUnitName()).append("\n");
        sb.append("真名：").append(chara.getActualName()).append("\n");
        sb.append("声优：").append(chara.getVoice()).append("\n");
        sb.append("年龄：").append(chara.getAge()).append("岁").append("\n");
        sb.append("生日：").append(chara.getBirthDate()).append("\n");
        sb.append("身高：").append(chara.getHeight()).append(" cm").append("\n");
        sb.append("体重：").append(chara.getWeight()).append(" kg").append("\n");
        sb.append("血型：").append(chara.getBloodType()).append("型").append("\n");
        sb.append("喜好：").append(chara.getFavorite()).append("\n");
        sb.append("简介：").append(chara.getComment().replace("\\n", "\n")).append("\n\n");
        sb.append("发送 \"角色技能（空格）角色名\"来查询技能").append("\n");
        sb.append("发送 \"角色出招（空格）角色名\"来查询角色技能循环");

        event.getGroup().sendMessage(at.plus("\n").plus(image.plus("\n" + sb.toString())));

    }

    /**
     * 查询角色技能
     *
     * @param charaId 角色id
     * @param event
     */
    public void getCharaSkills(int charaId, GroupMessageEvent event) {
        if (!checkEnable(event)) {
            return;
        } else {
            event.getGroup().sendMessage("正在查询...");
        }
        Image charaIcon = getCharaIcon(charaId, event);
        Image ubIcon;
        Image s1Icon;
        Image s1pIcon;
        Image s2Icon;
        Image s2pIcon;
        Image exIcon;
        Image expIcon;
        At at = new At(event.getSender());
        //获取角色对象，以获得更多信息
        Chara chara = null;
        for (Chara it : charaList) {
            if (charaId == it.getUnitId()) {
                chara = it;
                break;
            }
        }
        charaStarter.mSetSelectedChara(chara);
        List<Message> messages = new ArrayList<>();
        messages.add(at);
        messages.add(charaIcon);
        messages.add(new PlainText("角色状态 -> \nRank：" + chara.getMaxCharaRank() + " Level：" + chara.getMaxCharaLevel() + "\n"));
        List<AttackPattern> attackPatternList = chara.getAttackPatternList();
        List<BufferedImage> loopImages = new ArrayList<>();
        int index = 0;
        for (AttackPattern pattern : attackPatternList) {
            for (AttackPattern.AttackPatternItem item : pattern.items) {
                BufferedImage icon = getLoopIcon(index, charaId, item.iconUrl);
                loopImages.add(icon);
                index++;
            }
        }
        index = 0;
        BufferedImage skillImages = mergeSkillImages(loopImages);
        if (null!=skillImages) {
            messages.add(event.getGroup().uploadImage(skillImages));
        }
        List<Skill> skills = chara.getSkills();
        for (Skill skill : skills) {
            if (skill.getSkillClass().getValue().equals(StringsCN.union_burst)) {
                ubIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(skill.getSkillClass().getValue() + "\n"));
                messages.add(ubIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M1")) {
                s1Icon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_1 + "\n"));
                messages.add(s1Icon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M1+")) {
                s1pIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_1_evo + "\n"));
                messages.add(s1pIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M2")) {
                s2Icon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_2 + "\n"));
                messages.add(s2Icon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("M2+")) {
                s2pIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.main_skill_2_evo + "\n"));
                messages.add(s2pIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("E1")) {
                exIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.ex_skill_1 + "\n"));
                messages.add(exIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
            if (skill.getSkillClass().getValue().equals("E1+")) {
                expIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(StringsCN.ex_skill_1_evo + "\n"));
                messages.add(expIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText() + "\n"));
                messages.add(new PlainText("技能效果：" + "\n"));
                messages.add(new PlainText(skill.getActionDescriptions().toString() + "\n"));
                messages.add(new PlainText("======================\n"));
            }
        }
        event.getGroup().sendMessage(MessageUtils.newChain(messages));
    }

    /**
     * 查询角色技能循环
     *
     * @param charaId 角色id
     * @param event
     */
    public void getCharaSkillRound(int charaId, GroupMessageEvent event) {
        if (!checkEnable(event)) {
            return;
        }
    }

    /******************************************************************/

    /**
     * 获取技能图标
     *
     * @param unitId  角色id
     * @param skillId 技能id
     * @param iconUrl 图标链接
     * @param event
     * @return
     */
    private Image getSkillIcon(int unitId, int skillId, String iconUrl, GroupMessageEvent event) {
        File skillIconPath = new File(getDataFolder() + "\\images\\skillIcons\\" + unitId);
        File png = new File(getDataFolder() + "\\images\\skillIcons\\" + unitId + "\\" + skillId + ".png");
        return getIconWithPng(iconUrl, skillIconPath, png, 0.5, event);
    }

    /**
     * 获取角色头像
     *
     * @param prefab_id 头像id
     * @param event
     * @return
     */
    private Image getCharaIcon(int prefab_id, GroupMessageEvent event) {
        File unitIconsPath = new File(getDataFolder() + "\\" + "images" + "\\" + "unitIcons");
        File png = new File(getDataFolder() + "\\" + "images" + "\\" + "unitIcons" + "\\" + prefab_id + 30 + ".png");
        String iconUrl = String.format(Locale.US, Statics.ICON_URL, prefab_id + 30);
        return getIconWithPng(iconUrl, unitIconsPath, png, 1, event);
    }

    private BufferedImage getLoopIcon(int index, int charaId, String iconUrl) {
        File loopIconPath = new File(getDataFolder() + "\\images\\loopIcons\\"+charaId);
        File png = new File(getDataFolder() + "\\images\\loopIcons\\" + charaId + "\\" + index + ".png");
        return getIconWithPng(iconUrl, loopIconPath, png, 0.5);
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