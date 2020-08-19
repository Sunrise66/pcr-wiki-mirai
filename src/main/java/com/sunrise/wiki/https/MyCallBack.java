package com.sunrise.wiki.https;

import okhttp3.Call;
import okhttp3.Response;

import java.io.IOException;

public interface MyCallBack {
    void onFailure(Call call, IOException e);
    void onResponse(Call call, Response response)throws IOException;
}
