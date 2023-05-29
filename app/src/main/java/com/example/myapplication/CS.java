package com.example.myapplication;

import static com.example.myapplication.ServerTask.TransTime;

import org.jtransforms.fft.*;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;


public class CS {
    private static final String TAG = "CollectSound";

    private AudioRecord mAudioRecord;

    private int mAudioSource = MediaRecorder.AudioSource.MIC;
    private int mSampleRate = 44100;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 4096; // 缓冲区大小

    private static final int WINDOW_SIZE = 1024; // 窗口大小
    private static final int STEP_SIZE = 1; // 重叠大小
//    private double[] overlapBuffer = new double[WINDOW_SIZE - STEP_SIZE]; // 重叠缓冲区

    private static final int TARGET_FREQUENCY = 10000; // 目标频率
    private static final int MAX_FREQUENCY_DEVIATION = 10; // 最大频率偏差

    private boolean mIsRecording = false;
    public boolean mTargetFrequencyDetected = false; // 新增变量，用于判断是否检测到目标频率
    public static long RecordTime;




    @SuppressLint("MissingPermission")//忽略该警告
    public CS() {
        int minBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);
        Log.i(TAG, "大小: " + minBufferSize);
        mAudioRecord = new AudioRecord(mAudioSource, mSampleRate, mChannelConfig, mAudioFormat, Math.max(BUFFER_SIZE, minBufferSize));
    }

    public void startRecording() throws IOException {
        // 创建缓冲区
        short[] audioBuffer = new short[BUFFER_SIZE];

        // 启动录制
        mAudioRecord.startRecording();
        mIsRecording = true;

        while (mIsRecording && !mTargetFrequencyDetected) {
            // 读取音频数据
            int bytesRead = mAudioRecord.read(audioBuffer, 0, BUFFER_SIZE);

            // 处理音频数据
            processAudioData(audioBuffer, bytesRead);
        }

        // 停止录制
        mAudioRecord.stop();
        mAudioRecord.release();
    }
    public void stopRecording() {
        mIsRecording = false;
    }

    private void processAudioData(short[] audioData, int bytesRead) {
        // 窗口处理循环
        for (int i = 0; i < bytesRead; i += 1) {
            if (i + WINDOW_SIZE <= bytesRead) {
                // 提取窗口数据
                double[] windowData = extractWindowData(audioData, i);



                // 应用汉宁窗
                applyHanningWindow(windowData);

                // 进行FFT变换
                double[] fftData = applyFFT(windowData);

                // 计算幅度最大的频率
                int maxFrequencyIndex = findMaxFrequencyIndex(fftData);
                double maxFrequency = calculateFrequency(maxFrequencyIndex);

                // 判断是否接收到信号
                if (Math.abs(maxFrequency - TARGET_FREQUENCY) <= MAX_FREQUENCY_DEVIATION) {
                    // 记录时间
                     RecordTime = System.currentTimeMillis();
                    Log.d(TAG, "收到信号的时间: "+ RecordTime);
                    // 处理信号 2177.2108.2124.2152.2127.2128.2117.2156.2084.2102.2081.2123.2167.2148.2119.2156.2157.2089.2134.2150///5516,5277,5225,5329,
                    mTargetFrequencyDetected = true;
                    break;
                }
            }
        }

        if (mTargetFrequencyDetected) {
            // 处理信号
            mIsRecording = false;

        }

    }

    private double[] extractWindowData(short[] audioData, int startIndex) {
        double[] windowData = new double[WINDOW_SIZE];
        for (int i = 0; i < WINDOW_SIZE; i++) {
            windowData[i] = audioData[startIndex + i];
        }
        return windowData;
    }

    private void applyHanningWindow(double[] windowData) {
        for (int i = 0; i < WINDOW_SIZE; i++) {
            double multiplier = 0.5 * (1 - Math.cos(2 * Math.PI * i / (WINDOW_SIZE - 1)));
            windowData[i] *= multiplier;
        }
    }

    private double[] applyFFT(double[] windowData) {
        DoubleFFT_1D fft = new DoubleFFT_1D(WINDOW_SIZE);
        double[] fftData = new double[2 * WINDOW_SIZE];
        System.arraycopy(windowData, 0, fftData, 0, WINDOW_SIZE);
        fft.realForwardFull(fftData);
        return fftData;
    }

    private int findMaxFrequencyIndex(double[] fftData) {
        int maxIndex = 0;
        double maxAmplitude = 0;
        for (int i = 0; i < WINDOW_SIZE; i++) {
            double amplitude = Math.sqrt(Math.pow(fftData[2 * i], 2) + Math.pow(fftData[2 * i + 1], 2));
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private double calculateFrequency(int index) {
        return index * ((double) mSampleRate / WINDOW_SIZE);
    }



}




