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
import android.webkit.JavascriptInterface;
import android.webkit.JavascriptInterface;

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
import com.crush.aiblackboxreview.managers.FcmTokenManager;
import com.crush.aiblackboxreview.managers.FcmTokenManager;

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

        // FCM 토큰 플러그인 등록
        // FCM 토큰 플러그인 등록
        this.registerPlugin(FcmTokenPlugin.class);

        // FCM 브릿지 설정 - 자바스크립트에서 호출 가능하도록
        setupJsInterface();
        // FCM 브릿지 설정 - 자바스크립트에서 호출 가능하도록
        setupJsInterface();

        // 웹뷰 레이아웃 설정
        setupWebViewLayout();

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
    }
    
    /**
     * 자바스크립트에서 호출할 수 있는 인터페이스 설정
     */
    private void setupJsInterface() {
        try {
            WebView webView = (WebView) this.getBridge().getWebView();
            webView.getSettings().setJavaScriptEnabled(true);
            
            // FCM 브릿지 객체 추가
            webView.addJavascriptInterface(new FcmBridge(), "androidFcmBridge");
            
            Log.d(TAG, "자바스크립트 인터페이스 설정 완료 - FCM 브릿지 등록됨");
        } catch (Exception e) {
            Log.e(TAG, "자바스크립트 인터페이스 설정 실패", e);
        }
    }
    
    /**
     * FCM 토큰 관련 자바스크립트 브릿지 클래스
     */
    private class FcmBridge {
        @JavascriptInterface
        public String registerFcmToken() {
            Log.d(TAG, "🌉 자바스크립트에서 registerFcmToken() 호출됨");
            
            try {
                // FcmTokenManager를 사용하여 토큰 등록 처리
                FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "JS 브릿지: FCM 토큰 요청 실패", task.getException());
                            return;
                        }

                        // FCM 토큰 획득 성공
                        String fcmToken = task.getResult();
                        Log.d(TAG, "JS 브릿지: FCM 토큰 획득 성공: " + 
                            (fcmToken.length() > 20 ? fcmToken.substring(0, 20) + "..." : fcmToken));

                        // FcmTokenManager를 사용하여 토큰 등록
                        try {
                            FcmTokenManager manager = new FcmTokenManager(MainActivity.this);
                            manager.registerTokenToServer(fcmToken);
                            Log.d(TAG, "JS 브릿지: FCM 토큰 등록 요청 완료");
                            
                            // 결과를 Capacitor 저장소에 저장 (JS에서 확인 가능)
                            SharedPreferences capacitorPrefs = 
                                getSharedPreferences("CapacitorStorage", MODE_PRIVATE);
                            capacitorPrefs.edit()
                                .putString("fcm_token_registered", "true")
                                .putString("fcm_token", fcmToken)
                                .apply();
                            
                        } catch (Exception e) {
                            Log.e(TAG, "JS 브릿지: FCM 토큰 등록 실패", e);
                        }
                    });
                
                // 성공 응답 반환 - 자바스크립트에서 성공으로 처리됨
                return "success";
            } catch (Exception e) {
                Log.e(TAG, "JS 브릿지: 예외 발생", e);
                return "error: " + e.getMessage();
            }
        }
        
        @JavascriptInterface
        public String getFcmToken() {
            Log.d(TAG, "🌉 자바스크립트에서 getFcmToken() 호출됨");
            
            try {
                // 저장된 토큰 조회
                FcmTokenManager manager = new FcmTokenManager(MainActivity.this);
                String token = manager.getFcmToken();
                
                if (token != null && !token.isEmpty()) {
                    Log.d(TAG, "JS 브릿지: 저장된 FCM 토큰 반환: " + 
                        (token.length() > 10 ? token.substring(0, 10) + "..." : token));
                    return token;
                } else {
                    // 저장된 토큰이 없으면 Firebase에서 직접 가져오기 시도
                    try {
                        FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String fcmToken = task.getResult();
                                    Log.d(TAG, "JS 브릿지: 새 FCM 토큰 획득: " + 
                                        (fcmToken.length() > 10 ? fcmToken.substring(0, 10) + "..." : fcmToken));
                                    
                                    // 저장
                                    manager.saveFcmToken(fcmToken);
                                }
                            });
                    } catch (Exception e) {
                        Log.e(TAG, "JS 브릿지: 토큰 획득 실패", e);
                    }
                    
                    Log.d(TAG, "JS 브릿지: 저장된 FCM 토큰 없음, 빈 문자열 반환");
                    return "";
                }
            } catch (Exception e) {
                Log.e(TAG, "JS 브릿지: 토큰 획득 중 오류", e);
                return "";
            }
        }
        
        @JavascriptInterface
        public String getNewReportIds() {
            Log.d(TAG, "🌉 자바스크립트에서 getNewReportIds() 호출됨");
            
            try {
                // 안드로이드 서비스에서 저장한 새 보고서 ID 목록 가져오기
                SharedPreferences sharedPref = getSharedPreferences("capacitor_new_reports", MODE_PRIVATE);
                String newReportIdsJson = sharedPref.getString("NEW_REPORT_IDS", "[]");
                
                Log.d(TAG, "새 보고서 ID 목록: " + newReportIdsJson);
                
                // 가져온 목록을 Capacitor Preferences에 동기화
                SharedPreferences capacitorPrefs = getSharedPreferences("CapacitorStorage", MODE_PRIVATE);
                capacitorPrefs.edit()
                    .putString("NEW_REPORT_IDS", newReportIdsJson)
                    .apply();
                
                return newReportIdsJson;
            } catch (Exception e) {
                Log.e(TAG, "새 보고서 ID 목록 가져오기 실패", e);
                return "[]";
            }
        }
        
        @JavascriptInterface
        public boolean updateNewReportIds(String jsonArray) {
            Log.d(TAG, "🌉 자바스크립트에서 updateNewReportIds() 호출됨: " + jsonArray);
            
            try {
                // 자바스크립트에서 보낸 업데이트된 ID 목록을 안드로이드에 저장
                SharedPreferences sharedPref = getSharedPreferences("capacitor_new_reports", MODE_PRIVATE);
                sharedPref.edit()
                    .putString("NEW_REPORT_IDS", jsonArray)
                    .apply();
                
                // Capacitor Preferences에도 저장
                SharedPreferences capacitorPrefs = getSharedPreferences("CapacitorStorage", MODE_PRIVATE);
                capacitorPrefs.edit()
                    .putString("NEW_REPORT_IDS", jsonArray)
                    .apply();
                
                Log.d(TAG, "새 보고서 ID 목록 업데이트 완료");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "새 보고서 ID 목록 업데이트 실패", e);
                return false;
            }
        }
    }

    /**
     * 웹뷰 레이아웃 관련 설정
     */
    private void setupWebViewLayout() {
        try {
            WebView webView = (WebView) this.getBridge().getWebView();
            ViewGroup.LayoutParams params = webView.getLayoutParams();

            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;

                // 기존 코드에서 navigation_bar_height를 쓰고 있다면 아래처럼 보일 것임
                int navBarHeight = getResources().getDimensionPixelSize(
                        getResources().getIdentifier("navigation_bar_height", "dimen", "android")
                );

                marginParams.bottomMargin = navBarHeight + 30; // ✅ 여기가 문제!

                webView.setLayoutParams(marginParams);
            }

        } catch (Exception e) {
            Log.e("WebViewSetup", "웹뷰 레이아웃 설정 실패", e);
        }
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

    /**
     * 알림 인텐트 처리
     * 알림을 통해 앱이 실행된 경우 처리하는 메서드
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent.getBooleanExtra("FROM_NOTIFICATION", false)) {
            final String reportId = intent.getStringExtra("REPORT_ID");
            if (reportId != null) {
                Log.d(TAG, "알림에서 열림: reportId=" + reportId);
                
                // 웹뷰 참조
                final WebView webView = (WebView) this.getBridge().getWebView();
                
                // FCM 플래그 설정 (딥링크 방지)
                webView.evaluateJavascript(
                    "window.__FROM_FCM_NOTIFICATION = true;" +
                    "window.__FCM_REPORT_ID = '" + reportId + "';",
                    null
                );
                
                // 먼저 대시보드로 이동한 후, 약간의 지연 후 분석 페이지로 이동
                String script = "window.location.href = '/dashboard';";
                webView.evaluateJavascript(script, null);
                Log.d(TAG, "대시보드로 먼저 이동합니다");
                
                // 0.5초 후에 분석 페이지로 이동
                webView.postDelayed(() -> {
                    try {
                        // 시간을 두고 분석 페이지로 이동
                        String delayedScript = String.format(
                            "try {" +
                            "  console.log('FCM 알림: 분석 페이지로 이동 시도, ID=%s');" +
                            "  if (window.next && window.next.router) {" +
                            "    window.next.router.push('/analysis?id=%s');" +
                            "  } else {" +
                            "    window.location.href = '/analysis?id=%s';" +
                            "  }" +
                            "} catch(e) {" +
                            "  console.error('이동 중 오류:', e);" +
                            "  window.location.href = '/analysis?id=%s';" +
                            "}",
                            reportId, reportId, reportId, reportId
                        );
                        
                        webView.evaluateJavascript(delayedScript, result -> {
                            Log.d(TAG, "분석 페이지 이동 결과: " + result);
                        });
                        
                        Log.d(TAG, "분석 페이지로 지연 이동: reportId=" + reportId);
                    } catch (Exception e) {
                        Log.e(TAG, "지연 이동 중 오류", e);
                    }
                }, 1000); // 1000ms 후 실행
            }
        }
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