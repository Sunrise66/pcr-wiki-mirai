package com.sunrise.wiki.db;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.sunrise.wiki.common.Statics;
import com.sunrise.wiki.https.MyCallBack;
import com.sunrise.wiki.https.MyOKHttp;
import com.sunrise.wiki.utils.BrotliUtils;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DBDownloader {
    private String basePath;
    private Long dbVersion;
    private boolean isReady = false;
    private String DB_VERSION_INFO_PATH;
    private String DB_FILE_COMPRESSED_PATH;
    private String DB_FILE_URL;

    public DBDownloader(String basePath,LogOut logOutCallback) {
        this.basePath = basePath;
        this.out = logOutCallback;
        this.DB_VERSION_INFO_PATH = basePath + "\\" + "dbVersion.json";
        this.DB_FILE_COMPRESSED_PATH = basePath + "\\" + Statics.DB_FILE_NAME_COMPRESSED;
        this.DB_FILE_URL = Statics.DB_FILE_URL;
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
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(Statics.LATEST_VERSION_URL).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                out.out(e.toString());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String lastVersionJson = Objects.requireNonNull(response.body()).string();
                JsonObject object = JsonParser.parseString(lastVersionJson).getAsJsonObject();
                Long serverVersion = object.get("TruthVersion").getAsLong();
                if (!dbVersion.equals(serverVersion)) {
                    out.out("数据库不是最新版本，开始下载！");
                    if (downloadDB()) {
                        File versionInfo = new File(DB_VERSION_INFO_PATH);
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(versionInfo), StandardCharsets.UTF_8));
                        bw.write(lastVersionJson);
                        bw.flush();
                        bw.close();
                    }
                } else {
                    isReady = true;
                    out.out("数据库文件为最新，准备完毕！");
                }
            }
        });
    }

    private void doFinish(){
        if(null!=fcallback){
            fcallback.onFinish();
        }
    }
    private LogOut out;
    public FinishCallback fcallback;

    public void setCallback(FinishCallback callback) {
        this.fcallback = callback;
    }

    public interface FinishCallback{
        void onFinish();
    }
    public interface LogOut{
        void out(String out);
    }
}
