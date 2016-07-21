package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Administrator on 2016/6/29.
 */

public class HttpUtil {

    public static void sendHttpRequest(final String address, final HttpCallbackListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpURLConnection connection = null;
                try {

                    URL url = new URL(address);//获取url对象
                    connection = (HttpURLConnection) url.openConnection();//建立连接对象
                    connection.setRequestMethod("GET");//请求

                    connection.setConnectTimeout(8000);

                    connection.setReadTimeout(8000);

                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null){
                        response.append(line);
                    }

                    if (listener != null) {
                        //回调onFinish方法

                        listener.onFinish(response.toString());//回调HttpCallbackListener的onFinish方法
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        //回调onError方法
                        listener.onError(e);//回调HttpCallbackListener的onError方法
                    }

                } finally {
                    if (connection != null) {
                        connection.disconnect();//断开连接
                    }
                }
            }
        }).start();
    }
}
