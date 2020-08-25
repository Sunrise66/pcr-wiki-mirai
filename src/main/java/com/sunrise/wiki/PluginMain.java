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
import com.sunrise.wiki.db.DBHelper;
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
    private String DB_FILE_URL;
    private String LATEST_VERSION_URL;
    private String location;
    private String DB_FILE_NAME_COMPRESSED;
    private String DB_FILE_NAME;
    private String DB_FILE_COMPRESSED_PATH;
    private String DB_VERSION_INFO_PATH;
    private Map<String, List<String>> charaNameMap;
    private Long dbVersion;
    private boolean autoUpdate;
    private boolean isReady = false;

    public void onLoad() {
        this.setting = loadConfig("setting.yml");
        this.setting.setIfAbsent("location", "CN");
        this.setting.setIfAbsent("autoUpdate", true);

        this.location = this.setting.getString("location");
        this.autoUpdate = this.setting.getBoolean("autoUpdate");
        if ("JP".equals(this.location)) {
            this.DB_FILE_URL = Statics.DB_FILE_URL_JP;
            this.LATEST_VERSION_URL = Statics.LATEST_VERSION_URL_JP;
            this.DB_FILE_NAME_COMPRESSED = Statics.DB_FILE_NAME_COMPRESSED_JP;
            this.DB_FILE_NAME = Statics.DB_FILE_NAME_JP;
            Statics.setUserLoc("JP");
        } else if ("CN".equals(this.location)) {
            this.DB_FILE_URL = Statics.DB_FILE_URL_CN;
            this.LATEST_VERSION_URL = Statics.LATEST_VERSION_URL_CN;
            this.DB_FILE_NAME_COMPRESSED = Statics.DB_FILE_NAME_COMPRESSED_CN;
            this.DB_FILE_NAME = Statics.DB_FILE_NAME_CN;
            Statics.setUserLoc("CN");
        }

        this.DB_FILE_COMPRESSED_PATH = getDataFolder().getPath() + "\\" + this.DB_FILE_NAME_COMPRESSED;
        this.DB_VERSION_INFO_PATH = getDataFolder().getPath() + "\\" + "dbVersion.json";

        Statics.setDbFilePath(getDataFolder().getPath() + "\\" + this.DB_FILE_NAME);

        //如果用户设置了自动升级，则每隔24小时检查一次版本，否则只在加载插件时运行一次
        if (autoUpdate) {
            Objects.requireNonNull(getScheduler()).repeat(this::checkDBVersion, 1000 * 60 * 60 * 24);
        } else {
            Objects.requireNonNull(getScheduler()).async(this::checkDBVersion);
        }

        //读取花名册
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
                //因指令已经匹配，故此处实际上不需要判断
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
                if(matcher.find()){
                    System.out.println(matcher.group());
                    if (null == matcher.group("name1") && null == matcher.group("name2")) {
                        event.getGroup().sendMessage("请输入正确的指令");
                        return;
                    }
                    if (null == matcher.group("name1") && null != matcher.group("name2")) {
                        charaName = matcher.group("name2").trim();
                    }
                    if(null!=matcher.group("name1")){
                        charaName = matcher.group("name1").trim();
                        lv = Integer.parseInt(matcher.group("lv").trim());
                        rank = Integer.parseInt(matcher.group("rank").substring(4).trim());
                    }
                    int charaId = getIdByName(charaName);
                    if (charaId == 100001) {
                        event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                    } else {
                        getCharaSkills(charaId,rank,lv,event);
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
        At at = new At(event.getSender());
        StringBuffer sb = new StringBuffer();
        DBHelper helper = DBHelper.get();
        RawUnitBasic info = helper.getCharaInfo(charaId);
        if (null == info) {
            event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
            return;
        }
        Image image = getCharaIcon(info.prefab_id, event);
        sb.append(info.unit_name).append("\n");
        sb.append("真名：").append(info.actual_name).append("\n");
        sb.append("声优：").append(info.voice).append("\n");
        sb.append("年龄：").append(info.age).append("岁").append("\n");
        sb.append("生日：").append(info.birth_month).append("月").append(info.birth_day).append("日").append("\n");
        sb.append("身高：").append(info.height).append(" cm").append("\n");
        sb.append("体重：").append(info.weight).append(" kg").append("\n");
        sb.append("血型：").append(info.blood_type).append("型").append("\n");
        sb.append("喜好：").append(info.favorite).append("\n");
        sb.append("简介：").append(info.comment.replace("\\n", "\n")).append("\n\n");
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
        StringBuffer sb = new StringBuffer();
        DBHelper helper = DBHelper.get();
        //获取角色对象，以获得更多信息
        Chara chara = new Chara();
        chara.setMaxRarity(5);
        RawUnitBasic charaInfo = helper.getCharaInfo(charaId);
        Property storyProperty = new Property();
        List<RawCharaStoryStatus> statusList = helper.getCharaStoryStatus(charaId);
        for(RawCharaStoryStatus states: statusList){
            storyProperty.plusEqual(states.getCharaStoryStatus(chara));
        }
        chara.setStoryProperty(storyProperty);
        Map<Integer,Property> promotionStatus = new HashMap<>();
        List<RawPromotionStatus> charaPromotionStatusList = helper.getCharaPromotionStatus(charaId);
        for(RawPromotionStatus  charaPromotionStatus:charaPromotionStatusList){
            promotionStatus.put(charaPromotionStatus.promotion_level,charaPromotionStatus.getPromotionStatus());
        }
        chara.promotionStatus = promotionStatus;



        charaInfo.setCharaBasic(chara);
        if ("CN".equals(Statics.USER_LOC)) {
            RawUnitSkillDataCN info = helper.getCNUnitSkillData(charaId);
            info.setCharaSkillList(chara);
        } else if ("JP".equals(Statics.USER_LOC)) {
            RawUnitSkillData info = helper.getUnitSkillData(charaId);
            info.setCharaSkillList(chara);
        }
        List<Message> messages = new ArrayList<>();
        messages.add(at);
        messages.add(charaIcon);
        List<Skill> skills = chara.getSkills();
//        if (lv > chara.getMaxCharaLevel() || lv < 0) {
//            event.getGroup().sendMessage("当前角色最大Lv为：" + chara.getMaxCharaLevel());
//            return;
//        }
//        if (rank > chara.getMaxCharaRank() || rank < 0) {
//            event.getGroup().sendMessage("当前角色最大Rank为：" + chara.getMaxCharaRank());
//            return;
//        }
        for (Skill skill : skills) {
            if (lv > 0 && rank > 0) {
                chara.setCharaProperty(chara.getMaxRarity(), rank, true);
                skill.setActionDescriptions(lv, chara.charaProperty);
            }
            if (skill.getSkillClass().getValue().equals(StringsCN.union_burst)) {
                ubIcon = getSkillIcon(charaId, skill.getSkillId(), skill.iconUrl, event);
                messages.add(new PlainText(skill.getSkillClass().getValue() + "\n"));
                messages.add(ubIcon);
                messages.add(new PlainText(skill.getSkillName() + "\n"));
                messages.add(new PlainText("技能描述：" + skill.getDescription() + "\n"));
                messages.add(new PlainText(skill.getCastTimeText()));
                if (lv > 0 && rank > 0) {
                    messages.add(new PlainText(skill.getActionDescriptions().toString()));
                }
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

    /**
     * 检查数据库版本信息
     */
    private void checkDBVersion() {
        isReady = false;
        File DB_VERSION_INFO = new File(DB_VERSION_INFO_PATH);
        if (!DB_VERSION_INFO.exists()) {
            dbVersion = 0L;
            getDBVersion();
        } else {
            try {
                JsonObject object = JsonParser.parseReader(new JsonReader(new InputStreamReader(new FileInputStream(DB_VERSION_INFO), StandardCharsets.UTF_8))).getAsJsonObject();
                dbVersion = object.get("TruthVersion").getAsLong();
                getDBVersion();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 下载数据库文件
     *
     * @return 是否下载完成
     */
    private boolean downloadDB() {
        boolean finished = false;
        try {
            File dbFileCompressed = new File(DB_FILE_COMPRESSED_PATH);
            HttpURLConnection conn = (HttpURLConnection) new URL(DB_FILE_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

            InputStream inputStream = conn.getInputStream();
            int maxLength = conn.getContentLength();
            if (dbFileCompressed.exists()) {
                dbFileCompressed.delete();
            }
            FileOutputStream outputStream = new FileOutputStream(dbFileCompressed);
            byte[] buf = new byte[1024 * 1024];
            int numRead;
            int totalDownload = 0;
            while (true) {
                numRead = inputStream.read(buf);
                totalDownload += numRead;
                getLogger().info("数据库文件下载进度：" + totalDownload + "/" + maxLength);
                if (numRead <= 0) {
                    break;
                }
                outputStream.write(buf, 0, numRead);
            }
            inputStream.close();
            outputStream.close();
            BrotliUtils.deCompress(DB_FILE_COMPRESSED_PATH, true);
            isReady = true;
            getLogger().info("数据库缓存文件下载成功！");
            finished = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return finished;
    }

    /**
     * 获取数据库版本信息及缓存文件
     */
    private void getDBVersion() {
        MyOKHttp.doRequest(LATEST_VERSION_URL, new MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                getLogger().info(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String lastVersionJson = Objects.requireNonNull(response.body()).string();
                JsonObject object = JsonParser.parseString(lastVersionJson).getAsJsonObject();
                Long serverVersion = object.get("TruthVersion").getAsLong();
                if (!dbVersion.equals(serverVersion)) {
                    getLogger().info("数据库不是最新版本，开始下载！");
                    if (downloadDB()) {
                        File versionInfo = new File(DB_VERSION_INFO_PATH);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(versionInfo), StandardCharsets.UTF_8));
                        bw.write(lastVersionJson);
                        bw.flush();
                        bw.close();
                    }
                } else {
                    isReady = true;
                    getLogger().info("数据库文件为最新，准备完毕！");
                }
            }
        });
    }

}