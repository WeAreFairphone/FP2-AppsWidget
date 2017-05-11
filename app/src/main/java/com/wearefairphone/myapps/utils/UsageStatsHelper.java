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
package com.wearefairphone.myapps.utils;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.wearefairphone.myapps.appinfo.ApplicationRunInformation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Helper class to gather required data for standalone version of MyApps widget.
 *
 * @author Jannis Pinter
 */

public class UsageStatsHelper {

    private static final int USAGE_STATS_QUERY_TIME_FRAME = 604800000;
    private static final int NO_LAUNCH_COUNT_FOUND = -1;
    private static final String TAG = UsageStatsHelper.class.getSimpleName();


    /**
     * Checks whether or not we have permission to access the usage statistics
     * @param context Application context we are running in.
     * @return {@code true}, when permission is granted, otherwise {@code false}.
     */
    public boolean hasPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());

        List<ApplicationRunInformation> usageStats = getUsageStats(context);
        return usageStats.size() != 0 && mode == AppOpsManager.MODE_ALLOWED;

    }

    /**
     * Returns a list of usage statistics for recently used apps
     * @param context Application context we are running in.
     * @return {@code List<ApplicationRunInformation>} with usage statistics of recently used apps.
     */
    public List<ApplicationRunInformation> getUsageStats(Context context) {
        List<ApplicationRunInformation> usageStats = new ArrayList<>();

        final UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - USAGE_STATS_QUERY_TIME_FRAME, time);

        for (UsageStats stats : queryUsageStats) {
            final PackageManager pm = context.getApplicationContext().getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(stats.getPackageName());
            if (launchIntent != null) {
                ApplicationRunInformation appInfo = getApplicationRunInformation(stats, launchIntent.getComponent());
                usageStats.add(appInfo);
            }
        }

        return usageStats;
    }

    @NonNull
    private ApplicationRunInformation getApplicationRunInformation(UsageStats stats, ComponentName componentName) {
        int launchCount = getLaunchCount(stats);

        if (launchCount == NO_LAUNCH_COUNT_FOUND) {
            // This is an ugly hack, since we cannot get the amount of launches for an app.
            // Currently we abuse the total time in foreground and make sure it is not exceeding an int.
            launchCount = stats.getTotalTimeInForeground() > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE : (int) stats.getTotalTimeInForeground();
        }

        ApplicationRunInformation appInfo = new ApplicationRunInformation(componentName, launchCount);
        appInfo.setLastExecution(new Date(stats.getLastTimeUsed()));
        return appInfo;
    }

    /**
     * Try to access launch count via reflection
     * @param stats UsageStats of certain app
     * @return determined launch count of app
     */
    private int getLaunchCount(UsageStats stats) {
        try {
            Field mLaunchCount = UsageStats.class.getDeclaredField("mLaunchCount");
            int launchCount = (Integer) mLaunchCount.get(stats);

            Log.d(TAG, "Launch count for " + stats.getPackageName() + ": " + launchCount);

            return launchCount;
        } catch (NoSuchFieldException e) {
            Log.d(TAG, "Could not find field", e);
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Could not access field", e);
        }

        return NO_LAUNCH_COUNT_FOUND;
    }
}
