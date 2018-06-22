package com.sprd.runtime;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.sprd.runtime.Const.NV_TESTED_FLAG_RUNTIME_ORDER;
import static com.sprd.runtime.Const.TEST_COUNT;
import static com.sprd.runtime.Const.TEST_DURATION;
import static com.sprd.runtime.Const.TEST_TYPE;

/**
 * Created by hefeng on 18-5-28.
 */

public class RuntimeTestResultActivity extends Activity {

    private Button btnPass;
    private Button btnFail;
    private TextView tvTestCount;
    private TextView tvTestDuration;
    private Long testCount;
    private Long testDuration;
    private PhaseCheckParse mTestedNVFlag;
    private boolean isTestFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);

        mTestedNVFlag = new PhaseCheckParse();

        testCount = getIntent().getLongExtra(TEST_COUNT, -1);
        tvTestCount = (TextView)findViewById(R.id.tv_test_count);
        tvTestCount.setText("count: " + testCount.toString() + " times");

        testDuration = getIntent().getLongExtra(TEST_DURATION, -1);
        tvTestDuration = (TextView)findViewById(R.id.tv_test_duration);
        tvTestDuration.setText("duration: " + testDuration.toString() + " minutes");

        isTestFinished = getIntent().getBooleanExtra(TEST_TYPE, false);

        if (!isTestFinished)
        {
            Toast.makeText(this, getString(R.string.runtime_finished_failed), Toast.LENGTH_SHORT).show();
            mTestedNVFlag.writeStationTested(NV_TESTED_FLAG_RUNTIME_ORDER);
            mTestedNVFlag.writeStationFail(NV_TESTED_FLAG_RUNTIME_ORDER);
            finish();
        }
        else
        {
            btnPass = (Button) findViewById(R.id.btn_pass);
            btnPass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTestedNVFlag.writeStationTested(NV_TESTED_FLAG_RUNTIME_ORDER);
                    mTestedNVFlag.writeStationPass(NV_TESTED_FLAG_RUNTIME_ORDER);
                    finish();
                }
            });
            btnFail = (Button) findViewById(R.id.btn_fail);
            btnFail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTestedNVFlag.writeStationTested(NV_TESTED_FLAG_RUNTIME_ORDER);
                    mTestedNVFlag.writeStationFail(NV_TESTED_FLAG_RUNTIME_ORDER);
                    finish();
                }
            });
        }
    }
}
