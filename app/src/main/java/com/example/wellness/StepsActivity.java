package com.example.wellness;

import android.annotation.SuppressLint;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class StepsActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager=null;
    private Sensor stepCounterSensor;
    private int totalSteps = 0;

    private ImageView home;

    private int previewTotalSteps = 0;

    private Button button;

    private TextView stepsCount;

    private CardView steps;


    @SuppressLint("MissingInflatedId")
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
        loadData();

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
        sensorManager.unregisterListener(this);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int newTotalSteps = (int) event.values[0];

            // Controllo se il dispositivo è stato riavviato
            if (newTotalSteps < previewTotalSteps) {
                // Il dispositivo è stato riavviato, resetta l'anteprima dei passi
                previewTotalSteps = newTotalSteps;
                stepsCount.setText(String.valueOf(0));
            } else {
                // Calcola i passi correnti solo se il valore del sensore è maggiore o uguale
                int currentSteps = newTotalSteps - previewTotalSteps;
                stepsCount.setText(String.valueOf(currentSteps));
            }
            saveData();

        }
    }

    public static void sData(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepCounterSensor != null) {
            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {

                    if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                        // Ottieni il numero di passi rilevati dall'evento
                        int steps = (int)event.values[0];
                        SharedPreferences sharedPreferences = context.getSharedPreferences("myPref", Context.MODE_PRIVATE);
                        int presteps = sharedPreferences.getInt("key1",0);
                        int current_steps;
                        if(steps < presteps) {
                            presteps = steps;
                            current_steps = 0;
                        } else {
                            current_steps = steps-presteps;
                        }

                        Log.d("StepCounter", "Passi rilevati: " + current_steps);

                        try{
                            MainActivity.fileLock.lock();
                            File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

                            // Assicurati che la cartella esista
                            if (!documentsDir.exists()) {
                                documentsDir.mkdirs();  // Crea la cartella se non esiste
                            }

                            // Crea il percorso per il file CSV
                            File csvFile = new File(documentsDir, "dati.csv");

                            try (FileWriter csvWriter = new FileWriter(csvFile, true)) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String timestamp = sdf.format(new Date());  // Ottieni il timestamp corrente

                                // Scrivi il timestamp e il numero di passi nel CSV

                                csvWriter.append(timestamp).append(",").append(String.valueOf(current_steps)).append("\n");
                                presteps = steps;
                                resetSteps(context,presteps,steps);
                                sensorManager.unregisterListener(this);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            // Rilascia il lock dopo aver completato l'operazione di scrittura
                            MainActivity.fileLock.unlock();
                        }
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    // Ignora questo metodo per il sensore di contapassi
                }
            }, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // Il dispositivo non supporta il sensore di contapassi
            Log.e("StepCounter", "Il dispositivo non supporta il sensore di contapassi");
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Ignora per il sensore di contapassi
    }


    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.d("preview", String.valueOf(previewTotalSteps));
        editor.putInt("key1",previewTotalSteps);
        editor.apply();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        int savedNumber = sharedPreferences.getInt("key1", 0);
        Log.d("saved", String.valueOf(savedNumber));
        previewTotalSteps = savedNumber;

    }

    /*public void saveStepsToCSV() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Ottieni il percorso della cartella "Documents"
                File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

                // Assicurati che la cartella esista
                if (!documentsDir.exists()) {
                    documentsDir.mkdirs();  // Crea la cartella se non esiste
                }

                // Crea il percorso per il file CSV
                File csvFile = new File(documentsDir, "dati.csv");

                try (FileWriter csvWriter = new FileWriter(csvFile, true)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String timestamp = sdf.format(new Date());  // Ottieni il timestamp corrente

                    int steps = 0;
                    steps= totalSteps - previewTotalSteps;

                    // Scrivi il timestamp e il numero di passi nel CSV
                    csvWriter.append(timestamp).append(",").append(String.valueOf(steps)).append("\n");
                    previewTotalSteps = totalSteps;
                    resetSteps();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /*public static void resetSteps() {
        previewTotalSteps = totalSteps;  // Aggiorna l'anteprima
        //stepsCount.setText("0");  // Mostra zero come valore resettato
        saveData();  // Salva i nuovi dati resettati
    }*/

    public static void resetSteps(Context context,int presteps, int totsteps) {
        //stepsCount.setText("0");  // Mostra zero come valore resettato
        // Salva i nuovi dati resettati
        SharedPreferences sharedPreferences = context.getSharedPreferences("myPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Log.d("preview", String.valueOf(presteps));
        editor.putInt("key1", presteps);
        editor.putInt("key2", totsteps);
        editor.apply();
    }



    public void goHome() {
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StepsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

}
