package com.example.dimo_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class AccountActivity extends AppCompatActivity {

    Button logoutBtn;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private String userId;

    TextView textViewShopName;
    Button addBtn,getBtn,updateBtn,deleteBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        textViewShopName = findViewById(R.id.shopNameTextView);
        addBtn = findViewById(R.id.addBtn);
        getBtn = findViewById(R.id.getBtn);
        updateBtn = findViewById(R.id.updateBtn);
        deleteBtn = findViewById(R.id.deleteBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        userId = firebaseAuth.getCurrentUser().getUid();

        DocumentReference documentReference = firebaseFirestore.collection("shops").document(userId);
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                textViewShopName.setText(value.getString("ShopName"));
            }
        });

        final String[] shopName = new String[1];

        firebaseFirestore.collection("shops").document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    shopName[0] = documentSnapshot.getString("ShopName");
                    Toast.makeText(AccountActivity.this, shopName[0], Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(AccountActivity.this, "Shop is not defined", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(AccountActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addBarcodeActivity = new Intent(getApplicationContext(),AddBarcodeActivity.class);
                addBarcodeActivity.putExtra("shopName",shopName[0]);
                startActivity(addBarcodeActivity);
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent updateBarcodeActivity = new Intent(getApplicationContext(),UpdateBarcodeActivity.class);
                updateBarcodeActivity.putExtra("shopName",shopName[0]);
                startActivity(updateBarcodeActivity);
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent deleteBarcodeActivity = new Intent(getApplicationContext(),DeleteBarcodeActivity.class);
                deleteBarcodeActivity.putExtra("shopName",shopName[0]);
                startActivity(deleteBarcodeActivity);

            }
        });

        getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getBarcodeDetailsActivity = new Intent(getApplicationContext(),ShowBarcodeDetailsActivity.class);
                getBarcodeDetailsActivity.putExtra("shopName",shopName[0]);
                startActivity(getBarcodeDetailsActivity);
            }
        });



        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                finish();
            }
        });
    }
}