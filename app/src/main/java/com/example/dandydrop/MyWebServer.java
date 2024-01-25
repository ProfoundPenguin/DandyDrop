package com.example.dandydrop;

import com.google.gson.Gson;

import android.content.Context;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MyWebServer extends NanoHTTPD {
    private static final String TAG = "MyWebServer";
    private Context context;
    private ArrayList<FileData> fileUri;

    public MyWebServer(int port, Context applicationContext, ArrayList<FileData> AppFileUrl) {
        super(port);
        this.context = applicationContext;
        this.fileUri = AppFileUrl;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if ("/".equals(uri)) {
            try {
                InputStream inputStream = context.getAssets().open("index.html");
                int available = inputStream.available();
                byte[] buffer = new byte[available];
                inputStream.read(buffer);
                inputStream.close();
                String response = new String(buffer);

                return newFixedLengthResponse(Response.Status.OK, "text/html", response);
            } catch (IOException e) {
                e.printStackTrace();
                return newFixedLengthResponse("Error loading the HTML file");
            }
        } else if (uri.startsWith("/file")) {
            String variableString = uri.substring("/file/".length());

            int variable;

            try {
                variable = Integer.parseInt(variableString);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid variable format: " + variableString);
                return newFixedLengthResponse("Error: Invalid variable format");
            }

            Uri file = null;
            String filename = "unkown";
            try {
                boolean file_found = false;
                for (int i = 0; i < fileUri.size(); i++) {
                    int fileId = fileUri.get(i).getId();
                    int requestId = variable;
                    if(fileId == requestId) {
                        file = fileUri.get(i).getUri();
                        filename = fileUri.get(i).getName();
                        file_found = true;
                    }
                }
                if (!file_found) {
                    return newFixedLengthResponse("404, File Not Found");
                }
            } catch (Exception e) {
                e.printStackTrace();
                return newFixedLengthResponse("Error: " + e.getMessage());
            }

            try {
                if (context == null) {
                    Log.e(TAG, "Context is null");
                    return newFixedLengthResponse("Error: Context is null");
                }

                if (file != null) {
                    ContentResolver contentResolver = context.getContentResolver();
                    InputStream inputStream = contentResolver.openInputStream(file);

                    String mimeType = contentResolver.getType(file);

                    long size = inputStream.available();

                    Log.d(TAG, "Serving image from URI: " + fileUri.toString() + ", Size: " + size + " bytes");

                    // Return the response with Content-Length header
                    Response response = newFixedLengthResponse(Response.Status.OK, mimeType, inputStream, size);
                    response.addHeader("Content-Length", String.valueOf(size));
                    response.addHeader("Content-Disposition", "attachment; filename=" + filename);
                    return response;
                } else {
                    Log.e(TAG, "Image URI is null");
                    return newFixedLengthResponse("Error: Image URI is null");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error loading the image file", e);
                return newFixedLengthResponse("Error loading the image file");
            }

        } else if (uri.startsWith("/getfiles")) {
            String json = new Gson().toJson(fileUri);
            return newFixedLengthResponse(json);
        } else {
            return super.serve(session);
        }
    }
}
