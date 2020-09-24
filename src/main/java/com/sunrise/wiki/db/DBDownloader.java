package com.sunrise.wiki.db;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sunrise.wiki.common.DBVersionInfo;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.utils.BrotliUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DBDownloader {
    private String basePath;
    private Long dbVersion;
    private boolean isReady = false;
    private String DB_VERSION_INFO_PATH;
    private String DB_FILE_COMPRESSED_PATH;
    private String DB_FILE_URL;
    private static DBDownloader instance;

    private DBDownloader(String basePath, LogOut logOutCallback) {
        this.basePath = basePath;
        this.out = logOutCallback;
        this.DB_VERSION_INFO_PATH = basePath + File.separator + "dbVersion.json";
        this.DB_FILE_COMPRESSED_PATH = basePath + File.separator + Statics.DB_FILE_NAME_COMPRESSED;
        this.DB_FILE_URL = Statics.DB_FILE_URL;
    }

    public static DBDownloader getInstance(String basePath, LogOut logOutCallback){
        if(instance==null){
            instance = new DBDownloader(basePath,logOutCallback);
        } return instance;
    }

    public boolean isReady() {
        return isReady;
    }

    /**
     * 检查数据库版本信息
     */
    public void checkDBVersion() {
        isReady = false;
        File DB_VERSION_INFO = new File(DB_VERSION_INFO_PATH);
        if (!DB_VERSION_INFO.exists()) {
            dbVersion = 0L;
            getDBVersion();
        } else {
            try {
                DBVersionInfo dbVersionInfo =JSON.parseObject(new FileInputStream(DB_VERSION_INFO), DBVersionInfo.class);
                dbVersion = dbVersionInfo.getTruthVersion();
                getDBVersion();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 下载数据库文件
     *
     * @return 是否下载完成
     */
    public boolean downloadDB() {
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
                out.out("数据库文件下载进度：" + totalDownload + "/" + maxLength);
                if (numRead <= 0) {
                    break;
                }
                outputStream.write(buf, 0, numRead);
            }
            inputStream.close();
            outputStream.close();
            BrotliUtils.deCompress(DB_FILE_COMPRESSED_PATH, true);
            isReady = true;
            out.out("数据库缓存文件下载成功！");
            finished = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return finished;
    }

    /**
     * 获取数据库版本信息及缓存文件
     */
    public void getDBVersion() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(Statics.LATEST_VERSION_URL).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String strRead = null;
            StringBuffer sbf = new StringBuffer();

            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            String lastVersionJson = sbf.toString();
            JSONObject object = JSON.parseObject(lastVersionJson);
//            JsonObject object = JsonParser.parseString(lastVersionJson).getAsJsonObject();
            Long serverVersion = object.getLong("TruthVersion");
            if (!dbVersion.equals(serverVersion)) {
                out.out("数据库不是最新版本，开始下载！");
                if (downloadDB()) {
                    File versionInfo = new File(DB_VERSION_INFO_PATH);
                    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(versionInfo), StandardCharsets.UTF_8));
                    bw.write(lastVersionJson);
                    bw.flush();
                    bw.close();
                    if (null != fcallback) {
                        this.fcallback.onFinish();
                    }
                }
            } else {
                isReady = true;
                out.out("数据库文件为最新，准备完毕！");
                if (null != fcallback) {
                    this.fcallback.onFinish();
                }
            }
        } catch (IOException e) {
            out.out(e.toString());
        }
    }

    private LogOut out;
    private FinishCallback fcallback;

    public void setCallback(FinishCallback callback) {
        this.fcallback = callback;
    }

    public interface FinishCallback {
        void onFinish();
    }

    public interface LogOut {
        void out(String out);
    }
}
