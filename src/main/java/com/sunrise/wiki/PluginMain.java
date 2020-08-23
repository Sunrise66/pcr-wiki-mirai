package com.sunrise.wiki;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.luciad.imageio.webp.WebPReadParam;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.db.DBHelper;
import com.sunrise.wiki.db.beans.RawSkillAction;
import com.sunrise.wiki.db.beans.RawSkillData;
import com.sunrise.wiki.db.beans.RawUnitBasic;
import com.sunrise.wiki.db.beans.RawUnitSkillData;
import com.sunrise.wiki.https.MyCallBack;
import com.sunrise.wiki.https.MyOKHttp;
import com.sunrise.wiki.utils.BrotliUtils;
import kotlinx.coroutines.Job;
import kotlinx.serialization.json.JsonArray;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.GroupMessage;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;
import okhttp3.Call;
import okhttp3.Response;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
            String message = event.getMessage().toString();
            if (message.contains("查询角色")) {
                getLogger().info(message);
//                System.out.println(message.substring(message.indexOf("查询角色")+4));
                String charaName = message.substring(message.indexOf("查询角色") + 4).toLowerCase().trim();
                if (null == charaName || "".equals(charaName)) {
                    event.getGroup().sendMessage("请输入\"查询角色（空格）角色名\"进行查询");
                } else {
                    int charaId = getIdByName(charaName);
                    if (charaId == 100001) {
                        event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                    } else {
                        getCharaInfo(charaId, event);
                    }
                }
            } else if (message.contains("角色技能")) {
                getLogger().info(message);
                String charaName = message.substring(message.indexOf("角色技能") + 4).toLowerCase().trim();
                if (null == charaName || "".equals(charaName)) {
                    event.getGroup().sendMessage("请输入\"角色技能（空格）角色名\"进行查询");
                } else {
                    int charaId = getIdByName(charaName);
                    if (charaId == 100001) {
                        event.getGroup().sendMessage("不知道您要查找的角色是谁呢？可能是未实装角色哦~");
                    } else {
                        getCharaSkills(charaId, event);
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
        File imgPath = new File(getDataFolder() + "\\" + "images"+"\\"+"unitIcons");
        if (!imgPath.exists()) {
            imgPath.mkdir();
        }
        if (null == info) {
            event.getGroup().sendMessage(at.plus("\n").plus("不知道您要查找的角色是谁呢？可能是未实装角色哦~"));
            return;
        }
//        File webp = new File(getDataFolder() + "\\" + "images" + "\\" + info.prefab_id + 30 + ".webp");
        File png = new File(getDataFolder() + "\\" + "images" + "\\" +"unitIcons"+"\\"+ info.prefab_id + 30 + ".png");

        String iconUrl = String.format(Locale.US, Statics.ICON_URL, info.prefab_id + 30);
        try {
//            Image image = event.getGroup().uploadImage(new URL("https://i.loli.net/2020/08/22/6GtQMbBghDJEirN.png"));
            Image image;
            if (!png.exists()) {
                HttpURLConnection conn = (HttpURLConnection) new URL(iconUrl).openConnection();
                InputStream inputStream = conn.getInputStream();
                BufferedImage bufferedImage = ImageIO.read(inputStream);
                ImageIO.write(bufferedImage, "png", png);
                inputStream.close();
            }
            image = event.getGroup().uploadImage(png);
            sb.append(info.unit_name).append("\n");
            sb.append("真名：").append(info.actual_name).append("\n");
            sb.append("声优：").append(info.voice).append("\n");
            sb.append("年龄：").append(info.age).append("\n");
            sb.append("生日：").append(info.birth_month).append("月").append(info.birth_day).append("日").append("\n");
            sb.append("身高：").append(info.height).append(" cm").append("\n");
            sb.append("体重：").append(info.weight).append(" kg").append("\n");
            sb.append("血型：").append(info.blood_type).append("型").append("\n");
            sb.append("喜欢的东西：").append(info.favorite).append("\n\n");
//            sb.append("角色简介：").append(info.self_text).append("\n\n");
            sb.append("发送 \"角色技能（空格）角色名\"来查询技能").append("\n");
            sb.append("发送 \"角色出招（空格）角色名\"来查询角色技能循环");

            event.getGroup().sendMessage(at.plus("\n").plus(image.plus("\n" + sb.toString())));

        } catch (Exception e) {
            e.printStackTrace();
        }

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
        }
        At at = new At(event.getSender());
        StringBuffer sb = new StringBuffer();
        DBHelper helper = DBHelper.get();
        RawUnitSkillData unitSkills = helper.getUnitSkillData(charaId);
        if(null == unitSkills){
            event.getGroup().sendMessage(at.plus("\n").plus("您查询的角色可能没有实装哦~"));
            return;
        }
        RawSkillData ex_skill_1 = helper.getSkillData(unitSkills.ex_skill_1);
        File ex_skill_1_icon = new File(getDataFolder()+"\\"+"images"+"\\"+"skillIcons"+"\\"+charaId+"\\"+ex_skill_1+".png");
        sb.append("技能1：").append("\n");



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