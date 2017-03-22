package com.doit.wutingkang.learningassistant;

import android.content.BroadcastReceiver;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by wutingkang on 2017/3/20.
 */

public class WifiInfoReceiver extends BroadcastReceiver {
    private Context context;
    private NotificationManager notifyManager;
    private static final int NOTIFICATION_ID = 1;

    private static final String TAG = "wifiReceiver";


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onReceive(final Context context, Intent intent) {
        this.context = context;
        //没有登录则才login


        login(context, intent);
    }

    public void login(final Context context, Intent intent) {
        SharedPreferences spsLogin;
        String userName, passWord;

        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            final Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;
                if (isConnected) {
                    Log.i(TAG, "onReceive: 连接上可用的WIFi"); // 开始登录

                    spsLogin = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
                    userName = spsLogin.getString("userName", "");
                    passWord = spsLogin.getString("passWord", "");
                    new Thread(initRunnable(handler, context, userName, passWord)).start();
                } else {
                    Log.i(TAG, "onReceive: 未连接可用wifi");
                }
            }
        }
    }

    public static Runnable initRunnable(final Handler handler, final Context context, final String userName, final String passWord) {
        Runnable runnable = new Runnable() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                String strRequestURL = "http://10.3.8.211/";

                //根据登陆网页的http报文提交的参数确定
                String parameter = "DDDDD=" + userName +
                                    "&upass=" + passWord +
                                    "&0MKKey=" + "";

                HttpURLConnection connection;
                URL url;
                try {
                    url = new URL(strRequestURL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(4000);
                    connection.setReadTimeout(4000);

                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.setUseCaches(false);

                    //设置请求属性 使用httpwatchpro查看对应值都要设置吗？
                    //connection.setRequestProperty("Accept", "text/html, application/xhtml+xml, */*");
                    connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                    ///connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.8");
                    //connection.setRequestProperty("Cache-Control", "max-age=0");
                    connection.setRequestProperty("Connection", "keep-alive");
                    //connection.setRequestProperty("Content-Length", "37");//？不同浏览器的值不一样
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");


                    connection.getOutputStream().write(parameter.getBytes());
                    //connection.connect(); //getOutputStream会隐含的进行connect,所以在开发中不调用connect()也可以

                    Log.i(TAG, "connection 应该发送了");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        String s = getHtml(connection.getInputStream(), "GBK");
                        String errorMsg = new String(s.getBytes("GBK"), "UTF-8");

                        System.out.println("Html" + s);

                        //不同学校网页返回的内容可能不一样
                        if (s.contains("您已经成功登录")) {//登录成功后返回的网页中的文字
                            handler.sendEmptyMessage(MainActivity.LOGIN_SUCCESS);
                        } else if (errorMsg.contains("ldap auth error")) {//密码错误
                            handler.sendEmptyMessage(MainActivity.PASSWORD_ERROR);
                        } else if (errorMsg.contains("账号或密码不对")) {//账号错误
                            handler.sendEmptyMessage(MainActivity.USERNAME_ERROR);
                        }else{
                            Log.i(TAG, "connection 其他UNSURE");

                            handler.sendEmptyMessage(MainActivity.UNSURE);
                        }

                        Log.i(TAG, "过了handler");

                        connection.disconnect();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        return runnable;
    }


    public static String getHtml(InputStream inputStream, String encode) {
        BufferedReader bufReader;
        StringBuffer strBuf = null;

        try {
            bufReader = new BufferedReader(new InputStreamReader(inputStream, encode));
            strBuf = new StringBuffer();
            String str;
            while ((str = bufReader.readLine()) != null) {
                if (str.isEmpty()) {

                } else {
                    strBuf.append(str);
                    strBuf.append("\n");
                }
            }
            bufReader.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return strBuf.toString();
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MainActivity.LOGIN_SUCCESS) {
                Toast.makeText(context, "登录成功~", Toast.LENGTH_SHORT).show();
            } else if (msg.what == MainActivity.USERNAME_ERROR) {
                errorNotification("用户名错误！");
            } else if (msg.what == MainActivity.PASSWORD_ERROR) {
                errorNotification("密码错误！");
            }else if (msg.what == MainActivity.UNSURE){
                errorNotification("可能是其他错误，也可能是正确登陆但是返回的字符串没有上述关键字。");
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void errorNotification(String msg) {
        notifyManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), 0);
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher) // 设置图标
                .setTicker("不知道为什么登录失败了？") // 设置在通知栏上滚动的信息
                .setContentTitle("登录失败")        // 设置主标题
                .setContentText(msg)
                .setContentIntent(pendingIntent).build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL; // 点击自动消失
        notification.defaults |= Notification.DEFAULT_VIBRATE; //使用自动的振动模式
        notifyManager.notify(NOTIFICATION_ID, notification); // 显示通知
    }
}


