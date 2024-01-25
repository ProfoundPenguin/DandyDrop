package com.example.dandydrop;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeGenerator extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = "QRCodeGenerator";
    private QRCodeListener listener;

    public interface QRCodeListener {
        void onQRCodeGenerated(Bitmap bitmap);
    }

    public void setListener(QRCodeListener listener) {
        this.listener = listener;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        String url = params[0];
        int width = 500;
        int height = 500;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);
            return createBitmap(bitMatrix);
        } catch (WriterException e) {
            Log.e(TAG, "Error generating QR code: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (listener != null) {
            listener.onQRCodeGenerated(bitmap);
        }
    }

    private Bitmap createBitmap(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFFE8E2E2 : 0xFF282828);
            }
        }

        return bitmap;
    }
}