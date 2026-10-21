package com.example.smsrelay;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public final class SmsReceiver extends BroadcastReceiver {
    static String normalize(String s) { return s == null ? "" : s.replaceAll("[^0-9+]", ""); }
    @Override public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        var prefs = context.getSharedPreferences("relay", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false)) return;
        if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return;
        String targetSender = prefs.getString("sender", "");
        String destination = prefs.getString("destination", "");
        if (targetSender.isEmpty() || destination.isEmpty()) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;
        String sender = normalize(parts[0].getDisplayOriginatingAddress());
        if (!sender.equals(targetSender) || sender.equals(destination)) return;
        StringBuilder message = new StringBuilder();
        for (SmsMessage part : parts) {
            if (!sender.equals(normalize(part.getDisplayOriginatingAddress()))) return;
            String body = part.getDisplayMessageBody();
            if (body != null) message.append(body);
        }
        if (message.length() == 0) return;
        try {
            // No external server is involved. SMS goes directly through the Galaxy's SIM.
            String text = "[문자 공유] 발신: " + sender + "\n" + message;
            SmsManager manager = SmsManager.getDefault();
            ArrayList<String> chunks = manager.divideMessage(text);
            if (chunks.size() == 1) manager.sendTextMessage(destination, null, chunks.get(0), null, null);
            else manager.sendMultipartTextMessage(destination, null, chunks, null, null);
            saveStatus(context, "전송 요청: " + sender);
        } catch (Exception e) {
            Log.e("SmsRelay", "SMS send failed", e);
            saveStatus(context, "전송 실패 (권한/SIM/통신사 확인)");
        }
    }
    private static void saveStatus(Context context, String status) {
        String time = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.KOREA).format(new Date());
        context.getSharedPreferences("relay", Context.MODE_PRIVATE).edit().putString("lastStatus", time + " " + status).apply();
    }
}
