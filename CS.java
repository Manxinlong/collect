package com.example.myapplication;

import static com.example.myapplication.ServerTask.TransTime;

import org.jtransforms.fft.*;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;

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
    int windowSize = 1024; // 滑动窗口大小

    // 目标频率和允许的浮动范围
    private double targetFrequency = 10000; // 10kHz
    private double frequencyRange = 1000; // 上下浮动10Hz

    private boolean mIsRecording = false;
    public boolean mTargetFrequencyDetected = false; // 新增变量，用于判断是否检测到目标频率
    public static long RecordTime;
    private long deltaTime;

    private long mStartTime = 0; // 当前数组读取时的时间
    private long mPreviousStartTime = 0; // 上一次记录的时间
    private boolean mIsFirstTime = true; // 标记是否第一次读取音频数据
    private short[] mPreviousBuffer = null;


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
// && !mTargetFrequencyDetected
        while (mIsRecording) {
            // 保存上一次记录的时间
            mPreviousStartTime = mStartTime;

            // 读取音频数据前保存当前时间
            long currentTime = System.currentTimeMillis();

            // 第一次读取音频数据时，初始化 mStartTime
            if (mIsFirstTime) {
                mStartTime = currentTime;
                mIsFirstTime = false;
            }

            // 计算数组处理的时间间隔
             deltaTime = currentTime - mStartTime;
//            Log.d(TAG, "数组之间更新的时间: " + deltaTime);

            // 更新 mStartTime
            mStartTime = currentTime;
            // 读取音频数据
            int bytesRead = mAudioRecord.read(audioBuffer, 0, BUFFER_SIZE);
//            Log.d(TAG, "实际读取数据大小: "+ bytesRead);

            // 处理音频数据
            processAudioData(audioBuffer, bytesRead);


        }

        // 停止录制
        mAudioRecord.stop();
        mAudioRecord.release();
    }

    private void processAudioData(short[] audioBuffer, int bytesRead) {
        // 对该数组内的数据进行目标检测
        boolean targetDetected = detectTargetFrequency(audioBuffer, bytesRead);
//        Log.d(TAG, "是否检测到了信号: "+ targetDetected);

        if (targetDetected) {
            Log.d(TAG, "检测到信号后进入了if语句");

            // 若检测到目标频率，拼接之前保存的数组数据
            short[] combinedBuffer = combineBuffers(mPreviousBuffer, audioBuffer, bytesRead);//4096*2

//            for (int i = 0; i < 10; i++) {
//                Log.d(TAG, "合并后的数组前十个数据: " + combinedBuffer[i]);
//            }

            //转换为double类型
            double[] combinedBuffer_double = convertToDoubleArray(combinedBuffer,combinedBuffer.length);

            // 应用汉宁窗进行预处理
            applyHanningWindow(combinedBuffer_double);

            // 利用滑动窗口移动，找出目标频率的位置
            int targetIndex = findTargetIndex(combinedBuffer_double);

            if (targetIndex != -1) {
                // 根据时间上的等比关系求出第一次检测出目标的时间
                long targetTime =  mPreviousStartTime + (targetIndex  / mSampleRate) * 1000;//因为拼接后找时间的话默认数组之间是连续的（一次性采集两个buffersize的数据），实际上数组之间存在更新时间
                // 处理目标时间
                processTargetTime(targetTime,targetIndex);
                //第一次检测到当前目标后，退出当前函数
                mIsRecording = false;
                return;
            }

            // 只保存当前处理数组的前一个数组
//            mPreviousBuffer = Arrays.copyOfRange(audioBuffer, 0, bytesRead);//如果一直检测到mmPreviousBuffer保存的是检测到频率是的buffer数据，所以索引是0
        } else {
            // 未检测到目标频率，保存当前处理数组
            mPreviousBuffer = Arrays.copyOfRange(audioBuffer, 0, bytesRead);

//            for (int i = 0; i < 10; i++) {
//                Log.d(TAG, "mPreviousBuffer的前十个数据: " + mPreviousBuffer[i]);
//            }
        }
    }

    private boolean detectTargetFrequency(short[] audioBuffer, int bytesRead) {

        double[] audioData = convertToDoubleArray(audioBuffer, bytesRead);

        // 应用汉宁窗进行预处理
        applyHanningWindow(audioData);//大小4096

        // 执行FFT变换

        double[] fftData = applyFFT(audioData);

        // 计算幅值最大的频率
        int maxAmplitudeIndex = findMaxFrequencyIndex(fftData);
        double maxAmplitudeFrequency = maxAmplitudeIndex * (mSampleRate / BUFFER_SIZE);
        Log.d(TAG, "1-幅值最大的频率: " + maxAmplitudeFrequency);



        // 判断幅值最大的频率是否在浮动范围内
        if (Math.abs(maxAmplitudeFrequency - targetFrequency) <= frequencyRange) {
            // 第一次检测到目标频率，返回true
            return true;
        }

        // 未检测到目标频率，返回false
        return false;
    }


    private short[] combineBuffers(short[] previousBuffer, short[] currentBuffer, int bytesRead) {
        int combinedLength = previousBuffer.length + bytesRead;
        short[] combinedBuffer = new short[combinedLength];
        System.arraycopy(previousBuffer, 0, combinedBuffer, 0, previousBuffer.length);
        System.arraycopy(currentBuffer, 0, combinedBuffer, previousBuffer.length, bytesRead);
        return combinedBuffer;
    }

    private int findTargetIndex(double[] buffer) {

        int targetIndex = -1; // 初始化目标频率的索引
        // 将拼接的两个数组进行滑动窗口遍历
        for (int i = 0; i <= buffer.length - windowSize; i++) {
            // 获取当前窗口的数据
            double[] windowData = Arrays.copyOfRange(buffer, i, i + windowSize);
            // 在当前窗口内查找目标频率的索引
            int frequencyIndex = findTargetFrequencyIndex(windowData, windowSize);

            // 如果找到目标频率，则更新目标索引，并退出循环
            if (frequencyIndex != -1) {
                targetIndex = i + frequencyIndex;
                Log.d(TAG, "窗口开始的位置: " + i);
                Log.d(TAG, "目标频率在窗口内的位置: "+ frequencyIndex );
                Log.d(TAG, "目标频率位置: "+ targetIndex);
                break;
            }

        }

        // 未找到目标频率，返回-1
        return targetIndex;
    }


    private void processTargetTime(long targetTime, int index) {
        // 处理目标时间
        if (index < BUFFER_SIZE){
            RecordTime = targetTime;
        }else {
            RecordTime = targetTime + deltaTime;
        }
        Log.d(TAG, "TargetTime: "+ targetTime);
        Log.d(TAG, "deltaTime: "+ deltaTime);
        Log.d(TAG, "RecordTime: "+ RecordTime);
    }

    private void applyHanningWindow(double[] windowData) {
        for (int i = 0; i < windowData.length; i++) {
            double multiplier = 0.5 * (1 - Math.cos(2 * Math.PI * i / (windowData.length - 1)));
            windowData[i] *= multiplier;
        }
    }

    private int findMaxFrequencyIndex(double[] fftData) {
        int maxIndex = 0;
        double maxAmplitude = 0;
        for (int i = 0; i < fftData.length/2; i++) {
            double amplitude = Math.sqrt(Math.pow(fftData[2 * i], 2) + Math.pow(fftData[2 * i + 1], 2));
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private double[] convertToDoubleArray(short[] audioBuffer, int length) {
        double[] doubleArray = new double[length];
        for (int i = 0; i < length; i++) {
            doubleArray[i] = (double) audioBuffer[i];
        }
        return doubleArray;
    }

    private double[] applyFFT(double[] windowData) {
        DoubleFFT_1D fft = new DoubleFFT_1D(windowData.length);
        double[] fftData = new double[2 * windowData.length];
        System.arraycopy(windowData, 0, fftData, 0, windowData.length);
        fft.realForwardFull(fftData);
        return fftData;
    }

    private int findTargetFrequencyIndex(double[] windowData, int windowSize) {
        // 将窗口数据进行 FFT 变换
        double[] fftData = applyFFT(windowData);

        // 计算频率分辨率
        double frequencyResolution = mSampleRate / windowSize;
//        Log.d(TAG, "窗口内进行FFT处理后的频率分辨率: " + frequencyResolution);


        // 初始化最大幅度和对应的索引
        double maxAmplitude = 0;
        int maxAmplitudeIndex = -1;

        // 在频域中查找幅度最大的频率
        for (int i = 0; i < windowSize / 2; i++) {
            double amplitude = Math.sqrt(fftData[2 * i] * fftData[2 * i] + fftData[2 * i + 1] * fftData[2 * i + 1]);
//            double frequency = i * frequencyResolution;
//            Log.d(TAG, "最大幅值的频率-2: " +  frequency);
            // 判断频率是否在目标频率的上下浮动范围内  Math.abs(frequency-targetFrequency) <= frequencyRange &&
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude;
                maxAmplitudeIndex = i;
            }
        }
        double frequency = maxAmplitudeIndex * frequencyResolution;

        if (Math.abs(frequency - targetFrequency) <= frequencyRange){
            Log.d(TAG, "findTargetFrequencyIndex: "+ frequency);
            return maxAmplitudeIndex;
        }else{
            return -1;
        }

//        Log.d(TAG, "窗口内的最大幅值的频率: " +  frequency);
//
//        Log.d(TAG, "窗口内的最大幅值的频率的索引: " +  maxAmplitudeIndex);


    }


    public void stopRecording() {
        mIsRecording = false;
    }

}


//2464,

