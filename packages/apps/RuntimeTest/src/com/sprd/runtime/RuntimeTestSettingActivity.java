package com.sprd.runtime;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import static com.sprd.runtime.RuntimeTestConfig.setCheckedIdsEmpty;

public class RuntimeTestSettingActivity extends Activity implements View.OnClickListener{
    private static final String TAG = "RuntimeTestSettingActivity";

    private int [] mcheckbox_id = new int[]{
            R.id.checkBox_refresh,
            R.id.checkBox_speaker,
            R.id.checkBox_vibrator,
            R.id.checkBox_camera};
    
    private CheckBox[] mcheckbox_list = new CheckBox[mcheckbox_id.length];
    private int mchecked_num = 0;
            
    private Button mbutton_clearall;
    private Button mbutton_selectall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_new);
        initViews();
    }
    
    private void initViews()
    {
        mbutton_clearall = (Button)findViewById(R.id.button_clearall);
        mbutton_clearall.setOnClickListener(this);
        
        mbutton_selectall = (Button)findViewById(R.id.button_selectall);
        mbutton_selectall.setOnClickListener(this);

        int[] checkbox_ids = RuntimeTestConfig.getCheckedIds();
        
        for(int index = 0; index < mcheckbox_id.length; index ++)
        {
        	mcheckbox_list[index] = (CheckBox)findViewById(mcheckbox_id[index]);
        	mcheckbox_list[index].setChecked(false);
        	
        	for(int i = 0; i < checkbox_ids.length; i++){
        		if(mcheckbox_id[index] == checkbox_ids[i]) {
        			mcheckbox_list[index].setChecked(true);
        			mchecked_num++;
        			break;
        		}
        	}
            
            mcheckbox_list[index].setOnClickListener(this);
        }
    }

    public void onClick(View arg0) {
        if(arg0 == mbutton_clearall) //clear all
        {
            mchecked_num = 0;
            mbutton_clearall.setEnabled(false);
            for(int index = 0; index < mcheckbox_id.length; index ++)
            {
                mcheckbox_list[index].setChecked(false);
            }

            saveCheckedIDS();
        }
        else if(arg0 == mbutton_selectall) //select all
        {
            mchecked_num = mcheckbox_id.length;
            mbutton_clearall.setEnabled(true);
            for(int index =0; index < mcheckbox_id.length; index ++)
            {
                mcheckbox_list[index].setChecked(true);
            }

            saveCheckedIDS();
        }        
        else //checkbox,update mbutton_run/mbutton_clearall's state and display text
        {           
            CheckBox checkbox = (CheckBox)arg0;
            if(checkbox.isChecked() == false)
            {
                mchecked_num--;
            }
            else
            {
                mchecked_num++;
            }
            
            saveCheckedIDS();
            mbutton_clearall.setEnabled((mchecked_num >0)?true:false);
        }
    }
    
    @Override
    protected void onDestroy( ) {
    	super.onDestroy();
    }
    
    private void saveCheckedIDS( ) {
    	
        if(mchecked_num > 0){
        	
        	int[] checked_ids = new int[mchecked_num];
        	int selectedIndex = 0;
        	
            for(int index =0; index < mcheckbox_list.length; index++)
            {
            	if(mcheckbox_list[index].isChecked()){
            		checked_ids[selectedIndex++] = mcheckbox_id[index];
            	}            	
            }
            
            RuntimeTestConfig.setCheckedIds(checked_ids);
            
            Log.d(TAG, "mchecked_num: " + mchecked_num);
            Log.d(TAG, "selectedIndex: " + selectedIndex);
        }
        else
        {
            setCheckedIdsEmpty(mcheckbox_id);
        }
    }
}