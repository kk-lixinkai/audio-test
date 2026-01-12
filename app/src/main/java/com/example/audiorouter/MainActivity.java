package com.example.audiorouter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView tvInfo;
    private Button btnRefresh;
    private AudioManager audioManager;
    private AudioDeviceCallback audioDeviceCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        btnRefresh = findViewById(R.id.btnRefresh);

        // 1. 获取 AudioManager 服务
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 2. 初始化手动刷新按钮
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAudioInfo();
            }
        });

        // 3. 注册音频设备插拔回调 (实现自动监听)
        registerAudioCallback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAudioInfo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
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
                    super.onAudioDevicesAdded(addedDevices);
                    showToast("设备已连接");
                    updateAudioInfo();
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    super.onAudioDevicesRemoved(removedDevices);
                    showToast("设备已移除");
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

        StringBuilder sb = new StringBuilder();

        // 获取当前的音频模式(如：正常、通话中、铃声中)
        sb.append("=== 当前音频模式 ===\n");
        sb.append("Mode: ").append(getModeName(audioManager.getMode())).append("\n\n");

        // 获取所有设备 (包括输入和输出)
        // GET_DEVICES_ALL: 返回所有连接的设备
        // 也可以使用 GET_DEVICES_OUTPUTS 或 GET_DEVICES_INPUTS 分别获取
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS | AudioManager.GET_DEVICES_OUTPUTS);

        sb.append("=== 检测到的设备 (共 ").append(devices.length).append(" 个) ===\n");

        for (AudioDeviceInfo device : devices) {
            sb.append("-----------------------------------\n");

            // 1. 设备名称(用户可见的名字，如 "Bluetooth Headphone")
            sb.append("Name: ").append(device.getProductName()).append("\n");

            // 2. 设备类型 (如 Speaker、Wired Headset、Bluetooth A2DP)
            sb.append("Type: ").append(getDeviceTypeName(device.getType())).append("\n");

            // 3. 角色 (输出 Sink 还是 输入 Source)
            String role = device.isSink() ? "Output (Sink)" : (device.isSource() ? "Input (Source)" : "Unknown");
            sb.append("Role: ").append(role).append("\n");

            // 4. 设备 ID(系统分配的唯一ID)
            sb.append("ID: ").append(device.getId()).append("\n");

            // 5. 采样率支持
            int[] sampleRates = device.getSampleRates();
            sb.append("Sample Rates: ").append(intArrayToString(sampleRates)).append("\n");

            // 6. 声道掩码(Channel Masks)
            int[] channelMasks = device.getChannelMasks();
            sb.append("Channel Masks: ").append(intArrayToString(channelMasks)).append("\n");

            // 7. 编码格式 (Encodings)
            int[] encoding = device.getEncodings();
            sb.append("Encodings: ").append(intArrayToString(encoding)).append("\n");

            // 8. 地址 (通常用于区分多个同类型设备，蓝牙设备显示MAC地址需权限，此处可能为空字符串)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                sb.append("Address: ").append(device.getAddress()).append("\n");
            }
        }

        tvInfo.setText(sb.toString());

    }

    // ------- 辅助工具方法 ----------
    private String intArrayToString(int[] arr) {
        if (arr == null || arr.length == 0) return "All/System Default";
        return Arrays.toString(arr);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String getModeName(int mode) {
        switch (mode) {
            case AudioManager.MODE_NORMAL:
                return "NORMAL";
            case AudioManager.MODE_RINGTONE:
                return "RINGTONE";
            case AudioManager.MODE_IN_CALL:
                return "IN_CALL";
            case AudioManager.MODE_IN_COMMUNICATION:
                return "IN_COMMUNICATION";
            default:
                return "UNKNOWN (" + mode + ")";
        }
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