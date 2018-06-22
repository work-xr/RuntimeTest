package com.sprd.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import static com.sprd.runtime.Const.NV_TESTED_FLAG_RUNTIME_ORDER;
import static com.sprd.runtime.Const.SETTINGS_ITEMS_SELECTED_ID;


public class RuntimeTestMainActivity extends Activity implements View.OnClickListener{
    private static String TAG = "RuntimeTestMainActivity";

    private Context mContext = null;
    private int [] mchecked_ids = null;
    private Button mbutton_run;
    private Button mbutton_setting;
    private PhaseCheckParse mTestedNVFlag;
    private Spinner mSpinner_preset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_new);
        
        mTestedNVFlag = new PhaseCheckParse();
        mContext = getApplicationContext();
        
        RuntimeTestConfig.initConfig(mContext);

        initViews();
    }

    @Override
    public void onResume(){
        super.onResume();
        
        TextView textview = (TextView)findViewById(R.id.textView_runin);
        if(textview == null)
        {
            return ;
        }
        
        if(mSpinner_preset == null)
        {
        	mSpinner_preset = (Spinner)findViewById(R.id.presetSpinnerInMain);
        	presetSprinnerInit();
        } else {
        	mSpinner_preset.setSelection(RuntimeTestConfig.getCurrentPreset()); 
        }
        
        String disp = getResources().getString(R.string.runtime_result);
        String resultOK = getResources().getString(R.string.button_pass);
        String resultFail = getResources().getString(R.string.button_fail);

        mTestedNVFlag = new PhaseCheckParse();
        //int runin_flag = mTestedNVFlag.readTestedFlag();
        int runin_flag = mTestedNVFlag.testPhaseCheck(NV_TESTED_FLAG_RUNTIME_ORDER);//station runin 9
        Log.d(TAG,"onResume:runin_flag:" + runin_flag);
        
        if(runin_flag == 0) //not tested
        {
            textview.setText(disp + resultFail);
        }
        else if(runin_flag ==1 )//tested success
        {
            textview.setText(disp + resultOK);
        }
        else if(runin_flag == 2) //tested failed.
        {
            textview.setText(disp + resultFail);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initViews()
    {
        mbutton_run = (Button)findViewById(R.id.button_run);
        mbutton_run.setOnClickListener(this);
        mbutton_setting = (Button)findViewById(R.id.button_setting);
        mbutton_setting.setOnClickListener(this);		
 
        mSpinner_preset = (Spinner)findViewById(R.id.presetSpinnerInMain);
        presetSprinnerInit();
    }
    
    public void onClick(View arg0) {

        if(arg0 == mbutton_run)
        {
            mchecked_ids = RuntimeTestConfig.getCheckedIds();
            if(mchecked_ids != null && mchecked_ids.length > 0)            
            {
                RuntimeTestConfig.saveCurrentPreset();

                Intent intent = new Intent(mContext, RuntimeTestStartActivity.class);
                intent.putExtra(SETTINGS_ITEMS_SELECTED_ID, mchecked_ids);
                startActivity(intent);
            }
        }
        else if(arg0 == mbutton_setting)
        {
            Intent intent = new Intent(this, RuntimeTestSettingActivity.class);
            startActivity(intent); 
        }        
    }

    private void presetSprinnerInit() {
        String[] presetList = getResources().getStringArray(R.array.preset_list);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.preset_spinner_item, presetList);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner_preset.setAdapter(adapter);

        mSpinner_preset.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                RuntimeTestConfig.setCurrentPreset(position);
            }

            public void onNothingSelected(AdapterView<?> parent) {
                //do something
            }
        });

        mSpinner_preset.setSelection(RuntimeTestConfig.getCurrentPreset());
    }

}
