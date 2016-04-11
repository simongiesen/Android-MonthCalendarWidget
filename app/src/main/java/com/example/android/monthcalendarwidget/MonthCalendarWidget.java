/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.monthcalendarwidget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class MonthCalendarWidget extends AppWidgetProvider {

    private static final String ACTION_PREVIOUS_MONTH = "com.example.android.monthcalendarwidget.action.PREVIOUS_MONTH";
    private static final String ACTION_NEXT_MONTH = "com.example.android.monthcalendarwidget.action.NEXT_MONTH";
    private static final String ACTION_RESET_MONTH = "com.example.android.monthcalendarwidget.action.RESET_MONTH";

    private static final String PREF_MONTH = "month";
    private static final String PREF_YEAR = "year";

    @Override
    public void onUpdate (Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    private void redrawWidgets (Context context) {
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, MonthCalendarWidget.class));

        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive (Context context, Intent intent) {
        super.onReceive(context, intent);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Calendar calendar = Calendar.getInstance();

        int thisMonth = preferences.getInt(PREF_MONTH, calendar.get(Calendar.MONTH));
        int thisYear = preferences.getInt(PREF_YEAR, calendar.get(Calendar.YEAR));

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.MONTH, thisMonth);
        calendar.set(Calendar.YEAR, thisYear);

        switch (intent.getAction()) {
            case ACTION_PREVIOUS_MONTH: {
                calendar.add(Calendar.MONTH, -1);

                break;
            }

            case ACTION_NEXT_MONTH: {
                calendar.add(Calendar.MONTH, 1);

                break;
            }

            case ACTION_RESET_MONTH:
                preferences.edit()
                        .remove(PREF_MONTH)
                        .remove(PREF_YEAR).apply();

                redrawWidgets(context);

                return;
        }

        preferences.edit()
                .putInt(PREF_MONTH, calendar.get(Calendar.MONTH))
                .putInt(PREF_YEAR, calendar.get(Calendar.YEAR))
                .apply();

        redrawWidgets(context);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void onAppWidgetOptionsChanged (Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        drawWidget(context, appWidgetId);
    }

    private void drawWidget (Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Resources resources = context.getResources();
        Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        boolean shortMonthName = false;
        boolean mini = false;
        int numWeeks = 6;

        if (widgetOptions != null) {
            int minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            shortMonthName = minWidthDp <= resources.getInteger(R.integer.max_width_short_month_label_dp);
            mini = minHeightDp <= resources.getInteger(R.integer.max_height_mini_view_dp);

            if (mini) {
                numWeeks = minHeightDp <= resources.getInteger(R.integer.max_height_mini_view_1_row_dp) ? 1 : 2;
            }
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_YEAR);
        int todayYear = calendar.get(Calendar.YEAR);
        int thisMonth;

        if (!mini) {
            thisMonth = preferences.getInt(PREF_MONTH, calendar.get(Calendar.MONTH));

            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.MONTH, thisMonth);
            calendar.set(Calendar.YEAR, preferences.getInt(PREF_YEAR, calendar.get(Calendar.YEAR)));
        } else {
            thisMonth = calendar.get(Calendar.MONTH);
        }

        String mouthLabelFormat = calendar.get(Calendar.YEAR) == todayYear
                ? shortMonthName ? "LLL" : "LLLL"
                : shortMonthName ? "LLL yy" : "LLLL yyyy";
        views.setTextViewText(R.id.month_label, DateFormat.format(mouthLabelFormat, calendar));

        if (!mini) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, 1 - calendar.get(Calendar.DAY_OF_WEEK));
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, 1 - calendar.get(Calendar.DAY_OF_WEEK));
        }

        views.removeAllViews(R.id.calendar);

        RemoteViews headerRowViews = new RemoteViews(context.getPackageName(), R.layout.row_header);
        String[] weekdays = DateFormatSymbols.getInstance().getShortWeekdays();

        for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
            RemoteViews dayViews = new RemoteViews(context.getPackageName(), R.layout.cell_header);
            dayViews.setTextViewText(android.R.id.text1, weekdays[day]);
            headerRowViews.addView(R.id.row_container, dayViews);
        }

        views.addView(R.id.calendar, headerRowViews);

        for (int week = 0; week < numWeeks; week++) {
            RemoteViews rowViews = new RemoteViews(context.getPackageName(), R.layout.row_week);

            for (int day = 0; day < 7; day++) {
                boolean inYear = calendar.get(Calendar.YEAR) == todayYear;
                boolean inMonth = calendar.get(Calendar.MONTH) == thisMonth;
                boolean isToday = inYear && inMonth && calendar.get(Calendar.DAY_OF_YEAR) == today;

                boolean isFirstOfMonth = calendar.get(Calendar.DAY_OF_MONTH) == 1;
                int cellLayoutResId = R.layout.cell_day;

                if (isToday) {
                    cellLayoutResId = R.layout.cell_today;
                } else if (inMonth) {
                    cellLayoutResId = R.layout.cell_day_this_month;
                }

                RemoteViews cellViews = new RemoteViews(context.getPackageName(), cellLayoutResId);
                cellViews.setTextViewText(android.R.id.text1, Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));

                if (isFirstOfMonth) {
                    cellViews.setTextViewText(R.id.month_label, DateFormat.format("MMM", calendar));
                }

                rowViews.addView(R.id.row_container, cellViews);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            views.addView(R.id.calendar, rowViews);
        }

        views.setViewVisibility(R.id.prev_month_button, mini ? View.GONE : View.VISIBLE);
        views.setOnClickPendingIntent(R.id.prev_month_button, PendingIntent.getBroadcast(context, 0,
                new Intent(context, MonthCalendarWidget.class).setAction(ACTION_PREVIOUS_MONTH), PendingIntent.FLAG_UPDATE_CURRENT));
        views.setViewVisibility(R.id.next_month_button, mini ? View.GONE : View.VISIBLE);
        views.setOnClickPendingIntent(R.id.next_month_button, PendingIntent.getBroadcast(context, 0,
                new Intent(context, MonthCalendarWidget.class).setAction(ACTION_NEXT_MONTH), PendingIntent.FLAG_UPDATE_CURRENT));
        views.setOnClickPendingIntent(R.id.month_label, PendingIntent.getBroadcast(context, 0,
                new Intent(context, MonthCalendarWidget.class).setAction(ACTION_RESET_MONTH), PendingIntent.FLAG_UPDATE_CURRENT));
        views.setViewVisibility(R.id.month_bar, numWeeks <= 1 ? View.GONE : View.VISIBLE);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

}
