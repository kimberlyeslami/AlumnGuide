package com.example.alumnguide;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;


public class AddPostActivity extends AppCompatActivity {
    ActionBar actionBar;

    FirebaseAuth firebaseAuth;
    DatabaseReference userDbref;

    //permission constraints
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //image pick constraints
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    //permissions array
    String[] cameraPermissions;
    String[] storagePermissions;

    //views
    EditText titleEt, descriptionET;
    ImageView imageIv;
    Button uploadBtn;

    //user info
    String name, email, uid, dp;

    //info of post to be edited
    String editTitle,editDescription,editImage;
    //image picked will be the same
    Uri image_uri = null;
    //progress bar
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Post");
        //enable back btn
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //init permission arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        pd = new ProgressDialog(this);


        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        //initialise views
        titleEt = findViewById(R.id.pTitleEt);
        descriptionET = findViewById(R.id.pDescriptionET);
        imageIv = findViewById(R.id.pImageIv);
        uploadBtn = findViewById(R.id.pUploadBtn);

        //get data through intent from previous activity
        Intent intent = getIntent();
        final String isUpdateKey = ""+intent.getStringExtra("key");
        final String editPostId = ""+intent.getStringExtra("editPostId");
        //validate if we came here to update the post, e.g came from adapter post
        if(isUpdateKey.equals("editPost")) {
            actionBar.setTitle("Update Post");
            uploadBtn.setText("Update");
            loadPostData(editPostId);
        } else {
            actionBar.setTitle("Add New Post");
            uploadBtn.setText("Upload");
        }

        actionBar.setSubtitle(email);

        userDbref = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbref.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    name = "" + ds.child("username").getValue();
                    email = "" + ds.child("email").getValue();
                    dp = "" + ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //get image from camera/gallery click listener
        imageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });

        //click listener for the upload button
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEt.getText().toString().trim();
                String description = descriptionET.getText().toString().trim();

                if (TextUtils.isEmpty(title)) {
                    Toast.makeText(AddPostActivity.this, "Enter Title..", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(description)) {
                    Toast.makeText(AddPostActivity.this, "Enter Description..", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isUpdateKey.equals("editPost")) {
                    beginUpdate(title,description,editPostId);
                } else {
                    uploadData(title,description);
                }

            }
        });
    }

    private void beginUpdate(String title, String description, String editPostId) {
    pd.setMessage("Updating Post");
    pd.show();

    if (!editImage.equals("noImage")) {
        updateWasWithImage(title,description,editPostId);
    } else if (imageIv.getDrawable() != null){
        updateWithNowImage(title,description,editPostId);
    } else {
        updateWithoutImage(title,description,editPostId);
    }

    }

    private void updateWithoutImage(String title, String description, String editPostId) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", uid);
        hashMap.put("username", name);
        hashMap.put("uEmail", email);
        hashMap.put("uDp", dp);
        hashMap.put("pTitle", title);
        hashMap.put("pDescr", description);
        hashMap.put("pImage", "noImage");
        hashMap.put("pLikes","0");
        hashMap.put("pComments","0");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.child(editPostId).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, "Updated Post", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWithNowImage(final String title, final String description, final String editPostId) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_"+timeStamp;

        Bitmap bitmap =((BitmapDrawable)imageIv.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());

                String downloadUri = uriTask.getResult().toString();
                if (uriTask.isSuccessful()) {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("uid", uid);
                    hashMap.put("username", name);
                    hashMap.put("uEmail", email);
                    hashMap.put("uDp", dp);
                    hashMap.put("pTitle", title);
                    hashMap.put("pDescr", description);
                    hashMap.put("pImage", downloadUri);
                    hashMap.put("pLikes","0");
                    hashMap.put("pComments","0");

                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                    ref.child(editPostId).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this, "Updated Post", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void updateWasWithImage(final String title, final String description, final String editPostId) {
        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                String timeStamp = String.valueOf(System.currentTimeMillis());
                String filePathAndName = "Posts/" + "post_"+timeStamp;

                Bitmap bitmap =((BitmapDrawable)imageIv.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //image compress
                bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
                byte[] data = baos.toByteArray();

                StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                ref.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());

                        String downloadUri = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()) {
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("uid", uid);
                            hashMap.put("username", name);
                            hashMap.put("uEmail", email);
                            hashMap.put("uDp", dp);
                            hashMap.put("pTitle", title);
                            hashMap.put("pDescr", description);
                            hashMap.put("pImage", downloadUri);
                            hashMap.put("pLikes","0");
                            hashMap.put("pComments","0");

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                            ref.child(editPostId).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            pd.dismiss();
                                            Toast.makeText(AddPostActivity.this, "Updated Post", Toast.LENGTH_SHORT).show();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                pd.dismiss();
                                    Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }


                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    private void loadPostData(String editPostId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        Query fquery = reference.orderByChild("pId").equalTo(editPostId);
        fquery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    //get data
                    editTitle =""+ds.child("pTitle").getValue();
                    editDescription=""+ds.child("pDescr").getValue();
                    editImage=""+ds.child("pImage").getValue();
                    //set data to views
                    titleEt.setText(editTitle);
                    descriptionET.setText(editDescription);
                    //set image
                    if(!editImage.equals("noImage")) {
                        try {
                            Picasso.get().load(editImage).into(imageIv);
                        }catch (Exception e) {

                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadData(final String title, final String description) {
        pd.setMessage("publishing post");
        pd.show();

        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathAndName = "posts/" + "post_" + timeStamp;

        if (imageIv.getDrawable() != null) {
            //get image from imageview
            Bitmap bitmap =((BitmapDrawable)imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
            byte[] data = baos.toByteArray();


            //post with image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());
                    String downloadUri = uriTask.getResult().toString();
                    //post with image
                    if (uriTask.isSuccessful()) {
                        HashMap<Object, String> hashMap = new HashMap<>();
                        hashMap.put("uid", uid);
                        hashMap.put("username", name);
                        hashMap.put("uEmail", email);
                        hashMap.put("uDp", dp);
                        hashMap.put("pId", timeStamp);
                        hashMap.put("pTitle", title);
                        hashMap.put("pDescr", description);
                        hashMap.put("pImage", downloadUri);
                        hashMap.put("pTime", timeStamp);
                        hashMap.put("pLikes","0");
                        hashMap.put("pComments","0");

                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                        ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                pd.dismiss();
                                Toast.makeText(AddPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
                                titleEt.setText("");
                                descriptionET.setText("");
                                imageIv.setImageURI(null);
                                image_uri = null;
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                pd.dismiss();
                                Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else { //post without image
            HashMap<Object, String> hashMap = new HashMap<>();
            hashMap.put("uid", uid);
            hashMap.put("username", name);
            hashMap.put("uEmail", email);
            hashMap.put("uDp", dp);
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescr", description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", timeStamp);
            hashMap.put("pLikes","0");
            hashMap.put("pComments","0");

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            ref.child(timeStamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
                    //reset views
                    titleEt.setText("");
                    descriptionET.setText("");
                    imageIv.setImageURI(null);
                    image_uri = null;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(AddPostActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void showImagePickDialog() {
        //options(camera,gallery) to show in dialog
        String[] options = {"Camera", "Gallery"};
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");
        //set options to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //item click handle
                if (which == 0) {
                    //camera clicked
                    if (!checkCameraPermission()) {
                        requestCameraPermission();
                    } else {
                        pickFromCamera();
                    }
                }
                if (which == 1) {
                    //gallery clicked
                    if (!checkStoragePermission()) {
                        requestStoragePermission();
                    } else {
                        pickFromGallery();
                    }
                }
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE, "temp pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temp Descr");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        //check if storage permission is enabled or not
        //return true if enabled
        // returns false if not enabled

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission() {
        //request runtime storage permission
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        //check if storage permission is enabled or not
        //return true if enabled
        // returns false if not enabled

        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        //request runtime storage permission
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            email = user.getEmail();
            uid = user.getUid();
        } else {
            startActivity(new Intent(this, Login.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // go to prev activity
        return super.onSupportNavigateUp();
    }


    //handles permissions results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length>0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        pickFromCamera();
                    } else {
                        Toast.makeText(this, "Both Camera & Storage permissions are necessary..", Toast.LENGTH_SHORT).show();
                    }
                } else {
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                if (grantResults.length>0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        pickFromGallery();
                    } else {
                        Toast.makeText(this, "Storage permissions are necessary..", Toast.LENGTH_SHORT).show();
                    }
                } else {

                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            image_uri = data.getData();
            imageIv.setImageURI(image_uri);
        } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
            imageIv.setImageURI(image_uri);
        }


        super.onActivityResult(requestCode, resultCode, data);
    }
}
