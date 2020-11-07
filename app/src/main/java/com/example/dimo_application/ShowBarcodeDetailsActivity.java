package com.example.dimo_application;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShowBarcodeDetailsActivity extends AppCompatActivity {

    TextInputLayout textInputLayout;
    TextInputEditText textInputEditText;
    Button showBtn;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String userId;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_barcode_details);

        textInputLayout = findViewById(R.id.textInputBarcodeValueShow);
        textInputEditText = findViewById(R.id.editTextBarcodeValueShow);

        textView = findViewById(R.id.resultView);

        showBtn = findViewById(R.id.cirShowBarcodeBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        showBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String barcode = textInputEditText.getText().toString();

                if(TextUtils.isEmpty(barcode)){
                    textInputEditText.setError("please enter barcode first");
                    return;
                }
                userId = firebaseAuth.getCurrentUser().getUid();

                firebaseFirestore.collection("shops").document(userId).collection("barcode").document(barcode)
                        .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
                            String barcode = documentSnapshot.getString("barcode");
                            String product = documentSnapshot.getString("product");
                            String price = documentSnapshot.getString("price");
                            String Exp = documentSnapshot.getString("Exp");
                            String offer = documentSnapshot.getString("offer");

                            textView.setText("Barcode: "+barcode+"\nProduct: "+product+"\nPrice: "+price + "\nExp: " + Exp + "\nOffers: " + offer);
                            textInputEditText.setText("");

                        }
                        else {
                            Toast.makeText(getApplicationContext(),"Barcode does not exist",Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),"Failure "+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }
}