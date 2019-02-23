package cn.lemon.whiteboard.module.account;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.alien95.util.Utils;
import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.whiteboard.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadVideoActivity extends ToolbarActivity implements View.OnClickListener {

    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> ids = new ArrayList<>();
    private boolean[] checkedItems;
    private String tagIds;
    private TextView tvTags;
    private EditText etName;
    private EditText etDesc;
    private String videoPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_activity_video_upload);
        videoPath = getIntent().getStringExtra("videoPath");

        initView();
        initData();
    }

    private void initData() {
        String tagUrl = "http://weike.qingcan-edu.com:8910/dc//lr/learningCourseMobile!getTagVOs.action";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(tagUrl)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String result = response.body().string();
                    Gson gson = new Gson();
                    TagResult tagResult = gson.fromJson(result, TagResult.class);
                    List<TagResult.KnowledgeBean> knowledge = tagResult.getKnowledge();
                    List<TagResult.LevelBean> levels = tagResult.getLevel();
                    for (TagResult.KnowledgeBean know : knowledge) {
                        names.add(know.getName());
                        ids.add(know.getId());
                    }
                    for (TagResult.LevelBean level : levels) {
                        names.add(level.getName());
                        ids.add(level.getId());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initView() {
        findViewById(R.id.btn_select_tag).setOnClickListener(this);
        findViewById(R.id.btn_confirm).setOnClickListener(this);
        etName = (EditText) findViewById(R.id.et_name);
        etDesc = (EditText) findViewById(R.id.et_desc);
        tvTags = (TextView) findViewById(R.id.tv_tags);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_select_tag:
                showTagDialog();
                break;
            case R.id.btn_confirm:
                uploadVideo();
                break;
        }
    }

    private void showTagDialog() {
        checkedItems = new boolean[names.size()];
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择标签")
                .setMultiChoiceItems(names.toArray(new String[names.size()]), checkedItems, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked;
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setChecked();
                    }
                })
                .create();
        dialog.show();
    }

    private void setChecked() {
        StringBuilder sb = new StringBuilder();
        StringBuilder tagIds = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (checkedItems[i]) {
                sb.append(names.get(i)).append("；");
                tagIds.append(ids.get(i)).append(",");
            }
        }
        tvTags.setText(sb);
        this.tagIds = tagIds.toString();
    }


    /**
     * 上传视频
     */
    private void uploadVideo() {
        if (TextUtils.isEmpty(videoPath)) {
            Toast.makeText(this, "视频路径失效", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(etName.getText().toString().trim())) {
            Toast.makeText(this, "请输入资源名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(tagIds)) {
            Toast.makeText(this, "请选择标签", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread() {
            @Override
            public void run() {
                File videoFile = new File(videoPath);
                Map<String, Object> map = new HashMap<>();
                map.put("loginName", Utils.getLoginName());
                map.put("iMicName", etName.getText().toString());
                map.put("tagIds", tagIds);
                map.put("discript", etDesc.getText().toString());
                map.put("filename", videoFile);
                uploadMoreFile(map).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        uploadFailed();
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            final String result = response.body().string();
                            if ("true".equals(result)) {
                                uploadSuccess();
                            } else {
                                uploadFailed();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            uploadFailed();
                        }
                    }
                });
            }
        }.start();
    }

    private void uploadSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UploadVideoActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void uploadFailed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UploadVideoActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * post方式二：stirng类型参数和上传文件参数
     */
    public static Call uploadMoreFile(Map<String, Object> params) {
        //post请求方式二：multipart/form-data(不仅能够上传string类型的参数，还可以上传文件（流的形式，file）)
        OkHttpClient okHttpClient1 = new OkHttpClient();
        MultipartBody.Builder builder1 = new MultipartBody.Builder();
        builder1.setType(MultipartBody.FORM);
        for (Map.Entry<String, Object> stringObjectEntry : params.entrySet()) {
            String key = stringObjectEntry.getKey();
            Object value = stringObjectEntry.getValue();
            if (value instanceof File) {
                //如果请求的值是文件
                File file = (File) value;
                //MediaType.parse("application/octet-stream")以二进制的形式上传文件
                builder1.addFormDataPart(key, ((File) value).getName(), RequestBody.create(MediaType.parse("multipart/form-data"), file));
            } else {//如果请求的值是string类型
                builder1.addFormDataPart(key, value.toString());
            }
        }
        Request request1 = new Request.Builder()
                .post(builder1.build())
                .url("http://weike.qingcan-edu.com:8910/dc/lr/learningCourseMobile!saveMobileResource.action")
                .build();

        return okHttpClient1.newCall(request1);
    }
}
