package com.example.wellness;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.tv.BroadcastInfoRequest;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AppActivity extends AppCompatActivity {

    private TextView ig;

    private Button button;
    private ImageView home;
    private TextView tg;
    private TextView wa;
    private TextView yt;
    private TextView tt;

    private TextView fb;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        ig = findViewById(R.id.instagram_tx);
        tg = findViewById(R.id.telegram_tx);
        wa = findViewById(R.id.whatsapp_tx);
        yt = findViewById(R.id.youtube_tx);
        tt = findViewById(R.id.tiktok_tx);
        fb = findViewById(R.id.facebook_tx);
        home = findViewById(R.id.home);

        goHome();

        if (hasUsageStatsPermission()) {
            // Ottieni le statistiche e aggiornare l'interfaccia utente
            //List<UsageStats> usageStats = getUsageStats();
            //UsageStatsAdapter adapter = new UsageStatsAdapter(usageStats);
            //usageStatsRecyclerView.setAdapter(adapter);
            LocalDate today = LocalDate.now(); // Ottieni la data corrente
            getDailyStats(today);
            //saveAppUsageToCSV();
            //getUsageStats();
        } else {
            // Richiedi l'autorizzazione se non è concessa
            requestUsageStatsPermission();
        }

    }



    private boolean hasUsageStatsPermission() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    private void getUsageStats() {


        // Intervallo di tempo (ultime 24 ore)
        long endTime = System.currentTimeMillis();
        //long startTime = endTime - (1000 * 60 * 60 * 24); // 24 ore
        //Log.d("Start time", String.valueOf(startTime));
        Log.d("End time", String.valueOf(endTime));

        // Imposta il calendario a mezzanotte del giorno corrente
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Prendi il tempo da mezzanotte
        long startTime = calendar.getTimeInMillis();

        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);
        Log.d("Start time", String.valueOf(startTime));
        Log.d("End time", String.valueOf(endTime));
        Log.d("Start time", String.valueOf(startDate));
        Log.d("End time", String.valueOf(endDate));

        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        List<UsageStats> usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        SharedPreferences sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();


        if (usageStats != null && !usageStats.isEmpty()) {
            for (UsageStats stats : usageStats) {

                //Log.d("AppUsageTracker", "Package: " + stats.getPackageName() + " - Time used: " + stats.getTotalTimeInForeground() + "timestamp " + stats.getLastTimeStamp());
                if (stats.getPackageName().equals("com.instagram.android")) {
                    ig.setText(convertMillisToHHMMSS(stats.getTotalTimeInForeground()));
                    editor.putLong("instagram", stats.getTotalTimeInForeground());
                    //usare SharedPreferences
                } else if (stats.getPackageName().equals("org.telegram.messenger")) {
                    tg.setText(convertMillisToHHMMSS(stats.getTotalTimeInForeground()));
                    editor.putLong("telegram", stats.getTotalTimeInForeground());
                }  else if (stats.getPackageName().equals("com.whatsapp")) {
                    wa.setText(convertMillisToHHMMSS(stats.getTotalTimeInForeground()));
                    Log.d("AppUsage", "WhatsApp Time used: " + stats.getTotalTimeInForeground());
                    editor.putLong("whatsapp", stats.getTotalTimeInForeground());
                } else if (stats.getPackageName().equals("com.google.android.youtube")) {
                    yt.setText(convertMillisToHHMMSS(stats.getTotalTimeInForeground()));
                    editor.putLong("youtube", stats.getTotalTimeInForeground());
                    Log.d("AppUsage", "YouTube Time used: " + stats.getTotalTimeInForeground());
                } else if (stats.getPackageName().equals("com.zhiliaoapp.musically")) {
                    tt.setText(convertMillisToHHMMSS(stats.getTotalTimeInForeground()));
                    editor.putLong("tiktok", stats.getTotalTimeInForeground());
                    Log.d("AppUsage", "TikTok Time used: " + stats.getTotalTimeInForeground());
                } else if (stats.getPackageName().equals("com.facebook.katana")) {
                    fb.setText(convertMillisToHHMMSS(stats.getTotalTimeInForeground()));
                    editor.putLong("facebook", stats.getTotalTimeInForeground());
                    Log.d("AppUsage", "Facebook Time used: " + stats.getTotalTimeInForeground());
                }

            }

        } else {
            Log.d("AppUsageTracker", "No usage stats found.");

        }

        editor.apply();

    }

    public static String convertMillisToHHMMSS(long millis) {
        long totalSeconds = millis / 1000;  // Converti in secondi
        long hours = totalSeconds / 3600;  // Calcola le ore
        long remainder = totalSeconds % 3600;  // Resto dopo le ore
        long minutes = remainder / 60;  // Calcola i minuti
        long seconds = remainder % 60;  // Calcola i secondi rimanenti

        return String.format("%02dh%02dm%02ds", hours, minutes, seconds);  // Formatta in "hh:mm:ss"
    }


    public void saveAppUsageToCSV() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                    getUsageStats();
                    csvWriter.append(timestamp).append(",").append(ig.getText()).append(",").append(tg.getText()).append(",")
                            .append(wa.getText()).append(",").append(yt.getText()).append(",").append(tt.getText()).append(",")
                            .append(fb.getText()).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void goHome() {
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AppActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    public static class Stat {
        public String packageName;
        public long totalTime;
        public List<ZonedDateTime> startTimes;

        public Stat(String packageName, long totalTime, List<ZonedDateTime> startTimes) {
            this.packageName = packageName;
            this.totalTime = totalTime;
            this.startTimes = startTimes;
        }

        public Stat(String packageName, long totalTime) {
            this.packageName = packageName;
            this.totalTime = totalTime;
        }


        public String getPackageName() {
            return packageName;
        }

        public long getTotalTime() {
            return totalTime;
        }
    }

    public void getDailyStats(LocalDate date) {


        ZoneId utc = ZoneId.of("UTC");
        ZoneId defaultZone = ZoneId.systemDefault();

        ZonedDateTime startDate = date.atStartOfDay(defaultZone).withZoneSameInstant(utc);
        long start = startDate.toInstant().toEpochMilli();
        long end = startDate.plusDays(1).toInstant().toEpochMilli();

        Map<String, List<UsageEvents.Event>> sortedEvents = new HashMap<>();
        UsageStatsManager usageManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents systemEvents = usageManager.queryEvents(start, end); // `usageManager` needs initialization
        while (systemEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            systemEvents.getNextEvent(event);

            sortedEvents
                    .computeIfAbsent(event.getPackageName(), k -> new ArrayList<>())
                    .add(event);
        }


        for (Map.Entry<String, List<UsageEvents.Event>> entry : sortedEvents.entrySet()) {
            String packageName = entry.getKey();
            List<UsageEvents.Event> events = entry.getValue();

            long startTime = 0L;
            long endTime = 0L;
            long totalTime = 0L;
            List<ZonedDateTime> startTimes = new ArrayList<>();

            for (UsageEvents.Event event : events) {
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    startTime = event.getTimeStamp();
                    startTimes.add(Instant.ofEpochMilli(startTime).atZone(utc).withZoneSameInstant(defaultZone));
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    endTime = event.getTimeStamp();
                }

                if (startTime == 0L && endTime != 0L) {
                    startTime = start;
                }

                if (startTime != 0L && endTime != 0L) {
                    totalTime += endTime - startTime;
                    startTime = 0L;
                    endTime = 0L;
                }
            }

            if (startTime != 0L && endTime == 0L) {
                totalTime += end - 1000 - startTime;
            }


            if (packageName.equals("com.instagram.android")) {
                ig.setText(convertMillisToHHMMSS(totalTime));
                //usare SharedPreferences
            } else if (packageName.equals("org.telegram.messenger")) {
                tg.setText(convertMillisToHHMMSS(totalTime));
            }  else if (packageName.equals("com.whatsapp")) {
                wa.setText(convertMillisToHHMMSS(totalTime));
            } else if (packageName.equals("com.google.android.youtube")) {
                yt.setText(convertMillisToHHMMSS(totalTime));
            } else if (packageName.equals("com.zhiliaoapp.musically")) {
                tt.setText(convertMillisToHHMMSS(totalTime));
            } else if (packageName.equals("com.facebook.katana")) {
                fb.setText(convertMillisToHHMMSS(totalTime));
            }

        }


    }

    public static void saveData(Context context,LocalDate date) {
        Log.d("Sono qui", date.toString());
        ZoneId utc = ZoneId.of("UTC");
        ZoneId defaultZone = ZoneId.systemDefault();

        ZonedDateTime startDate = date.atStartOfDay(defaultZone).withZoneSameInstant(utc);
        long start = startDate.toInstant().toEpochMilli();
        long end = startDate.plusDays(1).toInstant().toEpochMilli();

        Map<String, List<UsageEvents.Event>> sortedEvents = new HashMap<>();
        UsageStatsManager usageManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents systemEvents = usageManager.queryEvents(start, end); // `usageManager` needs initialization
        while (systemEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            systemEvents.getNextEvent(event);

            sortedEvents
                    .computeIfAbsent(event.getPackageName(), k -> new ArrayList<>())
                    .add(event);
        }

        List<Stat> stats = new ArrayList<>();

        for (Map.Entry<String, List<UsageEvents.Event>> entry : sortedEvents.entrySet()) {
            String packageName = entry.getKey();
            List<UsageEvents.Event> events = entry.getValue();

            long startTime = 0L;
            long endTime = 0L;
            long totalTime = 0L;
            List<ZonedDateTime> startTimes = new ArrayList<>();

            for (UsageEvents.Event event : events) {
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    startTime = event.getTimeStamp();
                    startTimes.add(Instant.ofEpochMilli(startTime).atZone(utc).withZoneSameInstant(defaultZone));
                } else if (event.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    endTime = event.getTimeStamp();
                }

                if (startTime == 0L && endTime != 0L) {
                    startTime = start;
                }

                if (startTime != 0L && endTime != 0L) {
                    totalTime += endTime - startTime;
                    startTime = 0L;
                    endTime = 0L;
                }
            }

            if (startTime != 0L && endTime == 0L) {
                totalTime += end - 1000 - startTime;
            }

            if (packageName.equals("com.facebook.katana") || packageName.equals("com.zhiliaoapp.musically") || packageName.equals("com.google.android.youtube") || packageName.equals("org.telegram.messenger") || packageName.equals("com.whatsapp") || packageName.equals("com.instagram.android"))
                stats.add(new Stat(packageName, totalTime, startTimes));
        }

        String data = "";
        List<String> expectedPackages = Arrays.asList(
                "com.instagram.android",
                "org.telegram.messenger",
                "com.whatsapp",
                "com.google.android.youtube",
                "com.zhiliaoapp.musically",
                "com.facebook.katana"
        );

        // Verifica quali pacchetti sono già nella lista
        Set<String> existingPackages = new HashSet<>();
        for (Stat stat : stats) {
            existingPackages.add(stat.getPackageName());
        }

        // Aggiungi elementi con `totalTime` zero per i pacchetti mancanti
        for (String packageName : expectedPackages) {
            if (!existingPackages.contains(packageName)) {
                stats.add(new Stat(packageName, 0L)); // Aggiungi con totalTime zero
            }
        }

        // Ora la lista ha esattamente sei elementi, con eventuali spazi riempiti
        //String data = "";
        for (Stat s : stats) {
            data += s.getTotalTime() + ","; // Costruisci la stringa dei dati
        }

        if (data.length() > 0 && data.charAt(data.length() - 1) == ',') {
            data = data.substring(0, data.length() - 1);

        }
        System.out.println(data); // Stampa il risultato finale

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

            csvWriter.append(timestamp).append(",").append(data).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


