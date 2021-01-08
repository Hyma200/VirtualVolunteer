package com.example.virtualvolunteer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class Profile extends AppCompatActivity {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    DatabaseReference myRef = database.getReference("Users").child(user.getEmail().replace('.', '_'));
    DatabaseReference nameRef = myRef.child("name");
    DatabaseReference locationRef = myRef.child("location");
    DatabaseReference hoursRef = myRef.child("hours");
    DatabaseReference ageRef = myRef.child("age");
    DatabaseReference imageRef = myRef.child("image");
    private TextView nameOutput;
    private TextView locationOutput;
    private TextView hoursOutput;
    private static final String TAG = "Database";
    private Button chooseImage;
    private Button uploadImage;
    private EditText fileName;
    private ImageView image;
    private ProgressBar progressBar;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private StorageReference storageRef = FirebaseStorage.getInstance().getReference("uploads");
    private DatabaseReference databaseRef = myRef.child("image");
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        BottomNavigationView navView = findViewById(R.id.Bottom_navigation_icon);
        Navigation.enableNavigationClick(this, navView);
        Menu menu = navView.getMenu();
        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);
        nameOutput = (TextView) findViewById(R.id.profileName);
        hoursOutput = (TextView) findViewById(R.id.hoursProfile);
        locationOutput = (TextView) findViewById(R.id.location);
        chooseImage = (Button) findViewById(R.id.image_picker);
        uploadImage = (Button) findViewById(R.id.button_upload);
        fileName = (EditText) findViewById(R.id.file_name);
        image = (ImageView) findViewById(R.id.image);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        imageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    Upload upload = snapshot.getValue(Upload.class);
                    Picasso.with(Profile.this)
                            .load(upload.getImageUrl())
                            .fit()
                            .centerCrop().into(image);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Profile.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Sets value of name on profile page
        nameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                String value = snapshot.getValue(String.class);
                nameOutput.setText(value);
                if (value == null)
                    nameOutput.setText("Add Your Name to Your Profile");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                nameOutput.setText("ERROR: " + error.toException());
            }
        });

        //Sets location on profile page
        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                String value = snapshot.getValue(String.class);
                locationOutput.setText(value);
                if (value == null)
                    locationOutput.setText("Add Your Location");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                locationOutput.setText("ERROR: " + error.toException());
            }
        });

        //Sets hours on profile page
        hoursRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                Long value = snapshot.getValue(Long.class);
                if (value == null)
                    hoursOutput.setText("0 Hours");
                else
                    hoursOutput.setText(value.toString() + " Hours");

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hoursOutput.setText("ERROR: " + error.toException());
            }
        });

        //Sets age on profile page
        ageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot){
                String value = snapshot.getValue(String.class);
                //ageOutput.setText(value);
             //   if (value == null)
              //      ageOutput.setText("Please enter your date of birth");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
               // ageOutput.setText("ERROR: " + error.toException());
            }
        });

        chooseImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openFileChooser();
            }
        });

        uploadImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (uploadTask != null && uploadTask.isInProgress())
                    Toast.makeText(Profile.this, "Upload in progress", Toast.LENGTH_SHORT).show();
                else
                    uploadFile();
            }
        });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadFile(){
        if (imageUri != null){
            StorageReference fileReference = storageRef.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));
            uploadTask = fileReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(0);
                        }
                    }, 500);

                    Toast.makeText(Profile.this, "Upload successful",Toast.LENGTH_LONG).show();
                    String downloadUrl = "";
                    if(taskSnapshot.getMetadata() != null){
                        Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Upload upload = new Upload(fileName.getText().toString().trim(), uri.toString());
                                databaseRef.setValue(upload);
                            }
                        });
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(Profile.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressBar.setProgress((int) progress);
                }
            });
        }
        else
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
    }

    private void openFileChooser(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            Picasso.with(this).load(imageUri).into(image);
        }
    }
}
