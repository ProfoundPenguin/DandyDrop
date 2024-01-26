package com.example.dandydrop;

import java.io.File;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

class FileData {
    private static int lastId = 0;
    private final int id;
    private final Uri uri;
    private final String name;
    private final long filesize;

    // Constructor
    public FileData(Uri uri, String name, long filesize) {
        this.id = ++lastId;
        this.uri = uri;
        this.name = name;
        this.filesize = filesize;
    }
    public int getId() {
        return id;
    }
    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getReadableFilesize() {
        if (filesize <= 0) {
            return "0 B";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(filesize) / Math.log10(1024));

        return String.format("%.1f %s", filesize / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}



public class MainActivity extends AppCompatActivity implements RecyclerViewInterface {
    private static final int REQUEST_CODE_PICK_FILES = 1;
    private static final int REQUEST_CODE_PICK_MORE_FILES = 2;

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();

        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            fileName = uri.getLastPathSegment();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex);
                }
                cursor.close();
            }
        }

        return fileName;
    }
    private long getFileSizeFromUri(Uri uri) {
        long fileSize = -1;
        String scheme = uri.getScheme();

        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                File file = new File(uri.getPath());
                fileSize = file.length();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex);
                }
                cursor.close();
            }
        }

        return fileSize;
    }

    ArrayList<FileData> fileDataList = new ArrayList<>();
    List<Integer> selected = new ArrayList();
    ArrayList<FileData> latestDataInstance = new ArrayList<>();
    private MyWebServer webServer;
    private boolean standardLunch = false;
    private boolean same_network = false;

    private void setupMainPage() {
        setContentView(R.layout.activity_main);

        TextView developedTextView = findViewById(R.id.developed);

        CharSequence text = developedTextView.getText();
        SpannableString spannableString = new SpannableString(text);
        int startIndex = text.toString().indexOf("Habib");
        int endIndex = startIndex + "Habib".length();

        spannableString.setSpan(new UnderlineSpan(), startIndex, endIndex, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        developedTextView.setText(spannableString);
        developedTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://habib.pythonanywhere.com";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        Button exploreButton = findViewById(R.id.openFile);

        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }

            private void openFilePicker() {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                Intent chooserIntent = Intent.createChooser(chooseFile, "Choose a file");

                if (chooserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(chooserIntent, REQUEST_CODE_PICK_FILES);
                }
            }


        });

        TextView textView = findViewById(R.id.report_bug);

        String linkText = "Report an issue";
        SpannableString report_bug = new SpannableString(linkText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Open the link in a browser
                String url = "http://www.google.com";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        };

        report_bug.setSpan(clickableSpan, 0, linkText.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(report_bug);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        handleIntent(intent);


        if (standardLunch) {
            setupMainPage();
        } else {
            setupSharingPage(fileDataList, false);
        }

    }

    private void stop(){
        if (webServer != null) {
            webServer.stop();
        }
        fileDataList = new ArrayList<>();
        setupMainPage();
        same_network = false;
        standardLunch = true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FILES && resultCode == RESULT_OK) {
            if (data != null) {
                handleSelected(data);
            }
        } else if (requestCode == REQUEST_CODE_PICK_MORE_FILES && resultCode == RESULT_OK) {
            if (data != null) {
                handleSelected(data, true);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        handleIntent(intent);
    }

    private void setUnderlinedText(TextView textView, String text) {
        SpannableString content = new SpannableString(text);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);

        textView.setText(content);
    }

    private void setupSharingPage(ArrayList<FileData> fileDataList, boolean addingFiles) {
        System.out.println(addingFiles);
        if (!addingFiles) {
            setContentView(R.layout.sharing);
        }

        Button morefiles = findViewById(R.id.morefiles);

        morefiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
            private void openFilePicker() {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, REQUEST_CODE_PICK_MORE_FILES);
            }
        });

        Button stop = findViewById(R.id.immediateStop);

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        if (!same_network) {
            Button firststepdone = findViewById(R.id.firststepdone);

            firststepdone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    qr_ip();
                }
            });

            LinearLayout myLinearLayout = findViewById(R.id.secondstep);

            myLinearLayout.setVisibility(View.GONE);
        } else {
            LinearLayout myLinearLayout = findViewById(R.id.firststep);

            myLinearLayout.setVisibility(View.GONE);
        }

        Button deleteSelected = findViewById(R.id.deleteSelected);
        deleteSelected.setVisibility(View.INVISIBLE);

        deleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete(selected, true);
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FileDataAdapter adapter = new FileDataAdapter(this, fileDataList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        webServer = new MyWebServer(8080, getApplicationContext(), fileDataList);
        try {
            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        TextView fileCount = findViewById(R.id.filecount);
        fileCount.setText("Sharing: "+fileDataList.size()+" Files");
    }

    private void qr_ip() {
        TextView theLink = findViewById(R.id.thelink);

        SpannableString passContent;

        String localIpAddress = NetworkUtils.getLocalIpAddress(getApplicationContext());
        if (localIpAddress != null) {
            // Do something with the local IP address
            passContent = new SpannableString("http://"+localIpAddress+":8080");
        } else {
            passContent = new SpannableString("Not Available");
        }

        ImageView imageView = findViewById(R.id.urlQR);

        QRCodeGenerator qrCodeGenerator = new QRCodeGenerator();
        qrCodeGenerator.setListener(new QRCodeGenerator.QRCodeListener() {
            @Override
            public void onQRCodeGenerated(Bitmap bitmap) {
                // Use the generated QR code bitmap as needed
                if (bitmap != null) {
                    // Display the bitmap in the ImageView
                    imageView.setImageBitmap(bitmap);
                }
            }
        });

        qrCodeGenerator.execute("http://"+localIpAddress+":8080");

        passContent.setSpan(new UnderlineSpan(), 0, passContent.length(), 0);
        theLink.setText(passContent);

        LinearLayout myLinearLayout = findViewById(R.id.firststep);

        myLinearLayout.setVisibility(View.GONE);

        LinearLayout secondstep = findViewById(R.id.secondstep);

        secondstep.setVisibility(View.VISIBLE);

        Button refreship = findViewById(R.id.refresh_ip);

        refreship.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qr_ip();
            }
        });

        same_network = true;
    }

    private void handleSelected(Intent intent, boolean addingMoreFiles) {
        if (webServer != null) {
            webServer.stop();
        }

        standardLunch = false;

        ClipData clipData = intent.getClipData();

        latestDataInstance = new ArrayList<>();

        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri fileUri = clipData.getItemAt(i).getUri();

                String name = getFileNameFromUri(fileUri);
                long size = getFileSizeFromUri(fileUri);

                boolean dublicate = false;
                for (FileData fileData : fileDataList) {
                    if ((fileData.getUri()).equals(fileUri)) {
                        dublicate = true;
                        break;
                    }
                }

                if (!dublicate) {
                    fileDataList.add(new FileData(fileUri, name, size));
                    latestDataInstance.add(new FileData(fileUri, name, size));
                }
            }
        } else {
            Uri fileUri = intent.getData();
            if(fileUri != null) {
                String name = getFileNameFromUri(fileUri);
                long size = getFileSizeFromUri(fileUri);

                boolean dublicate = false;
                for (FileData fileData : fileDataList) {
                    if ((fileData.getUri()).equals(fileUri)) {
                        dublicate = true;
                        break;
                    }
                }
                if (!dublicate) {
                    fileDataList.add(new FileData(fileUri, name, size));
                    latestDataInstance.add(new FileData(fileUri, name, size));
                }
            }

        }

        setupSharingPage(fileDataList, true);
    }

    private void handleSelected(Intent intent) {
        if (webServer != null) {
            webServer.stop();
        }

        standardLunch = false;

        ClipData clipData = intent.getClipData();

        latestDataInstance = new ArrayList<>();

        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri fileUri = clipData.getItemAt(i).getUri();

                String name = getFileNameFromUri(fileUri);
                long size = getFileSizeFromUri(fileUri);

                boolean dublicate = false;
                for (FileData fileData : fileDataList) {
                    if ((fileData.getUri()).equals(fileUri)) {
                        dublicate = true;
                        break;
                    }
                }

                if (!dublicate) {
                    fileDataList.add(new FileData(fileUri, name, size));
                    latestDataInstance.add(new FileData(fileUri, name, size));
                }
            }
        } else {
            Uri fileUri = intent.getData();
            if(fileUri != null) {
                String name = getFileNameFromUri(fileUri);
                long size = getFileSizeFromUri(fileUri);

                boolean dublicate = false;
                for (FileData fileData : fileDataList) {
                    if ((fileData.getUri()).equals(fileUri)) {
                        dublicate = true;
                        break;
                    }
                }
                if (!dublicate) {
                    fileDataList.add(new FileData(fileUri, name, size));
                    latestDataInstance.add(new FileData(fileUri, name, size));
                }
            }

        }

        setupSharingPage(fileDataList, false);
    }

    @SuppressLint("UnsafeIntentLaunch")
    private void handleIntent(Intent intent) {
        if (webServer != null) {
            webServer.stop();
        }

        String action = intent.getAction();
        String type = intent.getType();

        latestDataInstance = new ArrayList<>();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(fileUri != null){
                String name = getFileNameFromUri(fileUri);
                long size = getFileSizeFromUri(fileUri);

                boolean dublicate = false;
                for (FileData fileData : fileDataList) {
                    if ((fileData.getUri()).equals(fileUri)) {
                        dublicate = true;
                        break;
                    }
                }
                if (!dublicate) {
                    fileDataList.add(new FileData(fileUri, name, size));
                    latestDataInstance.add(new FileData(fileUri, name, size));
                }
            }

            setupSharingPage(fileDataList, false);

        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Uri> fileUriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

            if (fileUriList != null) {
                for (int i = 0; i < fileUriList.size(); i++) {
                    Uri fileUri = fileUriList.get(i);

                    String name = getFileNameFromUri(fileUri);
                    long size = getFileSizeFromUri(fileUri);

                    boolean dublicate = false;
                    for (FileData fileData : fileDataList) {
                        if ((fileData.getUri()).equals(fileUri)) {
                            dublicate = true;
                            break;
                        }
                    }

                    if (!dublicate) {
                        fileDataList.add(new FileData(fileUri, name, size));
                        latestDataInstance.add(new FileData(fileUri, name, size));
                    }
                }
            }

            setupSharingPage(fileDataList, false);
        } else {
            standardLunch = true;
        }

    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webServer != null) {
            webServer.stop();
        }
    }

    @Override
    public void onFileDelete(int position) {
        List<Integer> file = new ArrayList<>();
        file.add(fileDataList.get(position).getId());

        delete(file, false);
    }



    @Override
    public void onFileSelect(int position, ImageView check_box) {
        int id = fileDataList.get(position).getId();
        if (selected.contains(id)) {
            int indexToRemove = -1;
            for (int i = 0; i < selected.size(); i++) {
                if (id == selected.get(i)) {
                    indexToRemove = i;
                    break;
                }
            }

            if (indexToRemove != -1) {
                selected.remove(indexToRemove);
            }
            check_box.setImageResource(R.drawable.ic_checkbox_unchecked);
        } else {
            selected.add(id);
            check_box.setImageResource(R.drawable.ic_checkbox_checked);
        }

        Button deleteSelected = findViewById(R.id.deleteSelected);

        if(selected.size() == 0) {
            deleteSelected.setVisibility(View.INVISIBLE);
        } else {
            deleteSelected.setVisibility(View.VISIBLE);
        }
    }



    public void delete(List<Integer> thefiles, boolean group) {
        if (group) {
            selected = new ArrayList();
        }
        List<Integer> files = thefiles;
        for (int x = 0;x < files.size();x++) {
            int id = files.get(x);

            for (int y = 0;y < fileDataList.size();y++) {
                FileData fileData = fileDataList.get(y);

                if (fileData.getId() == id) {
                    fileDataList.remove(y);
                }
            }
        }

        Button deleteSelected = findViewById(R.id.deleteSelected);

        if(selected.size() == 0) {
            deleteSelected.setVisibility(View.GONE);
        } else {
            deleteSelected.setVisibility(View.VISIBLE);
        }

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FileDataAdapter adapter = new FileDataAdapter(this, fileDataList, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
}