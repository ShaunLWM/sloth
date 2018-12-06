package dev.blacksheep.slothy;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class GalleryActivity extends BaseActivity {
    FloatingActionButton fab;
    FirebaseStorage storage;
    FirebaseFirestore db;
    SharedPreferences sharedPref;
    String TAG = "GalleryActivity";
    private int requestMode = 12345;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_picker);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        fab = findViewById(R.id.fab);
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == requestMode) {
                final Uri selectedUri = data.getData();
                if (selectedUri != null) {
                    startCrop(selectedUri);
                } else {
                    Toast.makeText(GalleryActivity.this, R.string.toast_cannot_retrieve_selected_image, Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == UCrop.REQUEST_CROP) {
                handleCropResult(data);
            }
        }
        if (resultCode == UCrop.RESULT_ERROR) {
            handleCropError(data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleCropResult(@NonNull Intent result) {
        final Uri resultUri = UCrop.getOutput(result);
        if (resultUri != null) {
            uploadImage(resultUri);
        } else {
            Toast.makeText(GalleryActivity.this, R.string.toast_cannot_retrieve_cropped_image, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void handleCropError(@NonNull Intent result) {
        final Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            Log.e(TAG, "handleCropError: ", cropError);
            Toast.makeText(GalleryActivity.this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(GalleryActivity.this, R.string.toast_unexpected_error, Toast.LENGTH_SHORT).show();
        }
    }


    private void startCrop(@NonNull Uri uri) {
        UCrop.Options options = new UCrop.Options();
        options.setFreeStyleCropEnabled(true);
        String destinationFileName = Utils.randomString(10) + ".png";
        UCrop uCrop = UCrop.of(uri, Uri.fromFile(new File(getCacheDir(), destinationFileName)));
        uCrop = uCrop.withOptions(options);
        uCrop.start(GalleryActivity.this);
    }

    private void uploadImage(final Uri filePath) {
        showProgressDialog();
        final String extension = filePath.toString().substring(filePath.toString().lastIndexOf(".")); // Extension with dot .jpg, .png
        StorageReference storageRef = storage.getReference();
        final String finalFileName = Utils.randomString(10) + extension;
        StorageReference imageRef = storageRef.child(finalFileName);
        UploadTask uploadTask = imageRef.putFile(filePath);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Log.e("ERROR UPLOADING", exception.getMessage());
                Toast.makeText(GalleryActivity.this, "Error Uploading: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Log.e("SUCCESSFUL UPLOAD", taskSnapshot.getUploadSessionUri().toString());
                File f = new File(getCacheDir(), filePath.getLastPathSegment());
                try {
                    //  Log.e("Path", f.getPath() + "|" + f.getAbsolutePath());
                    // Log.e("Exist", f.exists() + "");
                    f.delete();
                    // Log.e("Deleted", "Temporary file deleted.");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                sendToFunctions(finalFileName);
            }
        });
    }

    private void sendToFunctions(final String finalFileName) {
        final String partnerEmail = sharedPref.getString("partneremail", "");
        sendToDatabase(partnerEmail, finalFileName);
        return;
        // Log.e("PartnerEmail", partnerEmail);
        /*
        DocumentReference docRef = db.collection("users").document(partnerEmail);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //Log.e(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> data = document.getData();
                        sendToDatabase(partnerEmail, finalFileName, data.get("fcm").toString());
                    } else {
                        Log.e(TAG, "No such document");
                    }
                } else {
                    Log.e(TAG, "get failed with ", task.getException());
                }
            }
        });*/
    }

    private void sendToDatabase(String email, String finalFileName) {
        db.collection("messages").document().set(new Message(email, finalFileName)).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                hideProgressDialog();
                Toast.makeText(GalleryActivity.this, "Successfully sent!", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideProgressDialog();
                Toast.makeText(GalleryActivity.this, "Failed to send: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.permission_read_storage_rationale),
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                    .setType("image/*")
                    .addCategory(Intent.CATEGORY_OPENABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                String[] mimeTypes = {"image/jpeg", "image/png"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            }

            startActivityForResult(Intent.createChooser(intent, getString(R.string.label_select_picture)), requestMode);
        }
    }
}
