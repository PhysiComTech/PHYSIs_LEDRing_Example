package com.physicomtech.kit.physis_ledring_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.physicomtech.kit.physislibrary.PHYSIsBLEActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetupActivity extends PHYSIsBLEActivity {

    // region Check Bluetooth Permission
    private static final int REQ_APP_PERMISSION = 1000;
    private static final List<String> appPermissions
            = Collections.singletonList(Manifest.permission.ACCESS_COARSE_LOCATION);

    /*
        # 애플리케이션의 정상 동작을 위한 권한 체크
        - 안드로이드 마시멜로우 버전 이상에서는 일부 권한에 대한 사용자의 허용이 필요
        - 권한을 허용하지 않을 경우, 관련 기능의 정상 동작을 보장하지 않음.
        - 권한 정보 URL : https://developer.android.com/guide/topics/security/permissions?hl=ko
        - PHYSIs Maker Kit에서는 블루투스 사용을 위한 위치 권한이 필요.
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> reqPermissions = new ArrayList<>();
            for(String permission : appPermissions){
                if(checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED){
                    reqPermissions.add(permission);
                }
            }
            if(reqPermissions.size() != 0){
                requestPermissions(reqPermissions.toArray(new String[reqPermissions.size()]), REQ_APP_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQ_APP_PERMISSION){
            boolean accessStatus = true;
            for(int grantResult : grantResults){
                if(grantResult == PackageManager.PERMISSION_DENIED)
                    accessStatus = false;
            }
            if(!accessStatus){
                Toast.makeText(getApplicationContext(), "위치 권한 거부로 인해 애플리케이션을 종료합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    // endregion

    private final String SERIAL_NUMBER = "XXXXXXXXXXXX";       // PHYSIs Maker Kit 시리얼번호

    private static final String SETUP_STX = "$";               // LED 설정 메시지 프로토콜 STX/ETX
    private static final String SETUP_ETX = "#";

    Spinner spPattern, spColorType, spColor, spInterval;       // 액티비티 위젯
    Button btnConnect, btnDisconnect, btnTurnOn, btnTurnOff;
    ProgressBar pgbConnect;

    private boolean isConnected = false;             // BLE 연결 상태 변수

    // LED 출력 옵션 리스트
    private final String[] LED_PATTERNs = {"Dot Cycle", "Cycle", "Dot"};
    private final String[] LED_COLOR_TYPEs = {"Single", "Rotation", "Random"};
    private final String[] LED_COLORs = {"Red", "Green", "Blue", "Purple", "Sky Blue", "Yellow"};
    private final String[] LED_INTERVALs = {"50", "100", "250", "500", "1000", "2000"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        checkPermissions();                 // 앱 권한 체크 함수 호출
        initWidget();                       // 위젯 생성 및 초기화 함수 호출
        setEventListener();                 // 이벤트 리스너 설정 함수 호출
    }

    /*
        # 위젯 객체 생성 및 초기화
     */
    private void initWidget() {
        btnConnect = findViewById(R.id.btn_connect);                // 버튼 생성
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnTurnOn = findViewById(R.id.btn_turn_on);
        btnTurnOff = findViewById(R.id.btn_turn_off);
        pgbConnect = findViewById(R.id.pgb_connect);                // 프로그래스 생성

        spPattern = findViewById(R.id.sp_pattern);                  // 스피너 생성
        spColorType = findViewById(R.id.sp_color_type);
        spColor = findViewById(R.id.sp_color);
        spInterval = findViewById(R.id.sp_interval);

        // 정의된 LED 출력 속성 리스트에 대한 스피너 항목 설정
        spPattern.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                R.layout.item_spinner,LED_PATTERNs));
        spColorType.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                R.layout.item_spinner, LED_COLOR_TYPEs));
        spColor.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                R.layout.item_spinner, LED_COLORs));
        spInterval.setAdapter(new ArrayAdapter<>(getApplicationContext(),
                R.layout.item_spinner, LED_INTERVALs));
    }

    /*
        # 뷰 (버튼) 이벤트 리스너 설정
     */
    private void setEventListener() {
        btnConnect.setOnClickListener(new View.OnClickListener() {              // 연결 버튼
            @Override
            public void onClick(View v) {                   // 버튼 클릭 시 호출
                btnConnect.setEnabled(false);                       // 연결 버튼 비활성화 설정
                pgbConnect.setVisibility(View.VISIBLE);             // 연결 프로그래스 가시화 설정
                connectDevice(SERIAL_NUMBER);                       // PHYSIs Maker Kit BLE 연결 시도
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {           // 연결 종료 버튼
            @Override
            public void onClick(View v) {
                disconnectDevice();                                  // PHYSIs Maker Kit BLE 연결 종료
            }
        });

        btnTurnOn.setOnClickListener(new View.OnClickListener() {               // Turn On 버튼
            @Override
            public void onClick(View v) {
                if(isConnected) {                           // BLE 연결 시
                    sendControlMsg("1");                                // LED 제어 메시지 전송
                }
            }
        });

        btnTurnOff.setOnClickListener(new View.OnClickListener() {              // Turn Off 버튼
            @Override
            public void onClick(View v) {
                if(isConnected) {
                    sendControlMsg("0");                                // LED 제어 메시지 전송
                }
            }
        });
    }

    /*
      # BLE 연결 결과 수신
      - 블루투스 연결에 따른 결과를 전달받을 때 호출 (BLE 연결 상태가 변경됐을 경우)
      - 연결 결과 : CONNECTED(연결 성공), DISCONNECTED(연결 종료/실패), NO_DISCOVERY(디바이스 X)
    */
    @Override
    protected void onBLEConnectedStatus(int result) {
        super.onBLEConnectedStatus(result);
        setConnectedResult(result);                             // BLE 연결 결과 처리 함수 호출
    }

    /*
        # BLE 연결 결과 처리
     */
    private void setConnectedResult(int result){
        pgbConnect.setVisibility(View.INVISIBLE);               // 연결 프로그래스 비가시화 설정
        isConnected = result == CONNECTED;                      // 연결 결과 확인

        String toastMsg;                                        // 연결 결과에 따른 Toast 메시지 출력
        if(result == CONNECTED){
            toastMsg = "Physi Kit와 연결되었습니다.";
        }else if(result == DISCONNECTED){
            toastMsg = "Physi Kit 연결이 실패/종료되었습니다.";
        }else{
            toastMsg = "연결할 Physi Kit가 존재하지 않습니다.";
        }
        Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();

        btnConnect.setEnabled(!isConnected);                     // 연결 버튼 활성화 상태 설정
        btnDisconnect.setEnabled(isConnected);
    }

    /*
        # LED 제어 메시지 전송
        - LED 설정/제어 메시지 프로토콜 생성 및 전송 ( Data Format : $ 0 0 0 0 100 # )
        - 매개변수 ledState에 따라 LED 출력을 제어 ( 1 : LED On / 0 : LED Off )
     */
    private void sendControlMsg(String ledState) {
        // 제어 메시지 생성
        String ledSetData = SETUP_STX + ledState + String.valueOf(spPattern.getSelectedItemPosition())
                + String.valueOf(spColorType.getSelectedItemPosition())
                + String.valueOf(spColor.getSelectedItemPosition())
                + spInterval.getSelectedItem().toString() + SETUP_ETX;

        sendMessage(ledSetData);                                    // 제어 메시지 전송
    }
}
