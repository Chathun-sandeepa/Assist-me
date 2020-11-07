package com.example.dimo_application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    TextInputLayout textInputLayoutName;
    TextInputLayout textInputLayoutEmail;
    TextInputLayout textInputLayoutPassword;
    TextInputLayout textInputLayoutShopName;
    TextInputLayout textInputLayoutPhone;

    TextInputEditText editTextName;
    TextInputEditText editTextEmail;
    TextInputEditText editTextPassword;
    TextInputEditText editTextShopName;
    TextInputEditText editTextPhone;
    Button registerBtn;
    ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        textInputLayoutName = findViewById(R.id.textInputName);
        textInputLayoutEmail = findViewById(R.id.textInputEmail);
        textInputLayoutPassword = findViewById(R.id.textInputPassword);
        textInputLayoutShopName = findViewById(R.id.textIputShopName);
        textInputLayoutPhone = findViewById(R.id.textInputMobile);


        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmailRegister);
        editTextPassword = findViewById(R.id.editTextPasswordRegister);
        editTextShopName = findViewById(R.id.editTextShopName);
        editTextPhone = findViewById(R.id.editTextMobile);
        registerBtn = (Button) findViewById(R.id.cirRegisterButton);
        progressBar = findViewById(R.id.progressBar2);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String Name = editTextName.getText().toString();
                final String email = editTextEmail.getText().toString();
                final String password = editTextPassword.getText().toString();
                final String shopName = editTextShopName.getText().toString();
                final String phone = editTextPhone.getText().toString();

                if(TextUtils.isEmpty(Name)){
                    editTextName.setError("please enter name");
                    return;
                }

                if(TextUtils.isEmpty(email)){
                    editTextEmail.setError("please enter email");
                    return;
                }

                if(TextUtils.isEmpty(password)){
                    editTextPassword.setError("please enter password");
                    return;
                }

                if(password.length()<8){
                    editTextPassword.setError("Password must have 8 characters");
                    return;
                }

                if(TextUtils.isEmpty(shopName)){
                    editTextShopName.setError("please enter shop name");
                    return;
                }

                if(shopName.contains(" ")){
                    editTextShopName.setError("Spaces are not Allowed");
                    return;
                }

                if(!shopName.equals(shopName.toLowerCase())){
                    editTextShopName.setError("Only lowercase letters are allowed");
                    return;
                }

                if(!shopName.matches("^[a-zA-Z0-9]+$")){
                    editTextShopName.setError("Only alphanumeric characters are allowed");
                    return;
                }

                if(TextUtils.isEmpty(phone)){
                    editTextPhone.setError("please enter phone no");
                    return;
                }


                firestore.collection("guest").document(shopName).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.getResult().exists()){
                            editTextShopName.setError("Shop name is already assigned");
                        }
                        else {
                            progressBar.setVisibility(View.VISIBLE);

                            firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()){
                                        userId = firebaseAuth.getCurrentUser().getUid();
                                        DocumentReference guestRef = firestore.collection("guest").document(shopName);
                                        Map<String,Object> guest = new HashMap<>();
                                        guest.put("ShopName",shopName);
                                        guest.put("Name",Name);
                                        guest.put("Email",email);
                                        guest.put("Tel",phone);

                                        guestRef.set(guest).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getApplicationContext(),"ShopId added Successfully",Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(getApplicationContext(),"Failure "+e.getMessage(),Toast.LENGTH_SHORT).show();
                                                Log.e(TAG, "guest Collection" + e.getMessage() );
                                            }
                                        });


                                        DocumentReference shopsRef = firestore.collection("shops").document(userId);
                                        Map<String,Object> shop = new HashMap<>();
                                        shop.put("ShopName",shopName);
                                        shop.put("Name",Name);
                                        shop.put("Email",email);
                                        shop.put("Tel",phone);

                                        shopsRef.set(shop).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(getApplicationContext(),"Shop Registered Successfully",Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(getApplicationContext(),"Failure "+e.getMessage(),Toast.LENGTH_SHORT).show();
                                                Log.e(TAG, "shop Collection: " + e.getMessage() );
                                            }
                                        });

                                        Toast.makeText(getApplicationContext(),"Shop Registered",Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this,AccountActivity.class));

                                        progressBar.setVisibility(View.GONE);
                                        editTextName.setText("");
                                        editTextEmail.setText("");
                                        editTextPassword.setText("");
                                        editTextShopName.setText("");
                                        editTextPhone.setText("");
                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(),"Error!: "+ task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            });
                        }
                    }
                });

            }
        });

    }
}