package com.toddo.openwidrop;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRUtils {

    public static Bitmap generateQR(String text) throws Exception {

        int size = 512;

        QRCodeWriter writer = new QRCodeWriter();
        var bits = writer.encode(text, BarcodeFormat.QR_CODE, size, size);

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

        for(int x=0;x<size;x++){
            for(int y=0;y<size;y++){

                bmp.setPixel(
                        x,
                        y,
                        bits.get(x,y) ? 0xFF000000 : 0xFFFFFFFF
                );
            }
        }

        return bmp;
    }
}