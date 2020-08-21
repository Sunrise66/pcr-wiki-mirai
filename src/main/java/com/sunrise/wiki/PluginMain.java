package com.sunrise.wiki;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.https.MyCallBack;
import com.sunrise.wiki.https.MyOKHttp;
import com.sunrise.wiki.utils.BrotliUtils;
import kotlinx.coroutines.Job;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.GroupMessage;
import net.mamoe.mirai.message.GroupMessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;
import okhttp3.Call;
import okhttp3.Response;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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

        Statics.setDbFilePath(getDataFolder().getPath()+"\\" + this.DB_FILE_NAME);

        //如果用户设置了自动升级，则每隔24小时检查一次版本，否则只在加载插件时运行一次
        if (autoUpdate) {
            Objects.requireNonNull(getScheduler()).repeat(this::checkDBVersion,1000*60*60*24);
        }else {
            Objects.requireNonNull(getScheduler()).async(this::checkDBVersion);
        }

        getLogger().info("Plugin loaded!");
    }

    public void onEnable() {

        getLogger().info("Plugin enabled!");
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