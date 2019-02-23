package cn.lemon.whiteboard.module.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;

import cn.alien95.util.Utils;
import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.whiteboard.R;
import cn.lemon.whiteboard.module.main.MainActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LoginActivity extends ToolbarActivity implements View.OnClickListener {

    private EditText etMobile;
    private EditText etPassword;
    private ImageView ivShowPwd;
    private Button btnLogin;
    private boolean loging = false;// 没有菊花防多次登录

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        initView();
    }

    private void initView() {
        etMobile = (EditText) findViewById(R.id.et_mobile);
        etPassword = (EditText) findViewById(R.id.et_password);
        ivShowPwd = (ImageView) findViewById(R.id.iv_show_pwd);
        btnLogin = (Button) findViewById(R.id.btn_login);
        findViewById(R.id.tv_tourist).setOnClickListener(this);

//        etMobile.setText("TP101000H770317");
//        etPassword.setText("000000");
        etMobile.setText(Utils.getLoginName());
        etPassword.setText(Utils.getPassword());

        ivShowPwd.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_show_pwd:
                if (etPassword.getInputType() != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    ivShowPwd.setImageResource(R.drawable.pass_visuable);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    ivShowPwd.setImageResource(R.drawable.pass_gone);
                }
                String pwd = etPassword.getText().toString();
                if (!TextUtils.isEmpty(pwd)) {
                    etPassword.setSelection(pwd.length());
                }
                break;
            case R.id.btn_login:
                if (!loging) {
                    login();
                }
                break;
            case R.id.tv_tourist:
                etMobile.setText("");
                etPassword.setText("");
                jumpMain();
                break;
        }
    }

    /**
     * 登录
     */
    private void login() {
        if (TextUtils.isEmpty(etMobile.getText())) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(etPassword.getText())) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        loging = true;
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("loginName", etMobile.getText().toString().trim())
                .add("password", etPassword.getText().toString())
                .build();
        Request request = new Request.Builder()
                .url("http://weike.qingcan-edu.com:8910/dc/bd/welcome!ajaxValidationUser.action")
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                loginFailed();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    String json = body.string();
                    LoginResult loginResult = new Gson().fromJson(json, LoginResult.class);
                    if (loginResult.isSuccessStatus()) {
                        jumpMain();
                    } else {
                        showToast(loginResult.getErrorMsg());
                    }
                } else {
                    loginFailed();
                }
            }
        });
    }

    private void loginFailed() {
        loging = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void jumpMain() {
        Utils.saveLoginName(etMobile.getText().toString());
        Utils.savePassword(etPassword.getText().toString());
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
