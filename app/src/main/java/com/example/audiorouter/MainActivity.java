package com.example.audiorouter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvInfo;
    private Spinner spinnerDevices;
    private Button btnPlay, btnStop;

    private AudioManager audioManager;
    private AudioDeviceCallback audioDeviceCallback;
    private MediaPlayer mediaPlayer;

    /**
     * 用于保存当前所有可用的“输出”设备对象，以便播放时使用
     */
    private List<AudioDeviceInfo> mOutputDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        btnPlay.setOnClickListener(v -> playMusicOnSelectedDevice());
        btnStop.setOnClickListener(v -> stopMusic());

        // 注册插拔监听
        registerAudioCallback();
    }

    private void initViews() {
        tvInfo = findViewById(R.id.tvInfo);
        spinnerDevices = findViewById(R.id.spinnerDevices);
        btnPlay = findViewById(R.id.btnPlay);
        btnStop = findViewById(R.id.btnStop);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAudioInfo(); // 刷新列表和日志
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMusic();    // 释放MediaPlayer资源
        if (audioManager != null && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        }
    }

    /**
     * 核心逻辑：播放音乐并指定路由
     */
    private void playMusicOnSelectedDevice() {
        stopMusic();    // 如果正在播放，先停止

        // 1. 获取 Spinner 选中的索引
        int selectedIndex = spinnerDevices.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= mOutputDevices.size()) {
            showToast("请选择有效的输出设备");
            return;
        }

        // 2. 获取对应的 AudioDeviceInfo 对象
        AudioDeviceInfo targetDevice = mOutputDevices.get(selectedIndex);

        try {
            // 3. 初始化 MediaPlayer (使用系统默认铃声作为测试音频)
            Uri defaultRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, defaultRingtoneUri);

            // 4. 【关键步骤】设置音频路由到指定设备
            // Android M (API 23) 引入的方法
            boolean success = mediaPlayer.setPreferredDevice(targetDevice);

            if (!success) {
                showToast("系统拒绝了路由请求，可能将使用默认设备");
            }

            mediaPlayer.prepare();
            mediaPlayer.start();

            showToast("正在通过: " + targetDevice.getProductName() + " 播放");
            mediaPlayer.setOnCompletionListener(mp -> stopMusic());
        } catch (IOException e) {
            e.printStackTrace();
            showToast("播放失败: " + e.getMessage());
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 注册监听器，当耳机/蓝牙连接或断开时触发
     */
    private void registerAudioCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = new AudioDeviceCallback() {
                @Override
                public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                    updateAudioInfo();
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    stopMusic();
                    updateAudioInfo();
                }
            };
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, new Handler(Looper.getMainLooper()));
        }
    }

    /**
     * 核心方法：获取并显示所有音频设备信息
     */
    private void updateAudioInfo() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            tvInfo.setText("此功能需要 Android 6.0 (API 23) 及以上版本");
            return;
        }
        // 获取所有设备
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS | AudioManager.GET_DEVICES_OUTPUTS);

        //------------------------------------
        // 1. 更新下拉选择框 (只筛选 Output 设备)
        //------------------------------------
        mOutputDevices.clear();
        List<String> deviceNames = new ArrayList<>();

        for (AudioDeviceInfo device : devices) {
            // isSink() 返回 true 代表这是个输出设备 (扬声器、耳机)
            if (device.isSink()) {
                mOutputDevices.add(device);
                String name = device.getProductName() + " (" + getDeviceTypeName(device.getType()) + ")";
                deviceNames.add(name);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, deviceNames);
        spinnerDevices.setAdapter(adapter);

        //--------------------------------------------------
        // 2. 更新底部的详细日志文本 (原有逻辑)
        //--------------------------------------------------
        StringBuilder sb = new StringBuilder();
        sb.append("=== 当前检测到的设备 ===\n");
        for (AudioDeviceInfo device : devices) {
            sb.append("\nName: ").append(device.getProductName());
            sb.append("\nType: ").append(getDeviceTypeName(device.getType()));
            sb.append("\nRole: ").append(device.isSink() ? "Output" : "Input");
            sb.append("\nID:   ").append(device.getId());
            sb.append("\n--------------------");
        }
        tvInfo.setText(sb.toString());
    }

    // ------- 辅助工具方法 ----------

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 将设备类型的 int 常量转换为可读字符串
     * 基于 Android 11 API
     */
    private String getDeviceTypeName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "Built-in Earpiece";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "Built-in Speaker";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "Wired Headset (带麦克风)";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "Wired Headphone (无麦克风)";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "Bluetooth SCO (通话)";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "Bluetooth A2DP (媒体)";
            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";
            case AudioDeviceInfo.TYPE_DOCK:
                return "Dock";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB Device";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "USB Accessory";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "USB Headset";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "Built-in Microphone";
            case AudioDeviceInfo.TYPE_FM:
                return "FM Tuner";
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "Aux Line";
            case AudioDeviceInfo.TYPE_IP:
                return "IP";
            case AudioDeviceInfo.TYPE_BUS:
                return "BUS";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "Hearing Aid";
            // Android 11+ 新增类型(如果有)
            case 26 /* TYPE_BLE_HEADSET */:
                return "BLE Headset";
            case 27 /* TYPE_BLE_SPEAKER */:
                return "BLE Speaker";
            case AudioDeviceInfo.TYPE_UNKNOWN:
            default:
                return "Unknown Type (" + type + ")";
        }
    }
}