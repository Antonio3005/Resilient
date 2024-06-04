package com.example.wellness;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.wellness.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShowPhotoActivity extends AppCompatActivity {
    private String photoPath;

    private ImageView imageView;
    private ImageView home;

    private ImageView camera;

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);

        imageView = findViewById(R.id.image_view2);
        camera = findViewById(R.id.camera_view);
        home = findViewById(R.id.home);

        goHome();

        takePhoto();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        photoPath = image.getAbsolutePath();
        return image;
    }



    private void takePhoto() {
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(ShowPhotoActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ShowPhotoActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
                    return;
                }

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Handle error
                    }
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(ShowPhotoActivity.this,
                                "com.example.wellness.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
            imageView.setImageBitmap(bitmap);
            // Salvare la foto nella galleria
            saveImageToGallery(bitmap);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
            Toast.makeText(this, "Cattura della foto fallita o annullata.", Toast.LENGTH_SHORT).show();
        }
    }

    /*private void saveImageToGallery(Bitmap bitmap) {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timestamp + ".jpg";
        String folderName = timestamp + "_sessione_" + ;

        File imageFile = new File(storageDir, fileName);
        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);  // Prova a mantenere una compressione più bassa
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    private void saveImageToGallery(Bitmap bitmap) {
        // Ottieni la directory Documenti
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        // Ottieni la data corrente e formatta il prefisso del nome della cartella
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String sessionPrefix = date + "_Session";

        // Trova la directory di sessione disponibile
        File sessionDir = null;
        boolean newSessionAllowed = true;
        long currentTime = System.currentTimeMillis();
        long oneHourInMillis = 60 * 60 * 1000;

        for (int i = 1; i <= 3; i++) {
            sessionDir = new File(documentsDir, sessionPrefix + i);
            if (!sessionDir.exists()) {
                if (isLastSessionOlderThanOneHour(documentsDir, sessionPrefix, currentTime, oneHourInMillis)) {
                    sessionDir.mkdirs();
                } else {
                    newSessionAllowed = false;
                }
                break;
            } else if (sessionDir.isDirectory() && !containsImages(sessionDir)) {
                // La cartella esiste e non contiene immagini
                break;
            }
        }

        if (sessionDir == null || (sessionDir.exists() && sessionDir.list() != null && containsImages(sessionDir)) || !newSessionAllowed) {
            Toast.makeText(this, "Numero massimo di sessioni per oggi raggiunto, tutte le cartelle piene o meno di un'ora dall'ultima sessione.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nome file immagine
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timestamp + ".jpg";

        // File immagine
        File imageFile = new File(sessionDir, fileName);
        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(this, "Immagine salvata in: " + imageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Errore nel salvataggio dell'immagine", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean containsImages(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file.isFile() && isImageFile(file)) {
                return true;
            }
        }
        return false;
    }

    private boolean isImageFile(File file) {
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp"};
        String fileName = file.getName().toLowerCase();
        for (String extension : imageExtensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLastSessionOlderThanOneHour(File documentsDir, String sessionPrefix, long currentTime, long oneHourInMillis) {
        for (int i = 3; i >= 1; i--) {
            File sessionDir = new File(documentsDir, sessionPrefix + i);
            if (sessionDir.exists()) {
                long lastModified = sessionDir.lastModified();
                if ((currentTime - lastModified) < oneHourInMillis) {
                    return false;
                }
            }
        }
        return true;
    }


    public void goHome() {
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ShowPhotoActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }


}
