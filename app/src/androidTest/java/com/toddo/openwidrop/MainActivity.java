package com.toddo.openwidrop;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private FileServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        TextView portText = findViewById(R.id.portText);
        //TextView urlText = findViewById(R.id.urlText);
        ImageView qrImage = findViewById(R.id.qrImage);

        try {

            String ip = NetworkUtils.getLocalIp(this);

            int port = 8080;


            String url = " http://" + ip + ":" + port;
            String setport = "Port: " + port;

            //urlText.setText(url);
            portText.setText("Port");

            Bitmap qr = QRUtils.generateQR(url);

            qrImage.setImageBitmap(qr);

            File dir = new File("/storage/emulated/0/Download");

            server = new FileServer(port,dir);

            server.start();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if(server!=null){
            server.stop();
        }
    }
}