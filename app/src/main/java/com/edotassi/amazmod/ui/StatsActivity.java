package com.edotassi.amazmod.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import amazmod.com.transport.Constants;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationEntity_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.concurrent.Callable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class StatsActivity extends AppCompatActivity {

    @BindView(R.id.activity_stats_main_container)
    View statsMainContainer;
    @BindView(R.id.activity_stats_progress)
    MaterialProgressBar materialProgressBar;
    @BindView(R.id.activity_stats_notifications_last_hour)
    TextView notificationsLastHour;
    @BindView(R.id.activity_stats_notifications_24_hours)
    TextView notificationsLast24Hours;
    @BindView(R.id.activity_stats_notifications_total)
    TextView notificationsTotal;

    @BindView(R.id.activity_stats_open_notifications_log)
    Button openNotificationsLogButton;

    private final byte[] ALLOWED_FILTERS = {Constants.FILTER_CONTINUE,
                                            Constants.FILTER_UNGROUP,
                                            Constants.FILTER_VOICE,
                                            Constants.FILTER_MAPS,
                                            Constants.FILTER_LOCALOK};

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException exception) {
            //TODO log to crashlitics
        }

        getSupportActionBar().setTitle(R.string.stats);
        ButterKnife.bind(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        loadStats();
    }

    @SuppressLint("CheckResult")
    @OnClick(R.id.activity_stats_open_notifications_log)
    public void openLog() {
        startActivity(new Intent(this, NotificationsLogActivity.class));
    }

    @SuppressLint("CheckResult")
    private void loadStats() {
        materialProgressBar.setVisibility(View.VISIBLE);
        statsMainContainer.setVisibility(View.GONE);

        Flowable
                .fromCallable(new Callable<StatsResult>() {
                    @Override
                    public StatsResult call() throws Exception {

                        long total = SQLite
                                .selectCountOf()
                                .from(NotificationEntity.class)
                                .count();

                        long anHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
                        long aDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

                        long totalAnHourAgo = 0L;
                        long totalADayAgo = 0L;
                        long sum;

                        for (byte f: ALLOWED_FILTERS) {
                            sum = SQLite
                                    .selectCountOf()
                                    .from(NotificationEntity.class)
                                    .where(NotificationEntity_Table.date.greaterThan(anHourAgo))
                                    .and(NotificationEntity_Table.filterResult.eq(f))
                                    .count();
                            totalAnHourAgo += sum;
                        }

                        for (byte f: ALLOWED_FILTERS) {
                            sum = SQLite
                                    .selectCountOf()
                                    .from(NotificationEntity.class)
                                    .where(NotificationEntity_Table.date.greaterThan(aDayAgo))
                                    .and(NotificationEntity_Table.filterResult.eq(f))
                                    .count();
                            totalADayAgo += sum;
                        }

                        StatsResult result = new StatsResult();

                        result.setNotificationsTotal(total);
                        result.setNotificationsTotalADayAgo(totalADayAgo);
                        result.setNotificationsTotalAnHourAgo(totalAnHourAgo);

                        return result;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .subscribe(new Consumer<StatsResult>() {
                    @Override
                    public void accept(final StatsResult result) throws Exception {
                        StatsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notificationsTotal.setText(String.valueOf(result.getNotificationsTotal()));
                                notificationsLastHour.setText(String.valueOf(result.getNotificationsTotalAnHourAgo()));
                                notificationsLast24Hours.setText(String.valueOf(result.getNotificationsTotalADayAgo()));

                                materialProgressBar.setVisibility(View.GONE);
                                statsMainContainer.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
    }

    private class StatsResult {
        private long notificationsTotal;
        private long notificationsTotalAnHourAgo;
        private long notificationsTotalADayAgo;

        public long getNotificationsTotal() {
            return notificationsTotal;
        }

        public void setNotificationsTotal(long notificationsTotal) {
            this.notificationsTotal = notificationsTotal;
        }

        public long getNotificationsTotalAnHourAgo() {
            return notificationsTotalAnHourAgo;
        }

        public void setNotificationsTotalAnHourAgo(long notificationsTotalAnHourAgo) {
            this.notificationsTotalAnHourAgo = notificationsTotalAnHourAgo;
        }

        public long getNotificationsTotalADayAgo() {
            return notificationsTotalADayAgo;
        }

        public void setNotificationsTotalADayAgo(long notificationsTotalADayAgo) {
            this.notificationsTotalADayAgo = notificationsTotalADayAgo;
        }
    }
}
