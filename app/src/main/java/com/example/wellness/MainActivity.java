package com.example.wellness;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private CardView camera;
    private CardView survey;
    private CardView app;
    private CardView steps;

    private ImageView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_notifications);

        camera=findViewById(R.id.camera_card);
        steps=findViewById(R.id.steps_card);
        app=findViewById(R.id.app_card);
        survey=findViewById(R.id.survey_card);
        info=findViewById(R.id.info_button);

        setupNotifyAlarm();
        updateAlarm();

        openStepsActivity();
        openAppsActivity();
        openSurveyActivity();
        openCameraActivity();
        openInfo();

    }

    private void openCameraActivity() {
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ShowPhotoActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openStepsActivity() {
        steps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, StepsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openAppsActivity() {
        app.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AppActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openSurveyActivity() {
        survey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
                startActivity(intent);
            }
        });
    }

    private void openInfo() {
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupNotifyAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, NotifyReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long triggerTime = calendar.getTimeInMillis();
        if (System.currentTimeMillis() > triggerTime) {
            triggerTime += 24 * 60 * 60 * 1000;  // Se l'ora è passata, aggiungi un giorno
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, 24 * 60 * 60 * 1000, pendingIntent);  // Allarme giornaliero
    }

    private void updateAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, UpdateReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 58);
        calendar.set(Calendar.SECOND, 0);

        long triggerTime = calendar.getTimeInMillis();
        if (System.currentTimeMillis() > triggerTime) {
            triggerTime += 24 * 60 * 60 * 1000;  // Se l'ora è passata, aggiungi un giorno
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, 24 * 60 * 60 * 1000, pendingIntent);  // Allarme giornaliero
    }
}