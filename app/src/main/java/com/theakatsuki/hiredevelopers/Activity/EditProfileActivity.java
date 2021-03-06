package com.theakatsuki.hiredevelopers.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.theakatsuki.hiredevelopers.Model.User;
import com.theakatsuki.hiredevelopers.R;

import java.util.HashMap;

public class EditProfileActivity extends AppCompatActivity {

    ImageView imageView;
    EditText name, country, work,email,password,confirmPassword,number;
    Button btnEdit;
    FirebaseUser firebaseUser;

    StorageReference storageReference;
    private Uri imageURl;
    private StorageTask<UploadTask.TaskSnapshot> uploadsTask;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        imageView = findViewById(R.id.editProfileImage);
        name = findViewById(R.id.editName);
        country = findViewById(R.id.editCountry);
        number = findViewById(R.id.editPhoneNumber);
        work = findViewById(R.id.editWorking);
        email = findViewById(R.id.editEmail);
        password = findViewById(R.id.editPassword);
        confirmPassword = findViewById(R.id.editConfirmPassword);
        progressBar =findViewById(R.id.editProgressBar);
        btnEdit = findViewById(R.id.btnEditProfile);

        firebaseUser= FirebaseAuth.getInstance().getCurrentUser();
        storageReference = FirebaseStorage.getInstance().getReference("ProfileImages");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                name.setText(user.getFullname());
                country.setText(user.getCountry());
                number.setText(user.getPhoneNumber());
                work.setText(user.getWork());
                email.setText(user.getEmail());
                password.setText(user.getPassword());
                if(user.getProfileImage().equals("Default"))
                {
                    imageView.setImageResource(R.drawable.male);
                }
                else
                {
                    Glide.with(getApplicationContext()).load(user.getProfileImage()).into(imageView);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(password.getText().toString().equals(confirmPassword.getText().toString()))
                {
                    progressBar.setVisibility(View.VISIBLE);
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("country",country.getText().toString());
                    hashMap.put("email",email.getText().toString());
                    hashMap.put("fullname",name.getText().toString());
                    hashMap.put("password",password.getText().toString());
                    hashMap.put("phoneNumber",number.getText().toString());
                    hashMap.put("work",work.getText().toString());
                    databaseReference.updateChildren(hashMap);
                    uploadImage();

                    name.setText("");
                    confirmPassword.setText("");
                    password.setText("");
                    number.setText("");
                    country.setText("");
                    work.setText("");
                    email.setText("");
                }
                else
                {
                    Toast.makeText(EditProfileActivity.this, "Passwords didn't match", Toast.LENGTH_SHORT).show();
                }


            }
        });
    }
    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,1);
    }
    private String getFileExtension(Uri uri)
    {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( resultCode == RESULT_OK && data != null )
        {
            imageURl =data.getData();
            imageView.setImageURI(imageURl);
        }
    }


    private void uploadImage()
    {

        if(imageURl !=null)
        {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageURl));
            uploadsTask =fileReference.putFile(imageURl);
            uploadsTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>(){
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful())
                    {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {

                    Uri downloadUri = task.getResult();
                    String mUri = downloadUri.toString();
                    if (task.isSuccessful())
                    {
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                        HashMap<String,Object> hashMap = new HashMap<>();
                        hashMap.put("profileImage",mUri);
                        reference.child("Users").child(firebaseUser.getUid()).updateChildren(hashMap);
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditProfileActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {

                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        else {
            Toast.makeText(EditProfileActivity.this, "Profile Uploaded", Toast.LENGTH_SHORT).show();
        }

    }
}