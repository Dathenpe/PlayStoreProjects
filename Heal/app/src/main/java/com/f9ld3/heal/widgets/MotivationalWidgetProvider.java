package com.f9ld3.heal.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MotivationalWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Schedule a periodic worker to update the widget
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                MotivationalQuoteWorker.class, 24, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);
    }
}