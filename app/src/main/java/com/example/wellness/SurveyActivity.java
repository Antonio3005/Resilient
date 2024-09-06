package com.example.wellness;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SurveyActivity extends AppCompatActivity {

    private RadioGroup[] questionGroups;

    private ImageView home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        home= findViewById(R.id.home);

        goHome();

        // Definire le domande e le opzioni di risposta
        questionGroups = new RadioGroup[]{
                findViewById(R.id.question1_group),
                //findViewById(R.id.question2_group),
                //findViewById(R.id.question3_group),
                //findViewById(R.id.question4_group)
        };

        // Associare il pulsante di invio alla logica per salvare le risposte
        findViewById(R.id.submit_button).setOnClickListener(this::onSubmitButtonClicked);
    }

    public void onSubmitButtonClicked(View view) {
        String[] responses = new String[4];
        for (int i = 0; i < questionGroups.length; i++) {
            int selectedId = questionGroups[i].getCheckedRadioButtonId();
            RadioButton selectedRadioButton = findViewById(selectedId);

            if (selectedRadioButton == null) {
                Toast.makeText(this, "Rispondi a tutte le domande", Toast.LENGTH_SHORT).show();
                return;
            }

            responses[i] = selectedRadioButton.getText().toString();  // Risposta alla domanda
        }

        saveSurveyResults(responses);  // Salva le risposte

        Toast.makeText(this, "Questionario completato", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void saveSurveyResults(String[] responses) {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if(!documentsDir.exists()) {
            documentsDir.mkdirs();
            Log.d("folder:", documentsDir.getAbsolutePath());
        }
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String sessionPrefix = date + "_Session";

        File sessionDir = null;
        boolean newSessionAllowed = true;
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = 60 * 60 * 1000;


        for(int i=1; i<=3; i++) {
            sessionDir = new File(documentsDir, sessionPrefix + i);

            if (!sessionDir.exists()) {
                if (isLastSessionOlderThanOneHour(documentsDir, sessionPrefix, currentTime, oneHourInMillis)) {
                    Log.d("bool", String.valueOf(sessionDir));
                    sessionDir.mkdirs();
                } else {
                    newSessionAllowed = false;

                }
                break;
            } else if (sessionDir.isDirectory() && !containsCsv(sessionDir)) {
                // La cartella esiste e non contiene immagini
                break;
            }

        }

        if (sessionDir == null || (sessionDir.exists() && sessionDir.list() != null && containsCsv(sessionDir)) || !newSessionAllowed) {
            Log.d("session", String.valueOf(sessionDir));
            Toast.makeText(this, "Numero massimo di sessioni per oggi raggiunto, tutte le cartelle piene o meno di un'ora dall'ultima sessione.", Toast.LENGTH_SHORT).show();
            return;
        }

        File csvFile = new File(sessionDir, "dati.csv");

        try (FileWriter csvWriter = new FileWriter(csvFile, true)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());

            // Salva il timestamp e le risposte
            csvWriter.append(timestamp);  // Aggiungi il timestamp
            csvWriter.append(",");
            csvWriter.append(responses[0]);
            /*for (String response : responses) {
                csvWriter.append(",").append(response);  // Aggiungi le risposte
            }*/
            csvWriter.append("\n");  // Vai a capo

            csvWriter.flush();  // Assicurati che i dati siano scritti
            csvWriter.close();
            Toast.makeText(this, "Risposte salvate in: " + csvFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("error", e.toString());
            Toast.makeText(this, "Errore nel salvataggio dell'immagine", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isLastSessionOlderThanOneHour(File documentsDir, String sessionPrefix, long currentTime, long oneHourInMillis) {
        for (int i = 3; i >= 1; i--) {
            File sessionDir = new File(documentsDir, sessionPrefix + i);
            if (sessionDir.exists()) {
                long lastModified = sessionDir.lastModified();
                if ((currentTime - lastModified) < oneHourInMillis) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean containsCsv(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file.isFile() && isCsvFile(file)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCsvFile(File file) {
        String fileExtensions = "csv";
        String fileName = file.getName().toLowerCase();
        if(fileName.endsWith(fileExtensions)) {
            return true;
        }
        return false;
    }

    public void goHome() {
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SurveyActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}