package com.example.wellness;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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
                findViewById(R.id.question2_group),
                findViewById(R.id.question3_group),
                findViewById(R.id.question4_group)
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

        if (!documentsDir.exists()) {
            documentsDir.mkdirs();  // Crea la cartella se necessario
        }

        File csvFile = new File(documentsDir, "dati.csv");  // Percorso del file CSV

        try (FileWriter csvWriter = new FileWriter(csvFile, true)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());

            // Salva il timestamp e le risposte
            csvWriter.append(timestamp);  // Aggiungi il timestamp
            for (String response : responses) {
                csvWriter.append(",").append(response);  // Aggiungi le risposte
            }
            csvWriter.append("\n");  // Vai a capo

            csvWriter.flush();  // Assicurati che i dati siano scritti
        } catch (IOException e) {
            e.printStackTrace();
        }
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