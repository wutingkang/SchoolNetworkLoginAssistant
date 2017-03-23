package com.doit.wutingkang.learningassistant;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by wutingkang on 2017/3/20.
 */

public class MainActivity extends AppCompatActivity {
    private EditText editName;
    private EditText editPassWord;
    private static Button btnLogin;//还承担着提示wifi网络状态的功能
    private SharedPreferences.Editor spsEditorLogin;
    private SharedPreferences spsLogin;

    //传递给wifi处理广播，用于改变按钮文字
    public static Button getLoginButton(){
        return btnLogin;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editName = (EditText) findViewById(R.id.main_et_name);
        editPassWord = (EditText) findViewById(R.id.main_et_password);
        btnLogin = (Button) findViewById(R.id.main_btn_save);

        spsLogin = MainActivity.this.getSharedPreferences("loginInfo", this.MODE_PRIVATE);
        spsEditorLogin = spsLogin.edit();
        editName.setText(spsLogin.getString("userName", ""));
        editPassWord.setText(spsLogin.getString("passWord", ""));

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存当前数据
                spsEditorLogin.putString("userName", editName.getText().toString());
                spsEditorLogin.putString("passWord", editPassWord.getText().toString());
                spsEditorLogin.apply();


                WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                if (btnLogin.getText().equals("登录成功,再次点击退出")){
                    if (manager.isWifiEnabled())//必须确定是已登陆时才能退出，不能仅仅是wifi连接


                        finish();
                    else
                        Toast.makeText(MainActivity.this, "登录已断开，正在重新登录", Toast.LENGTH_SHORT).show();
                }else {//需放在else里， 否则finish（）之后还会执，不知道为什么
                    btnLogin.setText("登录中...");

                    //判断wifi的状态，实现一键连接并登录，如果需要的话
                    if (! manager.isWifiEnabled()) {
                        manager.setWifiEnabled(true);//打开wifi,剩下的工作交给WifiInfoReceiver去做
                    }else{
                        //如果已经打开Wifi,发送自定义广播给WifiInfoReceiver
                        Intent broadcastIntent = new Intent("com.doit.wutingkang.learningassistant.LOGIN_BROADCAST");
                        sendBroadcast(broadcastIntent);
                    }
                }
            }
        });
    }
}
