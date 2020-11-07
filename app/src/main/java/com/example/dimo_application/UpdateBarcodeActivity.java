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

import java.util.HashMap;
import java.util.Map;

public class UpdateBarcodeActivity extends AppCompatActivity {
    private static final String TAG = "UpdateBarcodeActivity";
    TextInputLayout textInputLayoutBarcode;
    TextInputLayout textInputLayoutProduct;
    TextInputLayout textInputLayoutPrice;
    TextInputLayout textInputLayoutExp;
    TextInputLayout textInputLayoutOffer;

    TextInputEditText editTextBarcode;
    TextInputEditText editTextProduct;
    TextInputEditText editTextPrice;
    TextInputEditText editTextExp;
    TextInputEditText editTextOffer;

    Button UpdateBtn;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    String userId;
    String shopName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_barcode);

        Intent intent = getIntent();
        shopName = intent.getStringExtra("shopName");

        textInputLayoutBarcode = findViewById(R.id.textInputBarcodeUpValue);
        textInputLayoutProduct = findViewById(R.id.textInputUpProductName);
        textInputLayoutPrice = findViewById(R.id.textInputUpPrice);
        textInputLayoutExp =findViewById(R.id.textInputUpExpDate);
        textInputLayoutOffer = findViewById(R.id.textInputUpOffers);

        editTextBarcode = findViewById(R.id.editTextBarcodeUpValue);
        editTextProduct = findViewById(R.id.editTextUpProductName);
        editTextPrice = findViewById(R.id.editTextUpPrice);
        editTextExp= findViewById(R.id.editTextUpExpDate);
        editTextOffer = findViewById(R.id.editTextUpOffers);

        UpdateBtn = findViewById(R.id.cirUpdateBarcodeBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        UpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String barcode = editTextBarcode.getText().toString();
                final String product = editTextProduct.getText().toString();
                final String price = editTextPrice.getText().toString();
                final String Exp = editTextExp.getText().toString();
                final String offer = editTextOffer.getText().toString();

                if(TextUtils.isEmpty(barcode)){
                    editTextBarcode.setError("please enter barcode");
                    return;
                }

                if(TextUtils.isEmpty(product)){
                    editTextProduct.setError("please enter product");
                    return;
                }

                if(TextUtils.isEmpty(price)){
                    editTextPrice.setError("please enter price");
                    return;
                }

                if(TextUtils.isEmpty(Exp)){
                    editTextExp.setError("please enter Exp");
                    return;
                }

                if(TextUtils.isEmpty(offer)){
                    editTextOffer.setError("please enter offer");
                    return;
                }

                userId = firebaseAuth.getCurrentUser().getUid();
//                final String[] shopName = new String[1];
//
//                firebaseFirestore.collection("shops").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//                    @Override
//                    public void onSuccess(DocumentSnapshot documentSnapshot) {
//                        if(documentSnapshot.exists()){
//                            shopName[0] = documentSnapshot.getString("ShopName");
//                        }
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//
//                    }
//                });

                firebaseFirestore.collection("shops").document(userId).collection("barcode").document(barcode)
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.getResult().exists()){
                            final Map<String,Object> UpdateBarcode = new HashMap<>();
                            UpdateBarcode.put("barcode",barcode);
                            UpdateBarcode.put("product",product);
                            UpdateBarcode.put("price",price);
                            UpdateBarcode.put("Exp",Exp);
                            UpdateBarcode.put("offer",offer);

                            firebaseFirestore.collection("guest").document(shopName).collection("barcode").document(barcode)
                                    .set(UpdateBarcode).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "guest barcode adding: " + e.getMessage());
                                }
                            });


                            firebaseFirestore.collection("shops").document(userId).collection("barcode").document(barcode)
                                    .update(UpdateBarcode).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getApplicationContext(),"Barcode is Updated Successfully",Toast.LENGTH_SHORT).show();
                                    editTextBarcode.setText("");
                                    editTextProduct.setText("");
                                    editTextPrice.setText("");
                                    editTextExp.setText("");
                                    editTextOffer.setText("");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(),"Failure "+e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else {
                            editTextBarcode.setError("Barcode is not assigned");
                        }
                    }
                });
            }
        });

    }
}