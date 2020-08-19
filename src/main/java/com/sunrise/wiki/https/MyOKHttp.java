package com.sunrise.wiki.https;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MyOKHttp {

    public static void doRequest(String url,MyCallBack callBack){
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callBack.onFailure(call,e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                callBack.onResponse(call,response);
            }
        });
    }

}
