package com.crush.aiblackboxreview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capacitorjs.plugins.browser.BrowserPlugin;
import com.capacitorjs.plugins.app.AppPlugin;

import com.crush.aiblackboxreview.services.VideoMonitoringService;
import com.crush.aiblackboxreview.plugins.AutoDetectPlugin;
import com.crush.aiblackboxreview.plugins.FcmTokenPlugin;
import com.crush.aiblackboxreview.notifications.ReportNotificationManager;

import com.getcapacitor.BridgeActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;

//개발때만 실서비스에선 지우기
import android.webkit.WebSettings;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int ALL_FILES_ACCESS_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // deep link 콜백
        this.registerPlugin(BrowserPlugin.class);
        this.registerPlugin(AppPlugin.class);

        // 자동 감지 설정 플러그인 등록
        this.registerPlugin(AutoDetectPlugin.class);

        // FCM 토큰 플러그인 등록 (이 줄을 추가)
        this.registerPlugin(FcmTokenPlugin.class);

        setupFcmTokenTest();

        // Capacitor UI 초기화 이후 권한 확인을 안전하게
        getWindow().getDecorView().post(this::checkAndRequestPermissions);

        // 2) WebView 디버깅 활성화
        WebView.setWebContentsDebuggingEnabled(true);
        WebView webView = (WebView) this.getBridge().getWebView();

        //이부분 지워야함 개발에서만 사용
        // 개발 중에만 사용 → 출시 빌드에선 꼭 제거하세요!
        webView.getSettings().setMixedContentMode(
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        );

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d(TAG, "[WebView] " + cm.message()
                        + " (" + cm.sourceId() + ":" + cm.lineNumber() + ")");
                return super.onConsoleMessage(cm);
            }
        });

        // 3) 앱이 처음 실행될 때 받은 Intent 데이터 로그
        Uri initData = getIntent().getData();
        Log.e(TAG, "onCreate Intent data: " + initData);

        // 알림에서 열린 경우 처리
        handleNotificationIntent(getIntent());

        // 보고서 알림 테스트 버튼 추가 (개발 중에만 사용)
        setupReportNotificationTest();

    }

    private void checkAndRequestPermissions() {

        List<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
                Log.d(TAG, "READ_MEDIA_VIDEO 권한 필요");
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
                Log.d(TAG, "POST_NOTIFICATIONS 권한 필요");
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                Log.d(TAG, "READ_EXTERNAL_STORAGE 권한 필요");
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                Log.d(TAG, "WRITE_EXTERNAL_STORAGE 권한 필요");
            }
        }

        // 포그라운드 서비스 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE);
            Log.d(TAG, "FOREGROUND_SERVICE 권한 필요");
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "모든 권한이 이미 부여됨. 서비스 시작");

            startVideoMonitoringService();
        } else {
            Log.d(TAG, "권한 요청: " + permissionsToRequest);
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 각 권한의 부여 결과 로깅
            for (int i = 0; i < permissions.length; i++) {
                String permissionName = permissions[i];
                boolean isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "권한 결과: " + permissionName + " = " + (isGranted ? "허용" : "거부"));
            }

            // 모든 권한이 부여되었는지 확인
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "모든 권한이 허용됨. 서비스 시작");
                startVideoMonitoringService();
            } else {
                Log.d(TAG, "일부 권한이 거부됨. 서비스를 시작할 수 없음");
                // 사용자에게 알림
                Toast.makeText(this, "블랙박스 영상 감지를 위해 모든 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ALL_FILES_ACCESS_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.d(TAG, "모든 파일 액세스 권한 획득됨");
                    // 나머지 권한 확인 계속 진행
                    checkAndRequestPermissions();
                } else {
                    Log.d(TAG, "모든 파일 액세스 권한 거부됨");
                    Toast.makeText(this, "파일 접근 권한이 필요합니다", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void startVideoMonitoringService() {
        try {
            Log.d(TAG, "비디오 모니터링 서비스 시작 시도");

            // 테스트 알림 표시
            testNotification();

            Intent serviceIntent = new Intent(this, VideoMonitoringService.class);

            // UI가 완전히 올라온 뒤 실행되도록 postDelayed
            getWindow().getDecorView().postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Log.d(TAG, "비디오 모니터링 서비스 시작 성공");
            }, 500); // 0.5초 정도 지연

        } catch (Exception e) {
            Log.e(TAG, "서비스 시작 실패: " + e.getMessage(), e);
        }
    }
    private void testNotification() {
        try {
            // 알림 채널 생성
            String channelId = "test_channel";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "테스트 알림 채널",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("테스트용 알림 채널입니다");
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }

            // 알림 생성
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("테스트 알림")
                    .setContentText("이 알림이 보이면 알림 기능이 정상 작동합니다.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 500, 250, 500});

            // 알림 표시
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(9999, builder.build());

            Log.d(TAG, "테스트 알림이 표시되었습니다.");
        } catch (Exception e) {
            Log.e(TAG, "테스트 알림 표시 중 오류 발생", e);
        }
    }

    /**
     * 알림 인텐트 처리
     * 알림을 통해 앱이 실행된 경우 처리하는 메서드
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
            String reportId = intent.getStringExtra("REPORT_ID");
            if (reportId != null) {
                Log.d(TAG, "알림에서 열림: reportId=" + reportId);
                // 여기서 보고서 상세 화면으로 이동하거나 데이터 표시
                Toast.makeText(this, "보고서 ID: " + reportId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 보고서 알림 테스트 버튼 설정
     * 개발 중에 알림 기능을 테스트하기 위한 UI 요소 추가
     */
    private void setupReportNotificationTest() {
        // 테스트 버튼 생성
        Button testButton = new Button(this);
        testButton.setText("보고서 알림 테스트");

        // 버튼 레이아웃 설정
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(0, 50, 0, 0);
        testButton.setLayoutParams(layoutParams);

        // 클릭 이벤트 설정
        testButton.setOnClickListener(v -> {
            // 테스트 알림 표시
            ReportNotificationManager notificationManager = new ReportNotificationManager(this);
            notificationManager.showReportNotification(
                    "사고 분석 완료",
                    "블랙박스 영상에서 감지된 사고의 분석이 완료되었습니다. 자세한 내용을 확인하려면 알림을 탭하세요.",
                    "test_report_" + System.currentTimeMillis()
            );
            Toast.makeText(this, "테스트 알림이 발송되었습니다.", Toast.LENGTH_SHORT).show();
        });

        // 기존 UI에 버튼 추가
        try {
            ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
            if (rootView != null) {
                rootView.addView(testButton);
                Log.d(TAG, "보고서 알림 테스트 버튼이 추가되었습니다.");
            }
        } catch (Exception e) {
            Log.e(TAG, "테스트 버튼 추가 실패: " + e.getMessage(), e);
        }
    }

    /**
     * FCM 토큰 등록을 테스트하기 위한 버튼을 추가하는 메서드
     */
    private void setupFcmTokenTest() {
        // 테스트 버튼 생성
        Button testButton = new Button(this);
        testButton.setText("FCM 토큰 등록 테스트");

        // 버튼 레이아웃 설정
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.setMargins(0, 150, 0, 0);
        testButton.setLayoutParams(layoutParams);

        // 클릭 이벤트 설정
        testButton.setOnClickListener(v -> {
            Toast.makeText(this, "FCM 토큰 테스트 시작...", Toast.LENGTH_SHORT).show();
            testFcmTokenWithManager();
        });

        // 기존 UI에 버튼 추가
        try {
            ViewGroup rootView = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
            if (rootView != null) {
                rootView.addView(testButton);
                Log.d(TAG, "FCM 토큰 테스트 버튼이 추가되었습니다.");
            }
        } catch (Exception e) {
            Log.e(TAG, "테스트 버튼 추가 실패: " + e.getMessage(), e);
        }
    }

    /**
     * FcmTokenManager를 사용하여 FCM 토큰 등록을 테스트합니다.
     */
    private void testFcmTokenWithManager() {
        Log.d(TAG, "FCM 토큰 테스트 시작 (FcmTokenManager 사용)");

        // 1. 임시 JWT 토큰 저장 (FcmTokenManager가 사용할 수 있도록)
        String jwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrYWthbzo0MjUxNzkzNzA2IiwidXNlcklkIjoxMSwiaWF0IjoxNzQ3Mjg5NzkwLCJleHAiOjE3NDczNzYxOTB9.FJ8gEd7DF1mqB4KpzgavwJzLt7vdha4ni1yugowe8JU";
        SharedPreferences authPref = getSharedPreferences("auth_prefs", MODE_PRIVATE);
        authPref.edit().putString("auth_token", jwtToken).apply();

        // 2. FCM 토큰 요청
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "FCM 토큰 요청 실패", task.getException());
                        Toast.makeText(this, "FCM 토큰 요청 실패", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // FCM 토큰 획득 성공
                    String fcmToken = task.getResult();
                    Log.d(TAG, "FCM 토큰 획득 성공: " + fcmToken.substring(0, 20) + "...");
                    Toast.makeText(this, "FCM 토큰 획득 성공", Toast.LENGTH_SHORT).show();
                    Log.d("AUTH_TOKEN", fcmToken);
                    // 3. FcmTokenManager를 사용하여 토큰 등록
                    try {
                        com.crush.aiblackboxreview.managers.FcmTokenManager manager =
                                new com.crush.aiblackboxreview.managers.FcmTokenManager(this);
                        manager.registerTokenToServer(fcmToken);
                        Log.d(TAG, "FcmTokenManager를 통한 등록 요청 완료");
                        Toast.makeText(this, "FCM 토큰 등록 요청 완료", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "FCM 토큰 등록 요청 실패", e);
                        Toast.makeText(this, "FCM 토큰 등록 요청 실패: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent.getData();
        if (data != null) {
            Log.e(TAG, "onNewIntent, 딥링크 URL: " + data.toString());
        } else {
            Log.e(TAG, "onNewIntent, data 가 null 입니다");
        }

        // 알림에서 열린 경우도 처리
        handleNotificationIntent(intent);
    }

}