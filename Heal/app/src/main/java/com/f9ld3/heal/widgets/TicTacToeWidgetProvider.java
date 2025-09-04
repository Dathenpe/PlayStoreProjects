package com.f9ld3.heal.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.f9ld3.heal.MainActivity;
import com.f9ld3.heal.R;

public class TicTacToeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tic_tac_toe_layout);

            // Create an Intent to launch MainActivity and then load the Tic-Tac-Toe Game Fragment
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction("ACTION_LAUNCH_GAME");
            intent.putExtra("game_fragment_id", R.id.nav_tic_tac_toe); // Use the nav ID for Tic-Tac-Toe
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            views.setOnClickPendingIntent(R.id.tic_tac_toe_icon, pendingIntent);
            views.setOnClickPendingIntent(R.id.tic_tac_toe_title, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}