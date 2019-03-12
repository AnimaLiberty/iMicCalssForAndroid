package cn.lemon.whiteboard.module.main;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import cn.alien95.util.Utils;
import cn.lemon.common.base.ToolbarActivity;
import cn.lemon.common.base.presenter.RequirePresenter;
import cn.lemon.view.RefreshRecyclerView;
import cn.lemon.whiteboard.R;
import cn.lemon.whiteboard.app.App;
import cn.lemon.whiteboard.app.Config;
import cn.lemon.whiteboard.module.account.NoteActivity;
import cn.lemon.whiteboard.module.account.VideoActivity;
import cn.lemon.whiteboard.module.qcxt.DeviceListActivity;
import cn.lemon.whiteboard.module.qcxt.MyPoint;
import cn.lemon.whiteboard.module.qcxt.PointFactory;
import cn.lemon.whiteboard.module.qcxt.Refreshd;
import cn.lemon.whiteboard.module.qcxt.UartService;
import cn.lemon.whiteboard.module.qcxt.UnoteManager;
import cn.lemon.whiteboard.screen.ScreenRecordService;
import cn.lemon.whiteboard.screen.ScreenUtil;
import cn.lemon.whiteboard.screen.util.CommonUtil;
import cn.lemon.whiteboard.widget.BoardView;
import cn.lemon.whiteboard.widget.FloatViewGroup;
import cn.lemon.whiteboard.widget.InputDialog;
import cn.lemon.whiteboard.widget.SuperViewPager;
import cn.lemon.whiteboard.widget.shape.DrawShape;
import cn.lemon.whiteboard.widget.shape.ShapeResource;
import cn.lemon.whiteboard.widget.shape.Type;


@RequirePresenter(MainPresenter.class)
public class MainActivity extends ToolbarActivity<MainPresenter>
        implements NavigationView.OnNavigationItemSelectedListener, ViewPager.OnPageChangeListener, View.OnClickListener {

    //    private BoardView mBoardView;
    private FloatViewGroup mFloatViews;
    private FunctionAdapter mAdapter;
    private long mFirstPressBackTime = 0;
    private Handler mHandler;

    private boolean isShowingColorSelector = false;
    private boolean isShowingSizeSelector = false;
    private boolean isShowingPenSet = false;
    private boolean isShowingWipe = false;

    private boolean isConnect = false;// 连接状态

    private PopupWindow mColorWindow;
    private PopupWindow mSizeWindow;
    private PopupWindow mPenSetWindow;
    private PopupWindow mWipeWindow;

    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;

    private final int REQUEST_LOCATION_CODE = 1001;// 请求定位权限
    private final int AUDIO_RECORD_CODE = 1003;// 请求录音权限请求码
    private final int PERMISSION_GROUP_CODE = 1002;// 请求一组权限
    private int tempDrawType = 0;// 临时保存橡皮擦类型
    private String tempDrawTypeTxt;


    private UartService mService = null;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            if (mService == null) {
//                Log.w("mService is null");
                return;
            }
            if (!mService.initialize()) {
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private Context context;
    private SuperViewPager mVpBoard;
    private BoardFragmentPagerAdapter mBoardAdapter;
    private TextView tvIndex;
    private ImageView ivRecord;
    private ImageView ivPlay;
    private TextView tvTime;
    private ImageView ivPen;
    private ImageView ivMainMenu;
    private ImageView ivSave;
    private ImageView ivEraser;
    private View toolBar;
    private ImageView ivBluetooth;
    private TextView tvIndicate;

    private void Init_service() {
        System.out.println("Init_service");
        Intent bindIntent = new Intent(context, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver,
                makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    boolean isReceiving = false;
    private List<Byte> byteList = new ArrayList<>();
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action == null) {
                    Log.w("qcxt : ", "action is null ..");
                    return;
                }
                if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                    isConnect = true;
                    System.out.println("BroadcastReceiver:ACTION_GATT_CONNECTED");
                    ivBluetooth.setImageResource(R.drawable.bluetooth_conn);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            initCommand();
                        }
                    }, 2000);
                }
                if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                    isConnect = false;
                    ivBluetooth.setImageResource(R.drawable.bluetooth_disconn);
                    System.out.println("BroadcastReceiver:ACTION_GATT_DISCONNECTED");
                    Log.w("qcxt : ", "设备已断开连接...");
                    if (mService != null) {
                        mService.disconnect();
                    }
                    searchBluetooth();
                }
                if ((action.equals(UartService.ACTION_DATA_AVAILABLE))) {
                    byte[] rxValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                    for (int i = 0; i < rxValue.length; i++) {
                        if ("fc".equals(Integer.toHexString(rxValue[i]).toLowerCase()) || "fffffffc".equals(Integer.toHexString(rxValue[i]).toLowerCase())) {
                            isReceiving = true;
                            byteList.clear();
                            byteList.add(rxValue[i]);
                        } else if ("ff".equals(Integer.toHexString(rxValue[i]).toLowerCase()) || "ffffffff".equals(Integer.toHexString(rxValue[i]).toLowerCase())) {
                            byteList.add(rxValue[i]);
                            byte[] bytes = new byte[byteList.size()];
                            for (int i1 = 0; i1 < byteList.size(); i1++) {
                                bytes[i1] = byteList.get(i1);
                            }
                            if (!getCommand().equals(Integer.toHexString(byteList.get(2)).toLowerCase().substring(1))) {
                                UnoteManager.readDataFromSerial(bytes);
                                setCommand("01013110" + Integer.toHexString(byteList.get(2)).toLowerCase().substring(1));
                            }
                            isReceiving = false;
                        } else {
                            if (isReceiving) {
                                byteList.add(rxValue[i]);
                            }
                        }
                    }
                }
                if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                    mService.enableTXNotification();
                }
                if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                    mService.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };
    private BluetoothAdapter mBtAdapter = null;
    private Intent enableIntent;
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_SCREEN = 3;

    /**
     * 搜索附近蓝牙设备
     */
    private void searchBluetooth() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Log.w("qcxt : ", "该设备无蓝牙模块...");
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Intent newIntent = new Intent(context, DeviceListActivity.class);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        }

    }

    private void initCommand() {
        String command2 = "9" + "\r" + "\n";
        byte[] b = command2.getBytes();
        mService.writeRXCharacteristic(b);
        commandThread();
    }

    private String command = "010131100" + "\r" + "\n";

    private void setCommand(String com) {
        command = com + "\r" + "\n";


    }

    private String getCommand() {
        return command.substring(8);
    }

    private Thread commandThread;

    private void commandThread() {
        commandThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (threadIsAlive) {
                            Thread.sleep(100);
                            if (mService != null)
                                mService.writeRXCharacteristic(command.getBytes());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        commandThread.start();
    }

    boolean threadIsAlive = true;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commandThread != null && commandThread.isAlive()) {
            threadIsAlive = false;
            commandThread.interrupt();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setToolbarHomeBack(false);
        setContentView(R.layout.main_activity);

        context = this;
        init();
        initView();
        initToolBar();
        initBoard();
        // 开启录屏服务
        startScreenRecordService();
        initBluetooth();
    }

    private void initBluetooth() {
        // 检查定位权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 已获得定位权限，获取附近设备列表
            getBluetoothDevice();
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
        }
    }

    private void getBluetoothDevice() {
        Init_service();
        searchBluetooth();
    }

    private void init() {
        // 获取屏幕宽高
        CommonUtil.init(this);
        mHandler = new Handler(getMainLooper());
        UnoteManager.init(App.getInstance());
        pointFactory = App.getInstance().getPointFactory();
        App.getInstance().setsPoint(setPoint);
    }

    private void initView() {
        tvIndicate = (TextView) findViewById(R.id.tv_indicate);

        mFloatViews = (FloatViewGroup) findViewById(R.id.float_view_group);
        mVpBoard = (SuperViewPager) findViewById(R.id.vp_board_list);
        mVpBoard.setScanScroll(false);
        tvIndex = (TextView) findViewById(R.id.tv_index);
        ivBluetooth = (ImageView) findViewById(R.id.iv_bt);
        ivBluetooth.setOnClickListener(this);
        findViewById(R.id.iv_front).setOnClickListener(this);
        findViewById(R.id.iv_left).setOnClickListener(this);
        findViewById(R.id.iv_right).setOnClickListener(this);
        findViewById(R.id.iv_after).setOnClickListener(this);
        findViewById(R.id.iv_menu).setOnClickListener(this);
        findViewById(R.id.iv_picture).setOnClickListener(this);

        //系统生成
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, getToolbar(), R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                dismissWindow();
            }
        };
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
    }


    /**
     * 获取录屏需要的权限
     */
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] mPermissionList = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO};

            List<String> notPermissionList = new ArrayList<>();
            for (String permission : mPermissionList) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    notPermissionList.add(permission);
                }
            }
            String[] permissionArray = notPermissionList.toArray(new String[notPermissionList.size()]);
            if (permissionArray != null && permissionArray.length > 1) {
                ActivityCompat.requestPermissions(this, permissionArray, PERMISSION_GROUP_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 定位权限已获得，搜索附近设备列表
                    getBluetoothDevice();
                } else {
                    Toast.makeText(this, "无法搜索附近笔设备，前往设置进行权限授予", Toast.LENGTH_LONG).show();
                }
                break;
            case AUDIO_RECORD_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //权限已授予
                    recordScreen();
                } else {
                    //权限未授予
                    Toast.makeText(this, "未获得权限，无法进行录屏", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 初始化工具栏
     */
    private void initToolBar() {
        toolBar = findViewById(R.id.rl_toolbar);

        ivMainMenu = (ImageView) findViewById(R.id.iv_main_menu);
        ivRecord = (ImageView) findViewById(R.id.iv_record);
        ivPlay = (ImageView) findViewById(R.id.iv_play);
        tvTime = (TextView) findViewById(R.id.tv_time);
        ivPen = (ImageView) findViewById(R.id.iv_pen);
        ivSave = (ImageView) findViewById(R.id.iv_save);
        ivEraser = (ImageView) findViewById(R.id.iv_eraser);

        ivMainMenu.setOnClickListener(this);
        ivRecord.setOnClickListener(this);
        ivPlay.setOnClickListener(this);

        ivPen.setOnClickListener(this);
        ivEraser.setOnClickListener(this);
        ivSave.setOnClickListener(this);
    }

    private ArrayList<BoardFragment> mBoardList = new ArrayList<>();// 画板列表
    private ArrayList<PageInfo> pageInfoList = new ArrayList<>();

    /**
     * 初始化一个画板
     */
    private void initBoard() {
        for (int i = 0; i < PageIndexHelper.maxIndex; i++) {
            BoardFragment boardPage = new BoardFragment();
            boardPage.setOnDownAction(new BoardView.OnDownAction() {
                @Override
                public void dealDownAction() {
                    mFloatViews.checkShrinkViews();
                    dismissWindow();
                }
            });
            boardPage.setBoardViewCreateListener(new BoardFragment.BoardViewCreateListener() {
                @Override
                public void onBoardViewCreate(BoardView boardView) {

                }
            });
            mBoardList.add(boardPage);
            // 创建对应的PageInfo
            PageInfo pageInfo = new PageInfo();
            pageInfo.setVpPosition(i);
            pageInfo.setPosition(i + 1);
            pageInfo.setCreate(i == 0);
            pageInfoList.add(pageInfo);
        }
        mBoardAdapter = new BoardFragmentPagerAdapter(getSupportFragmentManager(), mBoardList);
        mVpBoard.setAdapter(mBoardAdapter);
        mVpBoard.addOnPageChangeListener(this);
        mVpBoard.setOffscreenPageLimit(PageIndexHelper.maxIndex);// 防止销毁之前的页面   最多有10页

        // 初始化工具栏
        mAdapter = new FunctionAdapter(this, tvIndicate);
        mFloatViews.setAdapter(mAdapter);

        setCurrentPage();
    }

    public PointFactory pointFactory;

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            if (System.currentTimeMillis() - mFirstPressBackTime > 1000) {
                mFirstPressBackTime = System.currentTimeMillis();
                Utils.SnackbarShort(getToolbar(), "再点击一次退出App");
            } else
                super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recall:
                getCurrentBoardView().reCall();
                break;
            case R.id.recover:
                getCurrentBoardView().undo();
                break;
            case R.id.save_note:
                showNoteDialog();
                break;
            case R.id.save_image_to_app:
                getPresenter().saveImage(getCurrentBoardView().getDrawBitmap());
                break;
            case R.id.color:
                if (isShowingColorSelector) {
                    mColorWindow.dismiss();
                    isShowingColorSelector = false;
                } else {
                    showColorSelectorWindow();
                }
                break;
            case R.id.size:
                if (isShowingSizeSelector) {
                    mSizeWindow.dismiss();
                    isShowingSizeSelector = false;
                } else {
                    showSizeSelectorWindow();
                }
                break;
            default:
                break;
        }
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        mDrawer.closeDrawer(GravityCompat.START);
        int id = item.getItemId();
        switch (id) {
            case R.id.note:// 我的笔记
                Intent intent = new Intent(new Intent(this, NoteActivity.class));
                startActivityForResult(intent, Config.NOTE_REQUEST_CODE);
                break;
            case R.id.video:// 我的视频
                getPresenter().startActivity(VideoActivity.class);
                break;
            case R.id.shop:// 在线购买
                startActivity(new Intent(this, WebActivity.class));
                break;
            case R.id.service:// 在线客服
                startActivity(new Intent(this, ServiceActivity.class));
                break;
            case R.id.about:// 关于我没
                getPresenter().startActivity(AboutActivity.class);
                break;
            default:
                break;
        }
        return true;
    }

    //保存笔迹
    public void showNoteDialog() {
        final InputDialog noteDialog = new InputDialog(this);
        noteDialog.setCancelable(false);
        noteDialog.setTitle("请输入标题");
        noteDialog.setHint("标题");
        String noteTitle = Utils.getNoteTitle();
        noteDialog.getInputView().setText(noteTitle);
        if (getPresenter().getLocalNote() != null) {
            noteDialog.setContent(getPresenter().getLocalNote().mTitle);
        }
        noteDialog.show();
        noteDialog.setPositiveClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getPresenter().saveNote(noteDialog.getContent(), getCurrentBoardView().getNotePath());
                noteDialog.dismiss();
                getCurrentBoardView().clearScreen();
            }
        });
        noteDialog.setPassiveClickListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                noteDialog.dismiss();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getCurrentBoardView().updateBitmap();
                    }
                }, 100);
            }
        });
    }

    public void updateDrawPaths(List<ShapeResource> paths) {
        getCurrentBoardView().updateDrawFromPaths(paths);
    }

    //设置画笔大小
    public void showSizeSelectorWindow() {
        if (isShowingSizeSelector) {
            return;
        } else if (isShowingColorSelector) {
            mColorWindow.dismiss();
            isShowingColorSelector = false;
        } else if (isShowingPenSet) {
            mPenSetWindow.dismiss();
            isShowingPenSet = false;
        }
        isShowingSizeSelector = true;
        View view = LayoutInflater.from(this).inflate(R.layout.main_window_size_selector, null);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        final TextView size = (TextView) view.findViewById(R.id.size);
        int numSize = (int) DrawShape.mPaintWidth;
        seekBar.setProgress(numSize);
        size.setText(numSize + "");

        mSizeWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mSizeWindow.showAsDropDown(toolBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                size.setText(progress + "");
                DrawShape.mPaintWidth = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSizeWindow.dismiss();
                isShowingSizeSelector = false;
            }
        });
    }

    //颜色选择器
    public void showColorSelectorWindow() {
        if (isShowingColorSelector) {
            return;
        } else if (isShowingSizeSelector) {
            mSizeWindow.dismiss();
            isShowingSizeSelector = false;
        } else if (isShowingPenSet) {
            mPenSetWindow.dismiss();
            isShowingPenSet = false;
        }
        isShowingColorSelector = true;
        View view = LayoutInflater.from(this).inflate(R.layout.main_window_color_selector, null);
        mColorWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        RefreshRecyclerView recyclerView = (RefreshRecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        ColorAdapter adapter = new ColorAdapter(this, Config.COLORS, mColorWindow);
        recyclerView.setAdapter(adapter);

        mColorWindow.showAsDropDown(toolBar);
    }

    //笔设置窗口
    public void showPenSetWindow() {
        if (isShowingPenSet) {
            return;
        } else if (isShowingSizeSelector) {
            mSizeWindow.dismiss();
            isShowingSizeSelector = false;
        } else if (isShowingColorSelector) {
            mColorWindow.dismiss();
            isShowingColorSelector = false;
        } else if (isShowingWipe) {
            mWipeWindow.dismiss();
            isShowingWipe = false;
        }
        if (getCurrentBoardView().getDrawType() == Type.WIPE) {
            // 如果是橡皮，那就切换成笔
            if (tempDrawType == 0) {
                tempDrawType = Type.CURVE;
                tempDrawTypeTxt = "曲线";
            }
            tvIndicate.setText(tempDrawTypeTxt);
            getCurrentBoardView().setDrawType(tempDrawType);
            return;
        }
        isShowingPenSet = true;
        View view = LayoutInflater.from(this).inflate(R.layout.main_window_pen_set, null);
        mPenSetWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        RefreshRecyclerView recyclerView = (RefreshRecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2, LinearLayoutManager.HORIZONTAL, false));
        ColorAdapter adapter = new ColorAdapter(this, Config.COLORS, mPenSetWindow);
        recyclerView.setAdapter(adapter);


        // 画笔粗细
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
        final TextView size = (TextView) view.findViewById(R.id.size);
        int numSize = (int) DrawShape.mPaintWidth;
        seekBar.setProgress(numSize);
        size.setText(numSize + "");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                size.setText(progress + "");
                DrawShape.mPaintWidth = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPenSetWindow.dismiss();
                isShowingPenSet = false;
            }
        });

        mPenSetWindow.showAsDropDown(toolBar);
    }

    /**
     * 显示橡皮擦窗口
     */
    private void showWipeWindow() {
        if (isShowingWipe) {
            return;
        } else if (isShowingSizeSelector) {
            mSizeWindow.dismiss();
            isShowingSizeSelector = false;
        } else if (isShowingColorSelector) {
            mColorWindow.dismiss();
            isShowingColorSelector = false;
        } else if (isShowingPenSet) {
            mPenSetWindow.dismiss();
            isShowingPenSet = false;
        }
        isShowingWipe = true;
        View view = LayoutInflater.from(this).inflate(R.layout.main_window_wipe, null);
        mWipeWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        view.findViewById(R.id.rl_small).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 最小的橡皮
                setEraser(20);
            }
        });
        view.findViewById(R.id.rl_middle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 中间的橡皮
                setEraser(50);
            }
        });
        view.findViewById(R.id.rl_big).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 最大的橡皮
                setEraser(100);
            }
        });
        view.findViewById(R.id.ll_clean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 清屏
                getCurrentBoardView().clearScreen();
                getPresenter().setLocalNoteNull();
                mWipeWindow.dismiss();
            }
        });
        tempDrawTypeTxt = tvIndicate.getText().toString();
        tvIndicate.setText("橡皮擦");
        mWipeWindow.showAsDropDown(toolBar);
    }

    /**
     * 设置橡皮擦
     *
     * @param size 橡皮大小
     */
    private void setEraser(int size) {
        tempDrawType = getCurrentBoardView().getDrawType();
        getCurrentBoardView().setDrawType(Type.WIPE);
        getCurrentBoardView().setWipeWidth(size);
        mWipeWindow.dismiss();
    }

    /**
     * 获取当前页面的画板
     *
     * @return
     */
    public BoardView getCurrentBoardView() {
        return mBoardList.get(mVpBoard.getCurrentItem()).mBoardView;
    }

    public List<BoardView> getAllBoardView() {
        ArrayList<BoardView> boardViews = new ArrayList<>();
        for (BoardFragment boardFragment : mBoardList) {
            boardViews.add(boardFragment.mBoardView);
        }
        return boardViews;
    }

    public void setShowingColorSelector(boolean b) {
        isShowingColorSelector = b;
    }

    private BluetoothDevice mDevice = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    mService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Intent newIntent = new Intent(context, DeviceListActivity.class);
                    startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                } else {
                    Toast.makeText(this, "蓝牙连接失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_SCREEN:// 屏幕录制
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        ScreenUtil.setUpData(resultCode, data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                }
            default:
                System.out.println("wrong request code");
                break;
        }
    }

    private int oldX;
    private int oldY;
    public static boolean isClearScreen = false;

    private int fingerTag = -1;
    public Refreshd setPoint = new Refreshd() {

        public void drawLine(int flag, float previousX, float previousY, final float x, final float y) {
            // TODO Auto-generated method stub
            Log.w("flag", flag + "");

            getCurrentBoardView().receiveXY(flag, (int) previousX, (int) previousY);
            fingerTag = flag;
        }

        @Override
        public void refresh() {
            Queue<MyPoint> points = pointFactory.getPoints();
            MyPoint p0 = points.poll();

            MyPoint p1 = null;
            while (p0 != null) {
                try {
                    p1 = points.poll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (p1 == null) {
                    return;
                }
                if (isClearScreen) {//清屏，落笔
                    if (p1.x > 0) {
                        isClearScreen = false;
                        drawLine(0, p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale(), p0.x / App.getInstance().getQCXTScale(), p0.y / App.getInstance().getQCXTScale());
                        p0 = p1;
                    }
                } else {
                    if (p1.x < 0 || p1.y < 0) {//画完抬笔
                        if (fingerTag == 2 || fingerTag == 3) {
                            drawLine(3, p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale(), p0.x / App.getInstance().getQCXTScale(), p0.y / App.getInstance().getQCXTScale());
                        } else if (fingerTag != 2) {
                            drawLine(2, p0.x / App.getInstance().getQCXTScale(), p0.y / App.getInstance().getQCXTScale(), p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale());
                        }
                    } else if (p0.x < 0 && p1.x > 0) {//落笔
                        drawLine(0, p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale(), p0.x / App.getInstance().getQCXTScale(), p0.y / App.getInstance().getQCXTScale());
                        p0 = p1;
                    } else if (p1.x == -1) {
                        p0 = p1;
                    } else {
                        drawLine(1, p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale(), p0.x / App.getInstance().getQCXTScale(), p0.y / App.getInstance().getQCXTScale());
                        p0 = p1;
                    }
                }

                /*if (p0.x == -1) {
                    mPath.moveTo(p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale());
                    p0 = p1;
                    continue;
                }
                if (p1.x == -1) {
                    p0 = p1;
                    continue;
                }*/


//                drawLine(1,p0.x / App.getInstance().getQCXTScale(), p0.y / App.getInstance().getQCXTScale(), p1.x / App.getInstance().getQCXTScale(), p1.y / App.getInstance().getQCXTScale());
//                p0 = p1;
            }
        }
    };
    private final Path mPath = new Path();

    /**
     * 页面切换
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        setCurrentPage();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onClick(View view) {
        dismissWindow();
        switch (view.getId()) {
            case R.id.iv_front:
                // 撤回
                getCurrentBoardView().reCall();
                break;
            case R.id.iv_after:
                // 恢复
                getCurrentBoardView().undo();
                break;
            case R.id.iv_left:
                frontPage();
                break;
            case R.id.iv_right:
                nextPage();
                break;
            case R.id.iv_menu:
                mFloatViews.onClick(view);
                break;
            case R.id.iv_picture:

                break;
            case R.id.iv_main_menu:
                mDrawer.openDrawer(Gravity.LEFT);
                break;
            case R.id.iv_record:
                // 开始 停止
                if (ScreenUtil.isCurrentRecording()) {
                    ScreenUtil.stopScreenRecord(this);
                } else {
                    recordScreen();
                }
                break;
            case R.id.iv_play:
                // 暂停 继续
                if (ScreenUtil.isCurrentPause()) {
                    // 继续
                    ScreenUtil.resumeRecord();
                } else {
                    // 暂停
                    ScreenUtil.pauseRecord();
                }
                recordScreen();
                break;
            case R.id.iv_pen:
                if (isShowingPenSet) {
                    mPenSetWindow.dismiss();
                    isShowingPenSet = false;
                } else {
                    showPenSetWindow();
                }
                break;
            case R.id.iv_eraser:
                if (isShowingWipe) {
                    mWipeWindow.dismiss();
                    isShowingWipe = false;
                } else {
                    showWipeWindow();
                }
                break;
            case R.id.iv_save:
                showNoteDialog();
                break;
            case R.id.iv_bt:
                if (isConnect) {
                    Toast.makeText(this, "设备已连接", Toast.LENGTH_SHORT).show();
                } else {
                    searchBluetooth();
                }
                break;
        }
    }

    /**
     * 录制屏幕
     */
    private void recordScreen() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //如果用户已经拒绝过一次权限申请，该方法返回true
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
                //提示用户这一权限的重要性
                Toast.makeText(MainActivity.this, "需要录音和存储权限", Toast.LENGTH_SHORT).show();
            }
            //请求权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, AUDIO_RECORD_CODE);
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //如果用户已经拒绝过一次权限申请，该方法返回true
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)) {
                //提示用户这一权限的重要性
                Toast.makeText(MainActivity.this, "需要录音和存储权限", Toast.LENGTH_SHORT).show();
            }
            //请求权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, AUDIO_RECORD_CODE);
        } else {
            // 已获得录音和写入权限
            startRecord();
        }
    }

    /**
     * 开始录制屏幕
     */
    private void startRecord() {
        if (!ScreenUtil.isCurrentRecording()) {
            ScreenUtil.startScreenRecord(this, REQUEST_SCREEN);
        }
    }

    /**
     * 删除分页
     */
    public void deletePage() {
        if (getPageCount() > 1) {
            PageInfo currentPage = getCurrentPageInfo();
            int delPosition = currentPage.getPosition();
            getCurrentBoardView().clearScreen();
            currentPage.delete();
            // 修改其它页的索引
            for (PageInfo pageInfo : pageInfoList) {
                if (pageInfo.getVpPosition() != currentPage.getVpPosition()) {
                    // 修改非当前页的索引
                    if (pageInfo.getPosition() > delPosition) {
                        pageInfo.setPosition(pageInfo.getPosition() - 1);
                    }
                }
            }
            // 跳转上一页或下一页
            for (PageInfo pageInfo : pageInfoList) {
                if (pageInfo.isCreate() && pageInfo.getPosition() == delPosition) {
                    mVpBoard.setCurrentItem(pageInfo.getVpPosition(), false);
                    return;
                }
            }
            for (PageInfo pageInfo : pageInfoList) {
                if (pageInfo.isCreate() && pageInfo.getPosition() == delPosition - 1) {
                    mVpBoard.setCurrentItem(pageInfo.getVpPosition(), false);
                    return;
                }
            }
        } else {
            Toast.makeText(this, "已经是最后一页，不能再删除了", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 上一页
     */
    private void frontPage() {
        PageInfo currentPage = getCurrentPageInfo();
        if (currentPage.isFirstPage()) {
            Toast.makeText(this, "已经是第一页了", Toast.LENGTH_SHORT).show();
        } else {
            for (PageInfo pageInfo : pageInfoList) {
                if (currentPage.getPosition() - 1 == pageInfo.getPosition()) {
                    pageInfo.create();
                    mVpBoard.setCurrentItem(pageInfo.getVpPosition(), false);
                }
            }
        }
    }

    /**
     * 下一页
     */
    private void nextPage() {
        PageInfo currentPage = getCurrentPageInfo();
        if (currentPage.isLastPage()) {
            Toast.makeText(this, "已经是最后一页了", Toast.LENGTH_SHORT).show();
        } else {
            for (PageInfo pageInfo : pageInfoList) {
                if (currentPage.getPosition() + 1 == pageInfo.getPosition()) {
                    pageInfo.create();
                    mVpBoard.setCurrentItem(pageInfo.getVpPosition(), false);
                }
            }
        }
    }

    private PageInfo getCurrentPageInfo() {
        return pageInfoList.get(mVpBoard.getCurrentItem());
    }


    /**
     * 开启录制 Service
     */
    private void startScreenRecordService() {
        ServiceConnection mScreenService = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ScreenRecordService.RecordBinder recordBinder = (ScreenRecordService.RecordBinder) service;
                ScreenRecordService screenRecordService = recordBinder.getRecordService();
                ScreenUtil.setScreenService(screenRecordService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent intent = new Intent(this, ScreenRecordService.class);
        bindService(intent, mScreenService, BIND_AUTO_CREATE);
        ScreenUtil.addRecordListener(recordListener);
    }

    /**
     * 录制监听
     */
    private ScreenUtil.RecordListener recordListener = new ScreenUtil.RecordListener() {
        @Override
        public void onStartRecord() {
            tvTime.setVisibility(View.VISIBLE);
            ivPlay.setVisibility(View.VISIBLE);
            ivPlay.setImageResource(R.drawable.stop);
            ivRecord.setImageResource(R.drawable.save);
        }

        @Override
        public void onPauseRecord() {
            ivPlay.setImageResource(R.drawable.play);
//            Toast.makeText(MainActivity.this, "暂停", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResumeRecord() {
            ivPlay.setImageResource(R.drawable.stop);
//            Toast.makeText(MainActivity.this, "继续", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecord(String stopTip) {
            ivPlay.setImageResource(R.drawable.play);
            tvTime.setText("00:00");
            tvTime.setVisibility(View.GONE);
            ivPlay.setVisibility(View.GONE);
            ivRecord.setImageResource(R.drawable.record);
            Toast.makeText(MainActivity.this, stopTip, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRecording(String timeTip) {
            tvTime.setText(timeTip);
        }
    };

    /**
     * 关闭各个窗口
     */
    private void dismissWindow() {
        if (isShowingColorSelector) {
            mColorWindow.dismiss();
            isShowingColorSelector = false;
        } else if (isShowingSizeSelector) {
            mSizeWindow.dismiss();
            isShowingSizeSelector = false;
        } else if (isShowingPenSet) {
            mPenSetWindow.dismiss();
            isShowingPenSet = false;
        } else if (isShowingWipe) {
            mWipeWindow.dismiss();
            isShowingWipe = false;
        }
    }


    private void setCurrentPage() {
        PageInfo pageInfo = pageInfoList.get(mVpBoard.getCurrentItem());
        tvIndex.setText(pageInfo.getPosition() + "/" + getPageCount());
    }

    private int getPageCount() {
        int pageCount = 0;
        for (PageInfo pageInfo : pageInfoList) {
            if (pageInfo.isCreate()) {
                pageCount++;
            }
        }
        return pageCount;
    }

}
