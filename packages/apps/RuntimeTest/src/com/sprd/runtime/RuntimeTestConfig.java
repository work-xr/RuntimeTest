package com.sprd.runtime;

import android.content.Context;
import android.content.res.Resources;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;

import java.util.ArrayList;

import static com.sprd.runtime.Const.FIRST_TIME_ENTER_FLAG;
import static com.sprd.runtime.Const.SETTINGS_ITEMS_SELECTED_NUM;
import static com.sprd.runtime.Const.TIME_DURATION_INIT_FLAG;
import static com.sprd.runtime.Const.TIME_DURATION_INIT_PRESET;

public class RuntimeTestConfig {

    private static String TAG = "RuntimeTestConfig";

    private static Context mContext = null;
    private static Resources mResources = null;
    private static boolean mbInited = false;
    private static int mCurrent_preset = 0;     //default preset is 2h (96h, 48h, 24h, 16h, 8h, 4h, 2h)
    
    private static ArrayList<Integer> mCaseFlag;
    private static int [] mCheckboxID = new int[]{
            R.id.checkBox_refresh,R.id.checkBox_speaker,R.id.checkBox_vibrator,
            R.id.checkBox_camera};
    
    public static Context getContext()
    {
        return mContext;
    }
    

    public static void initConfig(Context context) {
        
        mContext = context;
        mResources = mContext.getResources();

        mCurrent_preset = TIME_DURATION_INIT_PRESET;//mResources.getInteger(R.integer.def_current_preset);
        mCaseFlag = loadIntegerArray(mResources, R.array.case_test_flag);
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sp.edit();
        mbInited = sp.getBoolean(FIRST_TIME_ENTER_FLAG, false);

        if(mbInited == false)
        {
            editor.putBoolean(FIRST_TIME_ENTER_FLAG, true);
            editor.putInt(TIME_DURATION_INIT_FLAG, mCurrent_preset);

            setCheckedIds(getInitialCheckedIds());
            editor.commit();
        }
        else
        {
            mCurrent_preset = restoreCurrentPreset();
        }
    }

    public static void setCurrentPreset(int curPreset) {
         mCurrent_preset = curPreset;     
    }
    
    public static int getCurrentPreset() {
         return mCurrent_preset;     
    }

    public static void saveCurrentPreset(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(TIME_DURATION_INIT_FLAG , mCurrent_preset);
        editor.commit();
    }

    public static int restoreCurrentPreset(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getInt(TIME_DURATION_INIT_FLAG, TIME_DURATION_INIT_PRESET);
    }

    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<Integer>(size);

        for (int i = 0; i < size; i++) {
            list.add(vals[i]);
        }

        return list;
    }

    public static void setCheckedIds(int [] checked_ids)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

        if(checked_ids.length > 0)
        {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(SETTINGS_ITEMS_SELECTED_NUM, checked_ids.length);
            
            for(int index = 0; index < checked_ids.length; index ++)
            {
                editor.putInt("checked_id" + index, checked_ids[index]);
            }
            
            editor.commit();
        }
    }

    public static void setCheckedIdsEmpty(int [] checked_ids)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);

        if(checked_ids.length >= 0)
        {
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(SETTINGS_ITEMS_SELECTED_NUM, checked_ids.length);

            for(int index = 0; index < checked_ids.length; index ++)
            {
                editor.putInt("checked_id" + index, 0);
            }

            editor.commit();
        }

    }

    public static int[] getCheckedIds()
    {
        int [] checked_ids = null;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        int chcked_num = sp.getInt(SETTINGS_ITEMS_SELECTED_NUM, 0);

        if(chcked_num > 0)
        {
            checked_ids = new int[chcked_num];
            for(int index = 0; index < chcked_num; index ++)
            {
                checked_ids[index] = sp.getInt("checked_id" + index, 0);
            }
        }
        
        return checked_ids;
    }

    public static int[] getInitialCheckedIds()
    {
        int [] checked_ids = null;    	
           	
        //first get count
        int chcked_num = 0;

        for(int i = 0; i < mCaseFlag.size(); i++) {
        	int flag = mCaseFlag.get(i);
        	if(flag == 1){
        		chcked_num++; 
        	}
        }     	
				
		//second get checked ids.
        if(chcked_num > 0)
        {
        	int index = 0;
            checked_ids = new int[chcked_num];

            for(int i = 0; i < mCaseFlag.size(); i ++)
            {
            		int flag = mCaseFlag.get(i);
            		if(flag == 1) {
		                checked_ids[index++] = mCheckboxID[i];
		                Log.i(TAG, "getInitialCheckedIds i:" + i);
            		}
            }
            
            Log.i(TAG, "getInitialCheckedIds checked_ids length:" + checked_ids.length);
        }
        
        return checked_ids;
    }
}
