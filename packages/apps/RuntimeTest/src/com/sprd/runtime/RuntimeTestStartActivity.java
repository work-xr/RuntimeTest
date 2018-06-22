package com.sprd.runtime;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.runtime.camera.RuntimeFrontCameraTestActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.sprd.runtime.Const.CHARGING_STOP_BATTERY_PERCENT;
import static com.sprd.runtime.Const.DEFAULT_BACKGROUND_COLOR;
import static com.sprd.runtime.Const.DEFAULT_BACKGROUND_SILENT;
import static com.sprd.runtime.Const.DEFAULT_BACKGROUND_DURATION;
import static com.sprd.runtime.Const.EXTRA_PHOTO_FILENAME;
import static com.sprd.runtime.Const.SETTINGS_ITEMS_SELECTED_ID;
import static com.sprd.runtime.Const.TEST_TYPE;
import static com.sprd.runtime.Const.VIBRATION_PRESET_SILENT;
import static com.sprd.runtime.Const.VIBRATION_PRESET_DURATION;
import static com.sprd.runtime.Const.VIBRATION_REPEATE;
import static com.sprd.runtime.Const.HOUR_TO_MILLSECOND_COUNT;
import static com.sprd.runtime.Const.AWAIT_EXIST_FROM_EXECUTOR_TIMEOUT;
import static com.sprd.runtime.Const.TEST_COUNT;
import static com.sprd.runtime.Const.TEST_DURATION;

/*
老化工作分配时间：
        (1)循环老化开始时播放声音
        (2)进入主菜单,7秒。
        (3)显示红色3秒。
        (4)显示绿色3秒
        (5)显示蓝色3秒
        (6)显示白色3秒
        (7)显示黑色3秒
        (8)使振动器5秒
        (9)停止振动器,
        (10)启动摄像头拍照
        (11)删除拍摄照片
        以上循环2 ~ 11步骤
*/

/**
 * Created by hefeng on 18-5-24.
 */

public class RuntimeTestStartActivity extends Activity/* BaseActivity*/ {
    private static final String TAG = "RuntimeTestStartActivity";
    private TextView mContent;

    private static final int[] LCD_COLOR_ARRAY = new int[] {
        Color.RED, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, //Color.YELLOW
    };

    private int[] mchecked_ids = null;                  //R.id.checkBox_refresh, R.id.checkBox_speaker, R.id.checkBox_vibrator, R.id.checkBox_camera
    private boolean REFRESH_SCREEN_FLAG = false;
    private boolean AUDIO_SPEAKER_FLAG = false;
    private boolean VIBRATION_FLAG = false;
    private boolean CAMERA_FLAG = false;

    private final int MSG_BEGIN_REFRESH_SCREEN = 0x0000;
    //private final int MSG_BEGIN_AUDIO_SPEAKER = 0x0001;
    //private final int MSG_BEGIN_VIBRATE = 0x0002;
    //private final int MSG_BEGIN_CAMERA = 0x0003;
    private final int MSG_BACKGROUND_SILENT = 0x0004;
    private final int MSG_ONE_TASK_OPERATION = 0x0005;
    private final int MSG_TOTAL_TASK_FINISHED = 0x0006;
    private final int MSG_REFRESH_SCREEN_FINISHED = 0x0007;
    private final int REQUEST_CODE_CAPTURE = 0x0000;

    private static final String STATUS = "status";
    private static final String PLUGGED = "plugged";
    //private static final String VOLTAGE = "voltage";
    private static final String HEALTH = "Good";
    private static final String BATTERY_HEALTH = "/sys/class/power_supply/battery/health";
    private static final String BATTERY_CHARGE = "/sys/class/power_supply/battery/stop_charge";

    private int lcdIndex = 0;

    private MediaPlayer mPlayer = null;
    private Vibrator mVibrator = null;
    private int backupMode = 0;

    private long startTime = 0;
    private long currentTime = 0;
    private long task_count = 0;

    public volatile boolean isStop = false;   // whether stop the thread
    private static boolean sIsRunninTestFinished = false;
    private static boolean sIsChargingStopped = false;

    private Object lockRefreshScreen = new Object();
    private Object lockCameraCapturing = new Object();

    private static final String ACTION_RUNTIME_TEST_START_AP_LOG = "com.sprd.runtime.start.ap.log";
    private static final String ACTION_RUNTIME_TEST_STOP_AP_LOG = "com.sprd.runtime.stop.ap.log";

    private Runnable audioRunnable = new Runnable() {
        @Override
        public void run() {
            doPlay();
        }
    };

    private Handler mUiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MSG_BACKGROUND_SILENT:
                    break;
                case MSG_BEGIN_REFRESH_SCREEN:
                    setLCDBackground(false);
                    lcdIndex++;
                    if (lcdIndex > LCD_COLOR_ARRAY.length - 1)
                    {
                        lcdIndex = 0;
                        mUiHandler.sendEmptyMessage(MSG_REFRESH_SCREEN_FINISHED);
                        return;
                    }
                    else
                    {
                        mUiHandler.sendEmptyMessageDelayed(MSG_BEGIN_REFRESH_SCREEN, DEFAULT_BACKGROUND_DURATION);
                    }
                    break;
                case MSG_REFRESH_SCREEN_FINISHED:
                    synchronized (lockRefreshScreen)
                    {
                        Log.d(TAG, "handleMessage: finished refreshing screen, unlocked.");
                        lockRefreshScreen.notify();
                    }
                    break;
                case MSG_TOTAL_TASK_FINISHED:
                    existFromExecutorService();
                    break;
                default:
                    break;
            }
            //super.handleMessage(msg);
        }
    };

    private Runnable refreshScreenRunnable = new Runnable() {
        @Override
        public void run() {
            //while (!isStop) { oops, it's a dead loop!!!
            if (!isStop){
                mUiHandler.sendEmptyMessageDelayed(MSG_BEGIN_REFRESH_SCREEN, DEFAULT_BACKGROUND_DURATION);

                try {
                    synchronized (lockRefreshScreen) {
                        Log.d(TAG, "run: refreshing screen, locked.");
                        lockRefreshScreen.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // the last background color duration 3s
                try {
                    Log.d(TAG, "run: refreshing screen, unlocked. begin sleep " + DEFAULT_BACKGROUND_DURATION/1000 + " seconds");
                    Thread.sleep(DEFAULT_BACKGROUND_DURATION);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "run: refreshing screen, unlocked. end sleep ");
            }
        }
    };

    private Runnable vibrateRunnable = new Runnable() {
        @Override
        public void run()
        {
            //while (!isStop) {
            if (!isStop){
                doVibrate();

                try {
                    Log.d(TAG, "run: vibrating ----------------------- sleeping for " + (VIBRATION_PRESET_SILENT + VIBRATION_PRESET_DURATION)/1000 + " seconds");
                    Thread.sleep(VIBRATION_PRESET_SILENT + VIBRATION_PRESET_DURATION);
                    Log.d(TAG, "run: vibrating ----------------------- sleeping finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Runnable cameraRunnable = new Runnable() {
        @Override
        public void run()
        {
            //while (!isStop) {
            if (!isStop){
                Intent intent = new Intent(RuntimeTestStartActivity.this, RuntimeFrontCameraTestActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CAPTURE);

                try {
                    synchronized (lockCameraCapturing) {
                        Log.d(TAG, "run: camera capturing, locked.");
                        lockCameraCapturing.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void startAPlog()
    {
        Log.d(TAG, "startAPlog: ");
        sendBroadcast(new Intent(ACTION_RUNTIME_TEST_START_AP_LOG));
    }

    private void stopAPlog()
    {
        Log.d(TAG, "stopAPlog: ");
        sendBroadcast(new Intent(ACTION_RUNTIME_TEST_STOP_AP_LOG));
    }

    private void existFromExecutorService()
    {
        Log.d(TAG, "existFromExecutorService: -----------------------");

        if (sIsRunninTestFinished)
        {
            stopAPlog();
        }

        if (se != null)
        {
            interruptActiveThread();
            Log.d(TAG, "existFromExecutorService: get se != null");
            startRuntimeResultActivity();
        }
    }

    private void interruptActiveThread()
    {
        Log.d(TAG, "interruptActiveThread: -------------------------- ");
        isStop = true;
    }

    private void printThreadStateInfo()
    {
        Log.d(TAG, "printThreadStateInfo: -------------------------- ");
        Log.d(TAG, "printThreadStateInfo: seThread.isAlive = " + seThread.isAlive() + ", seThread.isInterrupted() = " + seThread.isInterrupted());
    }

    private final Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    private SerialExecutor se = new SerialExecutor(executor);
    private Thread seThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!isStop) {
                if (!checkAcUsbCharge())
                {
                    mUiHandler.sendEmptyMessage(MSG_TOTAL_TASK_FINISHED);
                    isStop = true;
                    sIsRunninTestFinished = false;
                    break;
                }

                checkChargeState();

                task_count++;
                currentTime = System.currentTimeMillis();

                Log.d(TAG, "handleMessage: startTime = " + startTime + ", currentTime = " + currentTime + ", duration = " + (currentTime - startTime)/1000/60 +  " minutes, task_count = " + task_count + ", presetTime = " + getRuntimeDuration() + " hours.");

                if (currentTime - startTime >= /*2 * 60 *  1000)//*/getRuntimeDuration() * HOUR_TO_MILLSECOND_COUNT)
                {
                    mUiHandler.sendEmptyMessage(MSG_TOTAL_TASK_FINISHED);
                    isStop = true;
                    sIsRunninTestFinished = true;
                    break;
                }

                if (REFRESH_SCREEN_FLAG) {
                    if (se != null) {
                        se.addrun(refreshScreenRunnable);
                    }
                }

                if (VIBRATION_FLAG) {
                    if (se != null) {
                        se.addrun(vibrateRunnable);
                    }
                }

                if (CAMERA_FLAG) {
                    if (se != null) {
                        se.addrun(cameraRunnable);
                    }
                }

                if (se != null) {
                    se.scheduleNext();
                }
            }
        }
    });

    private void startRuntimeResultActivity()
    {
        Log.d(TAG, "startRuntimeResultActivity: -----------------------");
        Intent intent = new Intent(this, RuntimeTestResultActivity.class);
        intent.putExtra(TEST_COUNT, task_count);
        intent.putExtra(TEST_DURATION, (currentTime - startTime) / 1000 / 60);     // minutes
        intent.putExtra(TEST_TYPE, sIsRunninTestFinished);
        startActivity(intent);

        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (event.getKeyCode())
        {
            case KeyEvent.KEYCODE_BACK:
                sIsRunninTestFinished = false;
                existFromExecutorService();
                break;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setLCDBackground(boolean isDefault) {
        if (isDefault)
        {
            mContent.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
        }
        else
        {
            mContent.setBackgroundColor(LCD_COLOR_ARRAY[lcdIndex]);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mContent = new TextView(this);
        setContentView(mContent);
        setTitle(R.string.name_aging_tesing);
        printThreadStateInfo();

        if (!checkAcUsbCharge())
        {
            finish();
            return;
        }

        getCheckboxFlag();
        setLCDBackground(true);

        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        backupMode = audioManager.getMode();
        setAudio();
        startAPlog();

        //IntentFilter filter = new IntentFilter();
        //filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        //registerReceiver(mBroadcastReceiver, filter);

        if (AUDIO_SPEAKER_FLAG)
        {
             mUiHandler.post(audioRunnable);
        }

        if (REFRESH_SCREEN_FLAG)
        {
            mUiHandler.sendEmptyMessageDelayed(MSG_BACKGROUND_SILENT, DEFAULT_BACKGROUND_SILENT);
        }

        startTime = System.currentTimeMillis();
        //createChildThread();
        seThread.start();
    }

    private int getRuntimeDuration()
    {
        int hourCount;

        switch (RuntimeTestConfig.getCurrentPreset())
        {
            case 0:
                hourCount = 96;
                break;
            case 1:
                hourCount = 48;
                break;
            case 2:
                hourCount = 24;
                break;
            case 3:
                hourCount = 16;
                break;
            case 4:
                hourCount = 8;
                break;
            case 5:
                hourCount = 4;
                break;
            case 6:
                hourCount = 2;
                break;
            default:
                hourCount = 2;
                break;
        }

        return hourCount;
    }

    private void getCheckboxFlag()
    {
        mchecked_ids = getIntent().getExtras().getIntArray(SETTINGS_ITEMS_SELECTED_ID);

        Log.d(TAG, "fengfeng:  mchecked_ids = " + mchecked_ids.length);

        for (int i=0; i<mchecked_ids.length; ++i)
        {
            Log.d(TAG, "fengfeng: mchecked_ids[" +i + "]" + mchecked_ids[i]);

            switch (mchecked_ids[i])
            {
                case R.id.checkBox_refresh:
                    REFRESH_SCREEN_FLAG = true;
                    break;
                case R.id.checkBox_speaker:
                    AUDIO_SPEAKER_FLAG = true;
                    break;
                case R.id.checkBox_vibrator:
                    VIBRATION_FLAG = true;
                    break;
                case R.id.checkBox_camera:
                    CAMERA_FLAG = true;
                    break;
                default:
                    break;
            }
        }
    }

    private void doVibrate() {
        vibrateCreate();
        mVibrator.vibrate(new long[]{VIBRATION_PRESET_SILENT, VIBRATION_PRESET_DURATION}, VIBRATION_REPEATE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                return true;
            default:
                return true;
        }
    }

    private void playerCreate()
    {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
    }

    private void playerRelease()
    {
        if (mPlayer == null) {
            return;
        }
        else {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void playerPause()
    {
        if (mPlayer != null)
        {
            if (mPlayer.isPlaying())
            {
                mPlayer.pause();
            }
        }
    }

    private void playerResume()
    {
        if (mPlayer != null)
        {
            if (!mPlayer.isPlaying())
            {
                mPlayer.start();
            }
        }
    }

    private void vibrateCreate()
    {
        if (mVibrator != null) {
            mVibrator.cancel();
            mVibrator = null;
        }

        if(mVibrator == null)
        {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void vibrateCancel()
    {
        if (mVibrator == null)
        {
            return;
        }
        else
        {
            mVibrator.cancel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        vibrateCancel();
        playerPause();

        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(backupMode);
    }

    @Override
    protected void onResume() {
        super.onResume();

        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        backupMode = audioManager.getMode();

        setLCDBackground(true);

        playerResume();
    }

    @Override
    public void onDestroy() {
        mUiHandler.removeCallbacks(audioRunnable);
        //unregisterReceiver(mBroadcastReceiver);
        restartCharging();
        playerRelease();
        printThreadStateInfo();
        super.onDestroy();
    }

    private static void setDataSourceFromResource(Resources resources,
                                                  MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);

        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    private void doPlay() {
        playerCreate();

        try {
            //mPlayer.setDataSource(mFilePaths.get(audioNumber));
            //setDataSourceFromResource(this.getResources(), mPlayer, R.raw.autumn);
            setDataSourceFromResource(this.getResources(), mPlayer, R.raw.test_emmc);
            mPlayer.prepare();
        } catch (IllegalArgumentException e) {
            /*SPRD: fix bug350197 setDataSource fail due to crash @{*/
//            mPlayer = null;
            /* @}*/
            e.printStackTrace();
        } catch (IllegalStateException e) {
//            mPlayer = null;
            e.printStackTrace();
        } catch (IOException e) {
//            mPlayer = null;
            e.printStackTrace();
        }
        mPlayer.start();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (AudioSystem.DEVICE_STATE_AVAILABLE == AudioSystem.getDeviceConnectionState(
                AudioManager.DEVICE_OUT_EARPIECE, "")) {
            audioManager.setMode(AudioManager.MODE_RINGTONE);//modi for SPCSS00160783
        }
        mPlayer.setVolume(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mPlayer.setLooping(true);
        //mVibrator.vibrate(V_TIME);
        //mCurrentActionText.setText(getResources().getText(com.sprd.validationtools.R.string.melody_play_tag));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE && resultCode == Activity.RESULT_OK)
        {
            String fileAbsolutePath = data.getStringExtra(EXTRA_PHOTO_FILENAME);
            File file = new File(fileAbsolutePath);

            Log.d(TAG, "onActivityResult: filename = " + fileAbsolutePath + ", file.exists() = " + file.exists() + ", file.isFile() = " + file.isFile());

            if (file.exists() && file.isFile())
            {
                if (!file.delete())
                {
                    Log.d(TAG, "onActivityResult: delete file" + fileAbsolutePath + " failed.");
                }
                else
                {
                    Log.d(TAG, "onActivityResult: delete file" + fileAbsolutePath + " success.");
                    //mUiHandler.sendEmptyMessage(MSG_ONE_TASK_OPERATION);
					synchronized (lockCameraCapturing)
                    {
                        Log.d(TAG, "onActivityResult: camera capturing finished, unlocked.");
                        lockCameraCapturing.notify();
                    }
                }
            }
            else
            {
                Log.d(TAG, "onActivityResult: file" + fileAbsolutePath + " not existed or is directory.");
            }
        }
    }

    private boolean checkAcUsbCharge()
    {
        boolean isCharger = checkCharger();
        boolean isUSBPluged = checkUSB();

        Log.d(TAG, "checkAcUsbCharge: isCharger = " + isCharger + ", isUSBPluged = " + isUSBPluged);

        if(!isCharger && !isUSBPluged)
        {
            Toast.makeText(this, R.string.charged_not_connected_msg, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void checkChargeState()
    {
        int batteryPercent = checkBatteryPercent();
        boolean isCharger = checkAcUsbCharge();       // AC charge or USB charge

        Log.d(TAG, "checkChargeState: batteryPercent = " + batteryPercent + ", sIsChargingStopped = " + sIsChargingStopped + ", isCharger = " + isCharger);

        if (!sIsChargingStopped && batteryPercent > CHARGING_STOP_BATTERY_PERCENT)
        {
            if (isCharger)
            {
                Log.d(TAG, "checkChargeState: stop charging-----------------------------------");
                sIsChargingStopped = true;
                stopBatteryCharge();
            }
        }
        else if (sIsChargingStopped && batteryPercent < CHARGING_STOP_BATTERY_PERCENT)
        {
            if (isCharger)
            {
                Log.d(TAG, "checkChargeState: start charging-----------------------------------");
                sIsChargingStopped = false;
                startBatteryCharge();
            }
        }
        else
        {
            //
        }
    }

    private void restartCharging()
    {
        boolean isCharger = checkAcUsbCharge();       // AC charge or USB charge

        Log.d(TAG, "restartCharging: isCharger = " + isCharger + ", sIsChargingStopped = " + sIsChargingStopped);

        if(isCharger && sIsChargingStopped)
        {
            Log.d(TAG, "restartCharging: start charging-----------------------------------");
            sIsChargingStopped = false;
            startBatteryCharge();
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: action = " + action);

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(STATUS, 0);
                int plugged = intent.getIntExtra(PLUGGED, 0);
                //int voltage = intent.getIntExtra(VOLTAGE, 0);
/*
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                    case BatteryManager.BATTERY_STATUS_FULL:
                        break;
                    default:
                        break;
                }
*/
                Log.d(TAG, " plugged = " + plugged);
                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        //startBatteryCharge();
                        break;
                    default:
                        //stopBatteryCharge();
                        break;
                }
            }
        }
    };

    public int checkBatteryPercent()
    {
        int percent = 0;

        File localFile = new File("/sys/class/power_supply/battery/capacity");
        boolean bresult = false;
        //int percent = 0;
        try
        {
            FileReader localFileReader = new FileReader(localFile);
            char[] arrayOfChar = new char[30];
            try
            {
                String[] arrayOfString = new String(arrayOfChar, 0, localFileReader.read(arrayOfChar)).trim().split("\n");
                Log.i(TAG, "checkBatteryPercent: percent = " + arrayOfString[0]);

                //can do run time test only when battery percent >= 40%
                percent = Integer.parseInt(arrayOfString[0]);
            }
            catch (IOException localIOException)
            {
                Log.e(TAG, "checkBatteryPercent: read battery-capacity file err!");
            }
        }
        catch (FileNotFoundException localFileNotFoundException)
        {
            Log.e(TAG, "checkBatteryPercent: get battery capacity file err!");
        }
        /*
        BatteryManager batteryManager = (BatteryManager)getSystemService(BATTERY_SERVICE);
        percent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        Log.d(TAG, "getCurrentBatteryCapacity: battery = " + percent);
		*/
        return percent;
    }

    private void startBatteryCharge()
    {
        String health = readFile(BATTERY_HEALTH);

        if (!"readError".equals(health)) {
            Log.d(TAG, "startBatteryCharge: headth = " + health);

            if (HEALTH.equals(health)) {
                boolean success = writeFile("0", BATTERY_CHARGE);
                Log.d(TAG, "startBatteryCharge: write success = " + success);

                if (success) {
                    health = readFile(BATTERY_HEALTH);
                    Log.d(TAG, "startBatteryCharge: read health = " + health);

                    if (!HEALTH.equals(health)) {
                        Toast.makeText(this,"startBatteryCharge Write Charge Fail", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this,"startBatteryCharge Write Charge Error", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "startBatteryCharge Read Health Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopBatteryCharge()
    {
        String health = readFile(BATTERY_HEALTH);

        if (!"readError".equals(health)) {
            Log.d(TAG, "stopBatteryCharge: health = " + health);

            if (HEALTH.equals(health)) {
                boolean success = writeFile("1", BATTERY_CHARGE);
                Log.d(TAG, "stopBatteryCharge: write success = " + success);

                if (success) {
                    health = readFile(BATTERY_HEALTH);
                    Log.d(TAG, "stopBatteryCharge: read health = " + health);

                    if (!HEALTH.equals(health)) {
                        Toast.makeText(this,"stopBatteryCharge Write Charge Fail", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this,"stopBatteryCharge Write Charge Error", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "stopBatteryCharge Read Health Error", Toast.LENGTH_SHORT).show();
        }
    }

    private String readFile(String path) {
        File file = new File(path);
        String str = new String("");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                str = str + line;
            }
        } catch (Exception e) {
            Log.d(TAG, "Read file error!!!");
            str = "readError";
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        Log.d(TAG, str.trim());
        return str.trim();
    }

    public boolean writeFile(String str, String path) {
        Log.d(TAG, "str->" + str + " path->" + path);
        boolean writeSuccess = true;
        FileOutputStream out = null;
        PrintStream p = null;
        try {
            out = new FileOutputStream(path);
            p = new PrintStream(out);
            p.print(str);
        } catch (Exception e) {
            writeSuccess = false;
            Log.d(TAG, "Write file error!!!");
            e.printStackTrace();
        } finally {
            if (p != null) {
                try {
                    p.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        return writeSuccess;
    }

    public boolean checkCharger()
    {
        File localFile = new File("/sys/class/power_supply/ac/online");// new File("/sys/class/power_supply/battery/status");
        boolean bresult = false;
        try
        {
            FileReader localFileReader = new FileReader(localFile);
            char[] arrayOfChar = new char[30];
            try
            {
                String[] arrayOfString = new String(arrayOfChar, 0, localFileReader.read(arrayOfChar)).trim().split("\n");
                Log.i(TAG, "checkCharger: online = " + arrayOfString[0]);

                //can do run time test only when AC charger is on.
                if (!"1".equals(arrayOfString[0]))
                {
                    bresult  = false;
                }
                else
                {
                    bresult = true;
                }
            }
            catch (IOException localIOException)
            {
                Log.e(TAG, "checkCharger: read ac-online file err!");
            }
        }
        catch (FileNotFoundException localFileNotFoundException)
        {
            Log.e(TAG, "checkCharger: get battery status file err!");
        }

        return bresult;
    }

    public boolean checkUSB()
    {
        File localFile = new File("/sys/class/power_supply/usb/online");
        boolean bresult = false;
        try
        {
            FileReader localFileReader = new FileReader(localFile);
            char[] arrayOfChar = new char[30];
            try
            {
                String[] arrayOfString = new String(arrayOfChar, 0, localFileReader.read(arrayOfChar)).trim().split("\n");
                Log.i(TAG, "checkUSB: online = " + arrayOfString[0]);

                //can do run time test only when AC charger is on.
                if (!"1".equals(arrayOfString[0]))
                {
                    bresult  = false;
                }
                else
                {
                    bresult = true;
                }
            }
            catch (IOException localIOException)
            {
                Log.e(TAG, "checkUSB: read ac-online file err!");
            }
        }
        catch (FileNotFoundException localFileNotFoundException)
        {
            Log.e(TAG, "checkUSB: get battery status file err!");
        }

        return bresult;
    }

    private void setAudio() {
        AudioManager mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_DTMF, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_DTMF), 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION), 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_RING), 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_SYSTEM), 0);
    }

    /*
    public static final Executor THREAD_POOL_EXECUTOR;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 4));
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE_SECONDS = 30;
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                sPoolWorkQueue, sThreadFactory);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        THREAD_POOL_EXECUTOR = threadPoolExecutor;
    }

    private static class SerialExecutor implements Executor {
        final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
        Runnable mActive;

        public synchronized void execute(final Runnable r) {
            mTasks.offer(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext();
                    }
                }
            });
            if (mActive == null) {
                scheduleNext();
            }
        }

        protected synchronized void scheduleNext() {
            if ((mActive = mTasks.poll()) != null) {
                THREAD_POOL_EXECUTOR.execute(mActive);
            }
        }
    }

    public static final Executor SERIAL_EXECUTOR = new SerialExecutor();
*/
}
