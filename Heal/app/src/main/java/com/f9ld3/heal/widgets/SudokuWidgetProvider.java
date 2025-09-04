package com.f9ld3.heal.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

public class SudokuWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_sudoku_layout);

            // Create an Intent to launch MainActivity and then load the Sudoku Game Fragment
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction("ACTION_LAUNCH_GAME");
            intent.putExtra("game_fragment_id", R.id.nav_sudoku); // Use the nav ID for Sudoku
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            views.setOnClickPendingIntent(R.id.sudoku_icon, pendingIntent);
            views.setOnClickPendingIntent(R.id.sudoku_title, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}