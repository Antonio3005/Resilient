package com.example.wellness;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.time.LocalDate;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LocalDate today = LocalDate.now(); // Ottieni la data corrente
        AppActivity.saveData(context,today);

    }
}
