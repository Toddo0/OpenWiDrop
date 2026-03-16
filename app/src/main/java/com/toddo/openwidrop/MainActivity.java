package com.toddo.openwidrop;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.graphics.Bitmap;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    TextView urlText;
    EditText portText;
    EditText rootPathText;
    Button selectFolderButton;
    int port = 8080;
    String ip;
    File dir;
    Uri folderUri;

    private FileServer server;
    private static final String PREFS_NAME = "OpenWidropPrefs";
    private static final String KEY_FOLDER_URI = "folder_uri";
    public void openFolderPicker(View view) {
        openFolderPicker();
    }
    // Activity result launcher for folder selection
    private final ActivityResultLauncher<Intent> folderSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    handleFolderSelection(data);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Önce view'ları initialize et
        initializeViews();

        // Kaydedilmiş klasör URI'sini yükle (dir burada set ediliyor)
        loadSavedFolderUri();

        // İzin kontrolü yap
        if (checkPermission()) {
            startServer();
        } else {
            requestPermission();
        }
    }

    private void initializeViews() {
        portText = findViewById(R.id.portText);
        urlText = findViewById(R.id.urlText);
        rootPathText = findViewById(R.id.rootPathText);
        selectFolderButton = findViewById(R.id.selectFolderButton);
        // ImageView qrImage = findViewById(R.id.qrImage); // Burada kullanılmıyor
    }
    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) ve üzeri için
            return Environment.isExternalStorageManager();
        } else {
            // Android 10 ve altı için
            int readCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writeCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readCheck == PackageManager.PERMISSION_GRANTED && writeCheck == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ için kullanıcıyı ayarlar ekranına yönlendir
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
            }
        } else {
            // Android 10 ve altı için normal izin isteği
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void openFolderPicker() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        folderSelectionLauncher.launch(intent);
    }

    private void handleFolderSelection(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            try {
                // Take persistable permission for the URI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    final int takeFlags = data.getFlags() &
                            (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                }

                // Save the URI
                folderUri = uri;
                saveFolderUri(uri.toString());

                // Get folder name for display
                String folderName = getFolderName(uri);
                if (folderName != null) {
                    rootPathText.setText(folderName);
                } else {
                    rootPathText.setText(uri.getPath());
                }

                // Convert URI to File path if possible
                dir = getFileFromUri(uri);

                // Restart server with new directory
                restartServer();

                Toast.makeText(this, "Klasör seçildi: " + folderName, Toast.LENGTH_SHORT).show();

            } catch (SecurityException e) {
                e.printStackTrace();
                Toast.makeText(this, "Klasör izni alınamadı!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getFolderName(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
                if (documentFile != null) {
                    return documentFile.getName();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uri.getLastPathSegment();
    }

    private File getFileFromUri(Uri uri) {
        // For Android 10 and above, we might need to use DocumentFile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
                if (documentFile != null && documentFile.isDirectory()) {
                    // Return the path from URI
                    String path = uri.getPath();
                    if (path != null && path.contains(":")) {
                        String[] parts = path.split(":");
                        if (parts.length > 1) {
                            return new File("/storage/emulated/0/" + parts[1]);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fallback to default Download folder
        return new File("/storage/emulated/0/Download");
    }

    private void saveFolderUri(String uriString) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_FOLDER_URI, uriString).apply();
    }

    private void loadSavedFolderUri() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUri = prefs.getString(KEY_FOLDER_URI, "");

        if (!savedUri.isEmpty()) {
            try {
                folderUri = Uri.parse(savedUri);

                // Check if we still have permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        getContentResolver().takePersistableUriPermission(folderUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (SecurityException e) {
                        // Permission lost, clear saved URI
                        folderUri = null;
                        saveFolderUri("");
                    }
                }

                if (folderUri != null) {
                    String folderName = getFolderName(folderUri);
                    if (folderName != null) {
                        rootPathText.setText(folderName);
                    }
                    dir = getFileFromUri(folderUri);
                }
            } catch (Exception e) {
                e.printStackTrace();
                folderUri = null;
            }
        }

        // If no saved folder or error, use default Download folder
        if (dir == null) {
            dir = new File("/storage/emulated/0/Download");
            rootPathText.setText("Download");
        }
    }

    private void startServer() {
        try {
            // dir null mı kontrol et
            if (dir == null) {
                Log.e("MainActivity", "dir is null, using default");
                dir = new File("/storage/emulated/0/Download");
                rootPathText.setText("Download");
            }

            // Klasörün var olduğundan emin ol
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e("MainActivity", "Failed to create directory: " + dir.getPath());
                }
            }

            // Klasör yazılabilir mi kontrol et (null check ile)
            if (dir.exists() && !dir.canWrite()) {
                Toast.makeText(this, "Klasör yazılabilir değil: " + dir.getPath(), Toast.LENGTH_LONG).show();
            }

            String ip = NetworkUtils.getLocalIp();
            String url = "http://" + ip + ":" + port;
            urlText.setText(url);

            Bitmap qr = QRUtils.generateQR(url);
            ImageView qrImage = findViewById(R.id.qrImage);
            if (qrImage != null) {
                qrImage.setImageBitmap(qr);
            }

            // Stop existing server if running
            if (server != null) {
                server.stop();
            }

            // Start new server - dir null değilse
            if (dir != null) {
                server = new FileServer(port, dir);
                server.start();
                Toast.makeText(this, "Sunucu başlatıldı: " + url, Toast.LENGTH_SHORT).show();
            } else {
                Log.e("MainActivity", "Cannot start server: dir is null");
                Toast.makeText(this, "Klasör seçilmedi!", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            urlText.setText("Hata: " + e.getMessage());
        }
    }

    private void restartServer() {
        if (server != null) {
            server.stop();
        }
        startServer();
    }

    public void portUpdate(View view) {
        try {
            // Get new port number
            String portStr = portText.getText().toString();
            if (!portStr.isEmpty()) {
                port = Integer.parseInt(portStr);
            }

            // Restart server with new port
            restartServer();

        } catch (NumberFormatException e) {
            urlText.setText("Geçersiz port numarası!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            urlText.setText("Hata oluştu: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
    }
}