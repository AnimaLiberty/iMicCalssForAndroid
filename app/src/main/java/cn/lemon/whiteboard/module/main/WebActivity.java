package cn.lemon.whiteboard.module.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.whiteboard.R;
import cn.lemon.whiteboard.app.Api;

public class WebActivity extends ToolbarActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_shop);

        initWebView();
    }

    private void initWebView() {
        WebView webView = (WebView) findViewById(R.id.web_view);
        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //设置缓存
        webSettings.setJavaScriptEnabled(true);//设置能够解析Javascript
        webSettings.setDomStorageEnabled(true);//设置适应Html5
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;

                try {
                    if (url.startsWith("weixin://") //微信
                            || url.startsWith("alipays://") //支付宝
                            || url.startsWith("mailto://") //邮件
                            || url.startsWith("tel://")//电话
                            || url.startsWith("dianping://")//大众点评
                            || url.startsWith("tbopen://")//淘宝
                            || url.startsWith("openapp.jdmobile://")//淘宝
                            || url.startsWith("tmast://")//淘宝

                        //其他自定义的scheme
                    ) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception e) { //防止crash (如果手机上没有安装处理某个scheme开头的url的APP, 会导致crash)
                    return true;//没有安装该app时，返回true，表示拦截自定义链接，但不跳转，避免弹出上面的错误页面
                }
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl(Api.WEB_SHOP);
    }
}
