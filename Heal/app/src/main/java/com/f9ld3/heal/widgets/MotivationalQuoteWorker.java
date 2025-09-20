package com.f9ld3.heal.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.f9ld3.heal.R;

import java.util.Random;

public class MotivationalQuoteWorker extends Worker {

    private Context context;

    public MotivationalQuoteWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String[] quotes = context.getResources().getStringArray(R.array.motivational_quotes_widget);
        String randomQuote = quotes[new Random().nextInt(quotes.length)];

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.motivational_widget_layout);
        views.setTextViewText(R.id.motivational_quote_textview, randomQuote);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MotivationalWidgetProvider.class);
        appWidgetManager.updateAppWidget(thisWidget, views);

        return Result.success();
    }
}