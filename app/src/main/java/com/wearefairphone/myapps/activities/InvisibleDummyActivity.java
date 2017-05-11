/*
 * Copyright (C) 2013 Fairphone Project
 * Copyright (C) 2017 Jannis Pinter
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
package com.wearefairphone.myapps.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.wearefairphone.myapps.R;

public class InvisibleDummyActivity extends Activity {

    private boolean canFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invisible);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.permission_dialog_title));
        builder.setMessage(getString(R.string.permission_dialog_message));
        builder.setCancelable(false);
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        canFinish = true;
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        startActivity(intent);

                    }
                });
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Only call finish() when user clicked on OK in the previous dialog
        // Otherwise we will crash
        if (canFinish) {
            finish();
        }
    }
}
