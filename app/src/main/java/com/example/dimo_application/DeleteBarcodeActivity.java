package com.example.dimo_application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class DeleteBarcodeActivity extends AppCompatActivity {
    private static final String TAG = "DeleteBarcodeActivity";
    TextInputLayout textInputLayoutBarcodeDel;

    TextInputEditText textInputEditTextBarcodeDel;

    Button dltBtn;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String userId;
    String shopName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_barcode);

        textInputLayoutBarcodeDel = findViewById(R.id.textInputBarcodeValueDel);
        textInputEditTextBarcodeDel = findViewById(R.id.editTextBarcodeValueDel);

        Intent intent = getIntent();
        shopName = intent.getStringExtra("shopName");

        dltBtn = findViewById(R.id.cirDeleteBarcodeBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        dltBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String barcode = Objects.requireNonNull(textInputEditTextBarcodeDel.getText()).toString();

                if(TextUtils.isEmpty(barcode)){
                    textInputEditTextBarcodeDel.setError("please enter barcode first");
                    return;
                }

                userId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

                firebaseFirestore.collection("guest").document(shopName).collection("barcode").document(barcode)
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.getResult().exists()){
                            firebaseFirestore.collection("guest").document(shopName).collection("barcode").document(barcode)
                                    .delete();
                        }
                    }
                });

                firebaseFirestore.collection("shops").document(userId).collection("barcode").document(barcode)
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(Objects.requireNonNull(task.getResult()).exists()){
                            firebaseFirestore.collection("shops").document(userId).collection("barcode").document(barcode)
                                    .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getApplicationContext(),"barcode deleted Successfully",Toast.LENGTH_SHORT).show();
                                    textInputEditTextBarcodeDel.setText("");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(),"Failure "+e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "No Such Barcode", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

            }
        });


    }
}