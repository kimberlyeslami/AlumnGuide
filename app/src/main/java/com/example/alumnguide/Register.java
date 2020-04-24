package com.example.alumnguide;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
   // private static final String TAG = "" ;
    Button btn_Register;
    EditText txt_Username, txt_Email, txt_Password, txt_confPass, txt_CourseStudying;

    FirebaseAuth firebaseAuth;
    DatabaseReference reference;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        txt_Username = findViewById(R.id.txt_Username);
        txt_Email = findViewById(R.id.txt_Email);
        txt_Password = findViewById(R.id.txt_Password);
        txt_confPass = findViewById(R.id.txt_confPass);
        txt_CourseStudying = findViewById(R.id.txt_CourseStudying);

        btn_Register = findViewById(R.id.btn_Register);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        final Spinner spn_CurrentYear = findViewById(R.id.spn_CurrentYear);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.years_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_CurrentYear.setAdapter(adapter);
        spn_CurrentYear.setOnItemSelectedListener(this);

        btn_Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = txt_Username.getText().toString().trim();
                String email = txt_Email.getText().toString().trim();
                String password = txt_Password.getText().toString().trim();
                String confirmPass = txt_confPass.getText().toString().trim();
                String currentYear = spn_CurrentYear.getSelectedItem().toString().trim();
                String courseStudying = txt_CourseStudying.getText().toString().trim();

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    txt_Email.setError("Invalid Email");
                    txt_Email.setFocusable(true);
                } else if (password.length() < 6) {
                    txt_Password.setError("Password Length needs to be at least 6 characters");
                    txt_Password.setFocusable(true);
                }else if (confirmPass.length() < 6) {
                    txt_confPass.setError("Password Length needs to be at least 6 characters");
                    txt_confPass.setFocusable(true);
                } else if (!confirmPass.equals(password)) {
                    txt_confPass.setError("The passwords do not match.");
                    txt_confPass.setFocusable(true);
                }else {
                    registerUser(username, email, password, confirmPass, currentYear, courseStudying);
                }
            }
        });
    }

    private void registerUser(final String username, final String email, final String password, final String confirmPass, final String currentYear, final String courseStudying) {
       try {
           firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
               @Override
               public void onComplete(@NonNull Task<AuthResult> task) {
                   if (task.isSuccessful()) {
                       FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                       assert firebaseUser != null;
                       String userID = firebaseUser.getUid();

                       Toast.makeText(Register.this, "Registered...\n" + firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                       startActivity(new Intent(Register.this, Login.class));
                       finish();
                       reference = FirebaseDatabase.getInstance().getReference("Users").child(userID);

                       Map<String, Object> users = new HashMap<>();
                       users.put("id", userID);
                       users.put("onlineStatus", "online");
                       users.put("typingTo", "noOne");
                       users.put("username", username);
                       users.put("email", email);
                       users.put("password", password);
                       users.put("confirmPassword", confirmPass);
                       users.put("currentYear", currentYear);
                       users.put("courseStudying", courseStudying);
                       users.put("imageURI", "noImage");

                       reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                           @Override
                           public void onComplete(@NonNull Task<Void> task) {
                               if (task.isSuccessful()) {
                                   Intent intent = new Intent(Register.this, Login.class);
                                   intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                   startActivity(intent);
                                   finish();
                               }
                           }
                       });
                   } else {
                       Toast.makeText(Register.this, "SignUp Unsuccessful. please try again.", Toast.LENGTH_SHORT).show();
                   }
               }
           });
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String text = parent.getItemAtPosition(position).toString();
        Toast.makeText(parent.getContext(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}

