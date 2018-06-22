package com.sprd.runtime;

import android.graphics.Color;

/**
 * Created by hefeng on 18-5-31.
 */

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
public class Const {

    public static final int DEFAULT_BACKGROUND_COLOR = Color.GRAY;  // default background color
    public static final int DEFAULT_BACKGROUND_SILENT = 3000;       // enter main menu silent 7s
    public static final int DEFAULT_BACKGROUND_DURATION = 3000;     // duration of changing background

    public static final int VIBRATION_PRESET_SILENT = 500;          // start vibrate after changing background
    public static final int VIBRATION_PRESET_DURATION = 5000;       // duration of vibration
    public static final int VIBRATION_REPEATE = -1;                 // -1: once  0: infinite

    public static final long HOUR_TO_MILLSECOND_COUNT = 1 * 60 * 60 * 1000;
    public static final int AWAIT_EXIST_FROM_EXECUTOR_TIMEOUT = 3000;   // abort the threads before they finished
    //private int AWAIT_EXIST_FROM_EXECUTOR_TIMEOUT = DEFAULT_BACKGROUND_DURATION * 5;   // the longest duration of the thread, abort the threads after they finished, would cause fatal exception
    public static final String TEST_COUNT = "test_count";
    public static final String TEST_DURATION = "test_duration";
    public static final String TEST_TYPE = "test_type";

    public static final String FIRST_TIME_ENTER_FLAG = "first_time_enter_flag";             // whether the first time enter
    public static final int  TIME_DURATION_INIT_PRESET = 5;                                 // which time duration selected  4h?
    public static final String TIME_DURATION_INIT_FLAG = "time_duration_init_flag";         // time duration init flag

    public static final String SETTINGS_ITEMS_SELECTED_NUM = "settings_items_selected_num"; // num of selected items
    public static final String SETTINGS_ITEMS_SELECTED_ID = "settings_items_selected_id";   // ids of selected items

    public static final String EXTRA_PHOTO_FILENAME = "photo";
    public static final int AFTER_SLEEP_AUTO_CAPTURE_DURATION = 5000;   // sleep sometime for capturing picture automatically

    public static final int NV_TESTED_FLAG_RUNTIME_ORDER = 8;           // runtime tested flag index

    public static final int CHARGING_STOP_BATTERY_PERCENT = 80;         // stop charging when battery percent larger than 80
}
