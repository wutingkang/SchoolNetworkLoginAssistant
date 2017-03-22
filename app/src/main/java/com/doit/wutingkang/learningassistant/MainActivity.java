package com.doit.wutingkang.learningassistant;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by wutingkang on 2017/3/20.
 */

public class MainActivity extends AppCompatActivity {
    public static int LOGIN_SUCCESS = 11;
    public static int USERNAME_ERROR = 22;
    public static int PASSWORD_ERROR = 33;
    public static int UNSURE = 44;

    private EditText editName;
    private EditText editPassWord;
    private static Button btnLogin;
    SharedPreferences spsLogin;
    SharedPreferences.Editor spsEditorLogin;


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
                //判断wifi的状态，实现一键连接
                WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (! manager.isWifiEnabled()) {
                    manager.setWifiEnabled(true);//打开wifi再连接
                }

                spsEditorLogin.putString("userName", editName.getText().toString());
                spsEditorLogin.putString("passWord", editPassWord.getText().toString());
                spsEditorLogin.apply();
                btnLogin.setText("正在登录...");
                new Thread(WifiInfoReceiver.initRunnable(handlerMain, MainActivity.this,
                        editName.getText().toString(), editPassWord.getText().toString())).start();
            }
        });

    }

    public Handler handlerMain = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == LOGIN_SUCCESS) {
                 btnLogin.setText("登录成功,请关闭此页面");
            } else if (msg.what == USERNAME_ERROR) {
                 btnLogin.setText("用户名错误");
                editName.setError("用户名错误");
            } else if (msg.what == PASSWORD_ERROR) {
                 btnLogin.setText("登录失败,密码错误");
                editPassWord.setError("密码错误");
            }else if (msg.what == UNSURE){
                 btnLogin.setText("不知道连接是否成功，，，");
            }
        }
    };
}
