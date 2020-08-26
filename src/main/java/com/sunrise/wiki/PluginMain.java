package com.sunrise.wiki;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.sunrise.wiki.common.Commands;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.data.Chara;
import com.sunrise.wiki.data.Property;
import com.sunrise.wiki.data.Skill;
import com.sunrise.wiki.data.action.ActionParameter;
import com.sunrise.wiki.data.action.ActionRaw;
import com.sunrise.wiki.db.*;
import com.sunrise.wiki.db.beans.*;
import com.sunrise.wiki.https.MyCallBack;
import com.sunrise.wiki.https.MyOKHttp;
import com.sunrise.wiki.res.values.StringsCN;
import com.sunrise.wiki.utils.BrotliUtils;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;
import okhttp3.Call;
import okhttp3.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PluginMain extends PluginBase {

    private Config setting;
    private String location;
    private Map<String, List<String>> charaNameMap;
    private Long dbVersion;
    private boolean autoUpdate;
    private boolean isReady = false;
    private List<Chara> charaList;
    private DBDownloader dbDownloader;
    private CharaStarter charaStarter;
    private ClanBattleStarter clanBattleStarter;
    private EquipmentStarter equipmentStarter;
    private QuestStarter questStarter;

    public void onLoad() {
        this.setting = loadConfig("setting.yml");
        this.setting.setIfAbsent("location", "JP");
        this.setting.setIfAbsent("autoUpdate", true);

        this.location = this.setting.getString("location");
        this.autoUpdate = this.setting.getBoolean("autoUpdate");

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

        dbDownloader.setCallback(()->{
            equipmentStarter.loadData();
            equipmentStarter.setCallBack(() -> charaStarter.loadData(equipmentStarter.getEquipmentMap()));
            charaStarter.setCallBack(() -> {
                charaList = charaStarter.getCharaList();
                getLogger().info("角色数据加载完毕");
                isReady = true;
            });
//            clanBattleStarter.loadData();
//            questStarter.loadData();
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
            String commandStr = event.getMessage().contentToString();
            if (commandStr.matches(Commands.searchCharaCmd)) {
                //根据正则获取角色名
                Pattern pattern = Pattern.compile("\\s.{0,10}");
                Matcher matcher = pattern.matcher(commandStr);
                String charaName = "";
                if (matcher.find()) {
                    charaName = matcher.group().toLowerCase().trim();
                }
                int charaId = getIdByName(charaName);
                if (charaId == 100001) {
                    event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                } else {
                    getCharaInfo(charaId, event);
                }
            } else if (Commands.searchCharaSkillPattern.matcher(commandStr).find()) {
                Matcher matcher = Commands.searchCharaSkillPattern.matcher(commandStr);
                String charaName = "";
                int lv = 0;
                int rank = 0;
                if (matcher.find()) {
                    System.out.println(matcher.group());
                    if (null == matcher.group("name1") && null == matcher.group("name2")) {
                        event.getGroup().sendMessage("请输入正确的指令");
                        return;
                    }
                    if (null == matcher.group("name1") && null != matcher.group("name2")) {
                        charaName = matcher.group("name2").trim();
                    }
                    if (null != matcher.group("name1")) {
                        charaName = matcher.group("name1").trim();
                        lv = Integer.parseInt(matcher.group("lv").trim());
                        rank = Integer.parseInt(matcher.group("rank").substring(4).trim());
                    }
                    int charaId = getIdByName(charaName);
                    if (charaId == 100001) {
                        event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                    } else {
                        getCharaSkills(charaId, lv, rank, event);
                    }
                }
            }
        });
        getLogger().info("Plugin enabled!");
    }

    /*************************指令方法区*************************************/

    /**
     * 获取角色信息
     *
     * @param charaId 角色id
     * @param event
     */
    public void getCharaInfo(int charaId, GroupMessageEvent event) {
        if (!checkEnable(event)) {
            return;
        }
        Chara chara = null;
        for (Chara it :
                charaList) {
            if (charaId == it.getUnitId()) {
                chara = it;
                break;
            }
        }
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
        sb.append("简介：").append(chara.getComment().replace("\\n","\n")).append("\n\n");
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
    public void getCharaSkills(int charaId, int lv, int rank, GroupMessageEvent event) {
        if (!checkEnable(event)) {
            return;
        }
        Image charaIcon = getCharaIcon(charaId, event);
        Image ubIcon;
        Image s1Icon;
        Image s2Icon;
        Image exIcon;
        At at = new At(event.getSender());
        //获取角色对象，以获得更多信息
        Chara chara = null;
        for (Chara it : charaList){
            if(charaId==it.getUnitId()){
                chara = it;
                break;
            }
        }
        charaStarter.mSetSelectedChara(chara);
        List<Message> messages = new ArrayList<>();
        messages.add(at);
        messages.add(charaIcon);
        List<Skill> skills = chara.getSkills();
        if (lv > chara.getMaxCharaLevel() || lv < 0) {
            event.getGroup().sendMessage("当前角色最大Lv为：" + chara.getMaxCharaLevel());
            return;
        }
        if (rank > chara.getMaxCharaRank() || rank < 0) {
            System.out.println(rank);
            event.getGroup().sendMessage("当前角色最大Rank为：" + chara.getMaxCharaRank());
            return;
        }
        for (Skill skill : skills) {
//            if (lv > 0 && rank > 0) {
//                chara.setMaxCharaRank(rank);
//                chara.setMaxCharaLevel(lv);
//            }
            if (skill.getSkillClass().getValue().equals(StringsCN.union_burst)) {
                ubIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(skill.getSkillClass().getValue() + "\n"));
                messages.add(ubIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText()+"\n"));
//                if (lv > 0 && rank > 0) {
                    messages.add(new PlainText(skill.getActionDescriptions().toString()));
//                }
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
        return getIconWithPng(iconUrl, skillIconPath, png, event);
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
        return getIconWithPng(iconUrl, unitIconsPath, png, event);
    }

    /**
     * 获取png格式的icon
     *
     * @param iconUrl  图片链接
     * @param savePath 图片存储目录
     * @param target   图片完整存储路径，包括文件名
     * @param event
     * @return
     */
    private Image getIconWithPng(String iconUrl, File savePath, File target, GroupMessageEvent event) {
        if (!savePath.exists()) {
            savePath.mkdirs();
        }
        Image image;
        try {
            if (!target.exists()) {
                HttpURLConnection conn = (HttpURLConnection) new URL(iconUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
                InputStream inputStream = conn.getInputStream();
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                ImageIO.write(bufferedImage, "png", target);
                inputStream.close();
            }
            image = event.getGroup().uploadImage(target);
            return image;
        } catch (Exception e) {
            getLogger().debug(e);
            return null;
        }
    }

    /**
     * 根据名称查询角色id
     *
     * @param name 名称
     * @return 角色id
     */
    public int getIdByName(String name) {
        Set<String> keySet = charaNameMap.keySet();
//        System.out.println(charaNameMap.toString());
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