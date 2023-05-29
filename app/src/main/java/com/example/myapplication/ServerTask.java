package com.example.myapplication;

import static com.example.myapplication.CS.RecordTime;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerTask implements Runnable {
    private volatile boolean isCancelled = false;
    private static final int SERVER_PORT = 8080;
    private ServerSocket mServerSocket = null;
    public static long TransTime;

    Socket clientSocket = null;

    private Thread serverThread;

    //    public void startTask() {
//        stopTask();
//        if (serverThread == null || !serverThread.isAlive()) {
//            serverThread = new Thread(this);
//            serverThread.start();
//        }
//    }
    public void startTask() {
        stopTask(); // 先停止任务

        // 显式关闭先前的mServerSocket实例
//        if (mServerSocket != null && !mServerSocket.isClosed()) {
//            try {
//                mServerSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        // 创建新的线程并启动任务
        serverThread = new Thread(this);
        serverThread.start();
    }

    public void stopTask() {
        isCancelled = true;

        // 显式关闭mServerSocket实例
        if (mServerSocket != null && !mServerSocket.isClosed()) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void run() {
        isCancelled = false;

        while (!isCancelled) {
            System.out.println("Server started at port " + SERVER_PORT);
            try {
                mServerSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }


            while (!isCancelled()) {
                try {
                    clientSocket = mServerSocket.accept();
                    if (isCancelled()) {
                        break; // 检查任务是否被取消，如果是则跳出循环
                    }
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    String msg;
                    while ((msg = in.readLine()) != null) {
                        Long new_msg = Long.parseLong(msg);
                        long renew_msg = new_msg.longValue();
                        Log.d("TAG", "发送时间: " + renew_msg);
                        TransTime = renew_msg;

                    }

                    long diff = RecordTime - TransTime;
                    Log.d("mxl", "传输时间的误差: " + diff);

                    if (isCancelled()) {
                        clientSocket.close();
                        break; // 检查任务是否被取消，如果是则跳出循环
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (mServerSocket != null && !mServerSocket.isClosed()) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close server socket: " + e.getMessage());
            }
        }
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}


