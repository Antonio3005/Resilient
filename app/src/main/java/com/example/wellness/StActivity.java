package com.example.wellness;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class StActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager=null;
    private Sensor stepCounterSensor;
    private int backupSteps = 0;

    private ImageView home;

    private static int previewTotalSteps = 0;

    //private static int saveSteps = 0;

    private Button button;

    //private static boolean isReset = false;

    private TextView stepsCount;

    private CardView steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps);
        home = findViewById(R.id.home);

        goHome();

        steps = findViewById(R.id.steps_counter);
        stepsCount = findViewById(R.id.TV_STEPS);


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //loadData();

        //saveStepsToCSV();

    }

    protected void onResume() {
        super.onResume();
        if (stepCounterSensor == null) {
            Toast.makeText(this, "This device has no sensor", Toast.LENGTH_LONG).show();

        } else {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void onPause() {
        super.onPause();
        //sensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int newTotalSteps = (int) event.values[0];
            loadData();  // Carica i dati salvati

            Log.d("steps", String.valueOf(newTotalSteps));

            if (newTotalSteps < backupSteps) {
                previewTotalSteps += newTotalSteps;
                backupSteps = newTotalSteps;
            } else {
                previewTotalSteps += (newTotalSteps - backupSteps);
                backupSteps = newTotalSteps;
            }


            // Aggiorna l'interfaccia utente con il conteggio dei passi totali
            stepsCount.setText(String.valueOf(previewTotalSteps));
            saveData();

        }
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        int savedNumber = sharedPreferences.getInt("key1", 0);
        Log.d("saved", String.valueOf(savedNumber));
        previewTotalSteps = savedNumber;
        backupSteps = sharedPreferences.getInt("backup",0);
        Log.d("saved", String.valueOf(backupSteps));
    }

    private void resetAll() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("key1",0);
        editor.putInt("backup",0);
        editor.apply();
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.d("preview", String.valueOf(previewTotalSteps));
        editor.putInt("key1",previewTotalSteps);
        editor.putInt("backup",backupSteps);
        editor.apply();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void goHome() {
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    public static void saveStepsAsJson() {
        //saveSteps = previewTotalSteps;
        try{
            MainActivity.fileLock.lock();
            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            // Assicurati che la cartella esista
            if (!documentsDir.exists()) {
                documentsDir.mkdirs();  // Crea la cartella se non esiste
            }

            String dt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            String sessionPrefix = dt + "_Session";

            File sessionDir = null;

            for(int i=1; i<=3; i++) {
                sessionDir = new File(documentsDir,sessionPrefix+i);
                // Crea il percorso per il file CSV
                if(sessionDir.exists()) {
                    File csvFile = new File(sessionDir, "dati.csv");
                    try (FileWriter csvWriter = new FileWriter(csvFile, true)) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String timestamp = sdf.format(new Date());  // Ottieni il timestamp corrente

                        csvWriter.append(timestamp).append(",").append(String.valueOf(previewTotalSteps)).append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } finally {
            // Rilascia il lock dopo aver completato l'operazione di scrittura
            MainActivity.fileLock.unlock();
        }
    }
}