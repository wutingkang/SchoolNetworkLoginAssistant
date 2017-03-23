package com.doit.wutingkang.learningassistant;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
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

import static android.media.AudioManager.RINGER_MODE_VIBRATE;

/**
 * Created by wutingkang on 2017/3/20.
 */

public class WifiInfoReceiver extends BroadcastReceiver {
    private Context context;
    private NotificationManager notifyManager;
    private static final int NOTIFICATION_ID = 1;

    public static int LOGIN_SUCCESS = 11;
    public static int USERNAME_ERROR = 22;
    public static int PASSWORD_ERROR = 33;
    public static int UNSURE = 44;

    private static final String TAG = "wifiReceiver";


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onReceive(final Context context, Intent intent) {
        this.context = context;

        //需要登录的wifi才login
        if (needLogin(context, intent)) {
            login(context);
        }
    }

    //必须是wifi *连接上* 了某个 *需要登陆* 且 *还没有登录* 的网络时才发送登录请求
    public static boolean needLogin(final Context context, Intent intent){
        //wifi连接上与否
        if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){

            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            if(info.getState().equals(NetworkInfo.State.CONNECTED)){  //*连接上*
                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                //因为登录的用户名密码只对校园网有效，就直接根据wifi名称判断是否需要连接。(可以增加需登录的wifi名单增删改功能)

                System.out.println("连接到网络: " + wifiInfo.getSSID());

                if (wifiInfo.getSSID().equals("\"405\"") || //注意getSSID()返回值带有 ”“！！！， 而且直接用 == 比较不行
                    wifiInfo.getSSID().equals("\"BUPT-portal\"")){

                    System.out.println("inin");

                    Button btnLogin = MainActivity.getLoginButton();
                    if (btnLogin.getText().equals("登录") ||              //*还没有登录*
                        btnLogin.getText().equals("登录中...")) {

                        System.out.println("true");
                        return true;
                    }
                }
            }
        }else if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){//wifi打开与否
            int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

            if(wifistate == WifiManager.WIFI_STATE_DISABLED){
                //关闭了wifi开关，更改按钮文字
                Button btnLogin = MainActivity.getLoginButton();
                btnLogin.setText("登录");

                System.out.println("系统关闭wifi");
            }
            else if(wifistate == WifiManager.WIFI_STATE_ENABLED){//打开wifi开关不用做什么事
                System.out.println("系统开启wifi");
            }
        }else if (intent.getAction().equals("com.doit.wutingkang.learningassistant.LOGIN_BROADCAST")){ //自定义广播，模拟wifi状态发生改变
            //很遗憾，在Wifi已经打开的情况下，发送这条广播无法获得Wifi的信息（因为intent是自定义发送的）进行 *需登录* 判断，只好默认是true


            //或许可以通过http返回码来确定是否需要登录
            return true;
        }

        return  false;
    }



    public void login(final Context context) {
        SharedPreferences spsLogin;
        String userName, passWord;

        spsLogin = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE);
        userName = spsLogin.getString("userName", "");
        passWord = spsLogin.getString("passWord", "");
        new Thread(initRunnable(handlerWifi, context, userName, passWord)).start();
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

                System.out.println("in run");

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
                        String strHTMLgbk = getHtml(connection.getInputStream(), "GBK");
                        String strHTMLutf8 = new String(strHTMLgbk.getBytes("GBK"), "UTF-8");//按照utf-8编码转为java字符串

                        System.out.println("Html" + strHTMLgbk);

                        //不同学校网页返回的内容可能不一样
                        if (strHTMLgbk.contains("您已经成功登录")) {//登录成功后返回的网页中的文字
                            //连wifi时一般是在教室，顺便把手机调成静音，尊重他人，避免尴尬
                            AudioManager audioManager =  (AudioManager)context.getSystemService(Service.AUDIO_SERVICE);
                            audioManager.setRingerMode(RINGER_MODE_VIBRATE);//铃音为静音，启动震动

                            //本来想在这里改变登录按钮的文字，但发现会抛出异常，不能在其他线程改变UI线程的界面,(不过在广播里可以)

                            handler.sendEmptyMessage(LOGIN_SUCCESS);
                        } else if (strHTMLutf8.contains("ldap auth error")) {//密码错误
                            handler.sendEmptyMessage(PASSWORD_ERROR);
                        } else if (strHTMLutf8.contains("账号或密码不对")) {//账号错误
                            handler.sendEmptyMessage(USERNAME_ERROR);
                        }else{
                            Log.i(TAG, "connection 其他UNSURE");

                            handler.sendEmptyMessage(UNSURE);
                        }

                        Log.i(TAG, "过了handler");

                        connection.disconnect();
                    }else{
                        System.out.println("should be toast");

                        Toast.makeText(context, "连接到服务器出错了！", Toast.LENGTH_SHORT).show();
                    }

                    System.out.println("bottom of try");

                } catch (MalformedURLException e) {
                    System.out.println("eeeeee");

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

    public Handler handlerWifi = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Button btnLogin = MainActivity.getLoginButton();

            if (msg.what == LOGIN_SUCCESS) {
                btnLogin.setText("登录成功,再次点击退出");
                Toast.makeText(context, "登录成功~", Toast.LENGTH_SHORT).show();
            } else if (msg.what == USERNAME_ERROR) {
                btnLogin.setText("用户名错误"); //本来想用提示方式更加优美的输入组件的SharedPreferences.Editor，但是传进来后不能用setError（）
                errorNotification("用户名错误！");
            } else if (msg.what == PASSWORD_ERROR) {
                btnLogin.setText("密码错误");
                errorNotification("密码错误！");
            }else if (msg.what == UNSURE){
                btnLogin.setText("可能登录上了，你试试呗~");
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


