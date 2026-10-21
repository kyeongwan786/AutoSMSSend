package com.example.smsrelay;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText sender, destination;
    private Switch enabled;
    private TextView info;
    private static final int PERMISSIONS_REQUEST = 10;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText("문자 선택 공유");
        title.setTextSize(24);
        root.addView(title);
        TextView description = new TextView(this);
        description.setText("선택한 발신번호에서 온 문자만 지정한 휴대폰 번호로 SMS 재전송합니다. 전송에는 통신사 문자 요금이 발생할 수 있습니다.");
        root.addView(description);
        sender = new EditText(this);
        sender.setSingleLine(true);
        sender.setHint("감지할 발신번호 (예: 15881234)");
        sender.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(sender);
        destination = new EditText(this);
        destination.setSingleLine(true);
        destination.setHint("받을 아이폰 번호 (예: 01012345678)");
        destination.setInputType(InputType.TYPE_CLASS_PHONE);
        root.addView(destination);
        enabled = new Switch(this);
        enabled.setText("자동 문자 공유 ON / OFF");
        root.addView(enabled);
        Button save = new Button(this);
        save.setText("설정 저장");
        root.addView(save);
        Button permission = new Button(this);
        permission.setText("문자 수신·전송 권한 허용");
        root.addView(permission);
        info = new TextView(this);
        info.setPadding(0, pad, 0, 0);
        root.addView(info);
        setContentView(root);
        var prefs = getSharedPreferences("relay", MODE_PRIVATE);
        sender.setText(prefs.getString("sender", ""));
        destination.setText(prefs.getString("destination", ""));
        enabled.setChecked(prefs.getBoolean("enabled", false));
        save.setOnClickListener(v -> {
            String from = SmsReceiver.normalize(sender.getText().toString());
            String to = SmsReceiver.normalize(destination.getText().toString());
            if (from.length() < 4 || to.length() < 9 || to.length() > 15 || from.equals(to)) {
                Toast.makeText(this, "발신번호와 수신번호를 정확히 입력하세요.", Toast.LENGTH_LONG).show();
                return;
            }
            boolean on = enabled.isChecked();
            if (on && !permissionsGranted()) {
                enabled.setChecked(false);
                Toast.makeText(this, "먼저 문자 권한을 허용하세요.", Toast.LENGTH_LONG).show();
                requestSmsPermissions();
                return;
            }
            prefs.edit().putString("sender", from).putString("destination", to).putBoolean("enabled", on).apply();
            updateStatus();
            Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        });
        permission.setOnClickListener(v -> requestSmsPermissions());
        updateStatus();
    }
    private boolean permissionsGranted() {
        return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestSmsPermissions() {
        requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
    }
    @Override protected void onResume() { super.onResume(); if (info != null) updateStatus(); }
    private void updateStatus() {
        var p = getSharedPreferences("relay", MODE_PRIVATE);
        info.setText("저장된 상태: " + (p.getBoolean("enabled", false) ? "ON" : "OFF")
            + "\n문자 권한: " + (permissionsGranted() ? "허용" : "필요")
            + "\n마지막 처리: " + p.getString("lastStatus", "기록 없음")
            + "\n설정을 변경하면 반드시 '설정 저장'을 누르세요.");
    }
}
