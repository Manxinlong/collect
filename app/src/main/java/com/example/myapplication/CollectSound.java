package com.example.myapplication;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.jtransforms.fft.*;
import static android.content.ContentValues.TAG;


/**
 * Created by Administrator on 2017/6/1 0001.
 */

public class CollectSound {
    private static final double FREQUENCY = 500; //Hz，标准频率（这里分析的是500Hz）
    private static final double RESOLUTION = 10; //Hz，误差
    boolean isRecording = false; //true表示正在录音
    AudioRecord audioRecord=null;
    int bufferSize=0;//最小缓冲区大小，初始化为0
    int sampleRateInHz = 44100;//采样率
    int channelConfig = AudioFormat.CHANNEL_IN_MONO; //单声道
    //int channelConfig = AudioFormat.CHANNEL_IN_STEREO;//双声道，立体音
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //量化位数
    static String filename=null;
    static String filePath = null;
    static long collectime;
    private static final int BUFFER_SIZE = 1024; // 缓冲区大小




    @SuppressLint("MissingPermission")//忽略该警告
    public void start()
    {
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig, audioFormat);//计算最小缓冲区
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRateInHz,channelConfig, audioFormat, bufferSize);//创建AudioRecorder对象
        record();

    }
    //开始录音
    public void record() {

        isRecording = true;
        new Thread(new Runnable() {  //Runnable()函数接口，定义了run函数
            @Override
            public void run() {
                isRecording = true;
                try {
                    byte[] buffer = new byte[bufferSize];
                    audioRecord.startRecording();//开始录音
////
//////                    long currentTimeMillis = System.currentTimeMillis();
////////                    System.out.println("当前时间戳为：" + currentTimeMillis);
////                    collectime = currentTimeMillis;
//                    System.out.println(collectime);
                    byte[] bufferlist = new byte[sampleRateInHz];
                    int j = 0;
                    while (isRecording) {

                        int bufferReadResult = audioRecord.read(buffer,0,bufferSize);//从音频硬件录制缓冲区读取数据，直接复制到指定缓冲区
//                        long currentTimeMillis = System.currentTimeMillis();
//                        System.out.println("当前时间戳为：" + currentTimeMillis);


                        for (int i = 0; i < bufferReadResult; i++)
                        {

                            if(j<sampleRateInHz){
                                bufferlist[j] = buffer[i];
                                j += 1;
                            } else{
                                j = 0;
                            }
                        }

                        newfrequencyAnalyse(buffer);
                    }
                    short[] audioData = new short[BUFFER_SIZE];
                    int readSize;
                    double startTime = 0;

                    audioRecord.stop();//停止录音
//                    System.out.println("录音文件大小"+recordingFile.length());


                } catch (Throwable t) {
                    Log.e(TAG, "Recording Failed");//如果try语句异常，则输出Recording Failed
                }
            }
        }).start();//start()里有运行时异常，这种异常我们不需要处理，完全由虚拟机接管
    }
    //停止录音
    public void stopRecording()
    {
        isRecording = false;
    }


//    //对录音文件进行分析
//    public void frequencyAnalyse(){
//        if(recordingFile == null){return;
//        }
//        try {
//            DataInputStream inputStream = new DataInputStream(new FileInputStream(recordingFile));
//            //16bit采样，因此用short[]
//            //如果是8bit采样，这里直接用byte[]
//            //从文件中读出一段数据，这里长度是SAMPLE_RATE，也就是1s采样的数据
//            short[] buffer=new short[sampleRateInHz];
//            for(int i = 0;i<buffer.length;i++){
//                buffer[i] = inputStream.readShort();
//            }
//            short[] data = new short[FFT.FFT_N];
//
//            //为了数据稳定，在这里FFT分析只取最后的FFT_N个数据
//            System.arraycopy(buffer, buffer.length - FFT.FFT_N,
//                    data, 0, FFT.FFT_N);
//
//            //FFT分析得到频率
//            double frequence = FFT.GetFrequency(data);
//            if(Math.abs(frequence - FREQUENCY)<RESOLUTION){
//                //测试通过
//            }else{
//                //测试失败
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    // byte数组转short数组
    public static short[] byteArray2ShortArray(byte[] data) {
        short[] retVal = new short[data.length/2];
        for (int i = 0; i < retVal.length; i++)
            retVal[i] = (short) ((data[i * 2] & 0xff) | (data[i * 2 + 1] & 0xff) << 8);

        return retVal;
    }

////不足FFT_N的大小就填充0
//    public static void fillZeroIfNeeded(byte[] data) {
//        if (data.length < 4096) {
//            byte[] zeroArray = new byte[4096 - data.length];
//            Arrays.fill(zeroArray, (byte)0);
//            System.arraycopy(zeroArray, 0, data, data.length-1, zeroArray.length);
//        }
//    }

//    （时长 频率） 函数
//
    public void newfrequencyAnalyse(byte[] buffer){
        try {

//            fillZeroIfNeeded(buffer);
            //16bit采样，因此用short[]
            //如果是8bit采样，这里直接用byte[]
            //从文件中读出一段数据，这里长度是SAMPLE_RATE，也就是1s采样的数据
            short[] data = new short[FFT.FFT_N];

            //为了数据稳定，在这里FFT分析只取最后的FFT_N个数据
            short[] byteArray = byteArray2ShortArray(buffer);

//            System.arraycopy(byteArray, 0, data, 0, FFT.FFT_N);

            //FFT分析得到频率
            double frequence = FFT.GetFrequency(data);
            if(Math.abs(frequence - FREQUENCY)<RESOLUTION){
                //测试通过
            }else{
                //测试失败
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}





