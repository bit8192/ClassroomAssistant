package cn.bincker.classroom.assistant;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Author：censhengde on 2024/11/25 14:14
 *
 * explain：<a href="https://www.volcengine.com/docs/6561/1354868">大模型录音文件识别API</a>
 */
@RunWith(AndroidJUnit4.class)
public class BigASRHttpDemo {

    private static final int STATUS_CODE_OK = 20000000;
    private static final int STATUS_CODE_PROCESSING = 20000001;
    private static final int STATUS_CODE_IN_TASK_QUEUE = 20000002;

    @Test
    public void test() {
        String url = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/submit";
        final String appId = BuildConfig.BYTEDANCE_APPPID;
        final String token = BuildConfig.BYTEDANCE_ACCESS_TOKEN;

        JsonObject user = new JsonObject();
        user.addProperty("uid", "fake_uid");

        JsonObject audio = new JsonObject();
        audio.addProperty("url", "https://file.upfile.live/uploads/20250623/506469294e109b51dffa2f3301b3eebb.wav");
        audio.addProperty("format", "mp3");
        audio.addProperty("codec", "raw");
        audio.addProperty("rate", 16000);
        audio.addProperty("bits", 16);
        audio.addProperty("channel", 1);

        JsonObject requestObj = new JsonObject();
        requestObj.addProperty("model_name", "bigmodel");
        // ......
        JsonObject jsonBody = new JsonObject();
        jsonBody.add("user", user);
        jsonBody.add("audio", audio);
        jsonBody.add("request", requestObj);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.SECONDS)
                .writeTimeout(100, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody.toString()))
                .addHeader("X-Api-App-Key", appId)
                .addHeader("X-Api-Access-Key", token)
                .addHeader("X-Api-Resource-Id", "volc.bigasr.auc")
                .addHeader("X-Api-Request-Id", UUID.randomUUID().toString())
                .addHeader("X-Api-Sequence", "-1")
                .build();
        String xTtLogid = "";
        try {
            final Response response = okHttpClient.newCall(request).execute();
            xTtLogid = response.header("X-Tt-Logid", "");
            String logId = response.header("X-Tt-Logid");
            int statusCode = Integer.parseInt(response.header("X-Api-Status-Code","-1"));
            String statusMsg = response.header("X-Api-Message","");
            Log.d("BigASRHttpDemo.test",
                    "===>Submit response: X-Tt-Logid:" + xTtLogid + " X-Api-Status-Code:" + statusCode + " X-Api-Message:"
                            + statusMsg);
            if (statusCode != STATUS_CODE_OK) {
                return;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // query
        url = "https://openspeech.bytedance.com/api/v3/auc/bigmodel/query";
        request = request.newBuilder().url(url).post(RequestBody.create(MediaType.parse("application/json"), "{}"))
                .addHeader("X-Api-App-Key", appId)
                .addHeader("X-Api-Access-Key", token)
                .addHeader("X-Api-Resource-Id", "volc.bigasr.auc")
                .addHeader("X-Tt-Logid", xTtLogid)  // 固定传递 x-tt-logid
                .build();
        try {
            while (true) {
                // 间隔一定时间再查询。
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                final Response response = okHttpClient.newCall(request).execute();
                String logId = response.header("X-Tt-Logid");
                int statusCode = Integer.parseInt(response.header("X-Api-Status-Code"));
                String statusMsg = response.header("X-Api-Message");
                Log.d("BigASRHttpDemo.test",
                        "===> Query response: X-Tt-Logid:" + logId + " X-Api-Status-Code:" + statusCode
                                + " X-Api-Message:"
                                + statusMsg);

                // 识别完成
                if (statusCode == STATUS_CODE_OK) {
                    String responseBody = response.body().string();
                    Log.d("BigASRHttpDemo.test","===>Query successful:" + responseBody);
                    break;
                }
                // error
                else if (statusCode != STATUS_CODE_PROCESSING && statusCode != STATUS_CODE_IN_TASK_QUEUE) {
                    Log.d("BigASRHttpDemo.test","===>Query error:" + statusMsg);
                    break;
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getHttpLocation() throws IOException {
        var request = new Request.Builder().url("https://upfile.live/download/03379c37").build();
        try(var response = new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build().newCall(request).execute()){
            Log.d("BigASRHttpDemo.getHttpLocation", response.header("location", ""));
        }
    }

}
