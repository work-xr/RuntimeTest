/*
 * Copyright (C) 2013 Spreadtrum Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sprd.engineermode.debuglog.slogui;

import static com.sprd.engineermode.debuglog.slogui.SlogAction.ACTION_CLEARLOG_COMPLETED;
import static com.sprd.engineermode.debuglog.slogui.SlogAction.ACTION_DUMPLOG_COMPLETED;
import static com.sprd.engineermode.debuglog.slogui.SlogAction.EXTRA_CLEAR_RESULT;
import static com.sprd.engineermode.debuglog.slogui.SlogAction.EXTRA_DUMP_RESULT;
import static com.sprd.engineermode.debuglog.slogui.SlogAction.SLOG_COMMAND_RETURN_OK;
import static com.sprd.engineermode.debuglog.slogui.SlogService.ACTION_SCREEN_SHOT;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import android.util.Log;

import com.sprd.engineermode.R;

public class SlogUIReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        Log.d("fengfeng", "runtime SlogUIReceiver ------------------------------------ action = " + intent.getAction());
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            context.getApplicationContext().startService(
                    new Intent(Intent.ACTION_BOOT_COMPLETED)
                            .setClass(context.getApplicationContext(),
                            SlogService.class));
            return;
        }
        if (ACTION_SCREEN_SHOT.equals(intent.getAction())) {
            SlogAction.snap(context.getApplicationContext());
            return;
        }
        if (ACTION_CLEARLOG_COMPLETED.equals(intent.getAction())) {
            boolean success = SLOG_COMMAND_RETURN_OK == intent.getIntExtra(
                    EXTRA_CLEAR_RESULT, -1);
            Toast.makeText(
                    context,
                    context.getString(success ? R.string.clear_action_successed
                            : R.string.clear_action_failed), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        if (ACTION_DUMPLOG_COMPLETED.equals(intent.getAction())) {
            boolean success = SLOG_COMMAND_RETURN_OK == intent.getIntExtra(
                    EXTRA_DUMP_RESULT, -1);
            Toast.makeText(
                    context,
                    context.getString(success ? R.string.dump_action_successed
                            : R.string.dump_action_failed), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        
        if ("com.sprd.runtime.start.ap.log".equals(intent.getAction())) {
			Log.d("fengfeng", "runtime the android log opend------------------------------------");
            SlogAction.setState(SlogAction.ANDROIDKEY, true);
            return;
        }
        
        if ("com.sprd.runtime.stop.ap.log".equals(intent.getAction())) {
			Log.d("fengfeng", "runtime the android log closed------------------------------------");
            SlogAction.setState(SlogAction.ANDROIDKEY, false);
            return;
        }
    }

}
