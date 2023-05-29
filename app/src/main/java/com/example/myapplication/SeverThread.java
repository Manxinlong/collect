package com.example.myapplication;

import static com.example.myapplication.CollectSound.collectime;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;




public class SeverThread extends Thread {

    @Override
    public void run() {

//        try {
//            Log.i("yilei", "start server");
//            ServerSocket ss = new ServerSocket(8000);
//            Socket socket = ss.accept();
//            long currentTimeMillis = System.currentTimeMillis();
//            System.out.println("服务端当前时间戳为：" + currentTimeMillis);
//            InputStream is = socket.getInputStream();
//            InputStreamReader isr = new InputStreamReader(is);
//            boolean flag = true;
//            while (flag) {
//                int b;
//                if ((b = isr.read()) != -1) {
//                    System.out.println("接收到的客户端时间戳" + (char)b);
//
//                } else {
//                    Log.i("myapp", "no info");
//                }
////                Thread.sleep(10);
//            }
//
//            System.out.println("准备回写");
//
//            long t2 = System.currentTimeMillis();
//            System.out.println(t2);
//            String s = String.valueOf(t2);
//            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
//            dos.write(s.getBytes());
//
//            socket.close();
//            ss.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}

        try {
                    System.out.println(11111111);
                    Socket socket = new Socket("192.168.137.52", 12345);
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    FileInputStream fis = new FileInputStream(CollectSound.filePath);
//                    //发送麦克风记录时间
                    String s1 = String.valueOf(collectime);
                    while(s1.length()<100){
                        s1 = s1 + " ";
                    }
                    byte[] bytes1 = s1.getBytes();
                    out.write(bytes1);
                    out.flush();
                    //发送图片大小
                    int size = fis.available();
                    String s = String.valueOf(size);
                    while(s.length()<10){
                        s = s + " ";
                    }
                    byte[] bytes = s.getBytes();
                    out.write(bytes);
                    out.flush();
                    //发送图片
                    //读取图片到ByteArrayOutputStream
                    byte[] sendBytes = new byte[1024];
                    int length = 0;
                    while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                        out.write(sendBytes, 0, length);
                        out.flush();
                    }



                    socket.shutdownOutput();




                    socket.close();


                } catch (UnknownHostException e) {
//                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();

                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
    }
}
