package com.example.dimo_application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    TextInputLayout mTextInputLayoutEmail;
    TextInputLayout mTextInputLayoutPassword;

    TextInputEditText mEditTextEmail;
    TextInputEditText mEditTextPassword;

    Button loginBtn;
    ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mTextInputLayoutEmail = findViewById(R.id.textInputEmailLogin);
        mTextInputLayoutPassword = findViewById(R.id.textInputPasswordLogin);

        mEditTextEmail = findViewById(R.id.editTextEmailLogin);
        mEditTextPassword = findViewById(R.id.editTextPasswordLogin);

        loginBtn = findViewById(R.id.cirLoginButton);
        progressBar = findViewById(R.id.progressBar1);

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() != null){
            startActivity(new Intent(LoginActivity.this,AccountActivity.class));
        }

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEditTextEmail.getText().toString().trim();
                String password = mEditTextPassword.getText().toString().trim();

                if(TextUtils.isEmpty(email)){
                    mEditTextEmail.setError("please enter email");
                    return;
                }

                if(TextUtils.isEmpty(password)){
                    mEditTextPassword.setError("please enter password");
                    return;
                }

                if(password.length()<8){
                    mEditTextPassword.setError("Password must have 8 characters");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);

                firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(getApplicationContext(),"Logged in Successfully",Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this,AccountActivity.class));
                            progressBar.setVisibility(View.GONE);
                            mEditTextEmail.setText("");
                            mEditTextPassword.setText("");
                        }
                        else {
                            Toast.makeText(getApplicationContext(),"Error!: "+ task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });

            }
        });




    }
}