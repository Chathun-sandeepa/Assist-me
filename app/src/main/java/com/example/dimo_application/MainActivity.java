package com.example.dimo_application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.dnn.Dnn;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;
import org.opencv.utils.Converters;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.RECORD_AUDIO","android.permission.INTERNET"};
    private static final String TAG = "MainActivity";
    final float pitch = 1.0f;
    final float speedRate = 1.0f;

    private TextToSpeech mtextToSpeech;

    GestureDetectorCompat gestureDetector;

    SpeechRecognizer speechRecognizer;

    Intent speechRecognizerIntent;

    ToneGenerator toneGenerator;

    TextView recogTextView;

    Button startCameraBtn;
    TextView loginBtn;
    TextView signUpBtn;

    String shopName;

    FirebaseFirestore firebaseFirestore;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startCameraBtn = findViewById(R.id.cirCameraStartButton);
        startCameraBtn.setEnabled(false);

        loginBtn = findViewById(R.id.loginTextViewBtn);
        loginBtn.setEnabled(false);

        signUpBtn = findViewById(R.id.registerTextViewBtn);
        signUpBtn.setEnabled(false);

        firebaseFirestore = FirebaseFirestore.getInstance();

        mtextToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS){
                    int result = mtextToSpeech.setLanguage(Locale.ENGLISH);
                    if(result==TextToSpeech.LANG_MISSING_DATA|| result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e(TAG, "Text to Speech: Language not supported" );
                    }
                    else{
                        mtextToSpeech.setPitch(pitch);
                        mtextToSpeech.setSpeechRate(speedRate);
                        mtextToSpeech.speak("Welcome, Please say shop name by touching the screen",TextToSpeech.QUEUE_FLUSH,null);
                    }
                }
                else {
                    Log.e(TAG, "Text to speech: initializatiion failed" );
                }
            }
        });

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC,100);

        gestureDetector = new GestureDetectorCompat(this,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP,300);
                speechRecognizer.startListening(speechRecognizerIntent);
            }

        });

        recogTextView = findViewById(R.id.recogTextView);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onReadyForSpeech(Bundle params) {
                recogTextView.setText("Listening..");
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onEndOfSpeech() {
                recogTextView.setText("listening finished");
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onError(int error) {
                if(error == SpeechRecognizer.ERROR_NO_MATCH){
                    recogTextView.setText("No match found");
                }
                else if(error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
                    recogTextView.setText("Speech timed out");
                }
                else {
                    recogTextView.setText("Error");
                }
                mtextToSpeech.speak("I didn't here, please try again",TextToSpeech.QUEUE_FLUSH,null);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if(matches != null){
                    String recogText = matches.get(0);
                    recogTextView.setText(recogText);
                    shopName = recogText.toLowerCase().replaceAll("\\s","");
                    Toast.makeText(getApplicationContext(),shopName,Toast.LENGTH_SHORT).show();

                    mtextToSpeech.speak("connecting to shop",TextToSpeech.QUEUE_ADD,null);

                    firebaseFirestore.collection("guest").document(shopName).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.getResult().exists()){
                                firebaseFirestore.collection("guest").document(shopName).collection("barcode")
                                        .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        ArrayList<String> keys = new ArrayList<>();
                                        ArrayList<String> details = new ArrayList<>();

                                        for(QueryDocumentSnapshot documentSnapshot:queryDocumentSnapshots){
                                            String barcode = documentSnapshot.getString("barcode");
                                            String product = documentSnapshot.getString("product");
                                            String price = documentSnapshot.getString("price");
                                            String Exp = documentSnapshot.getString("Exp");
                                            String offer = documentSnapshot.getString("offer");

                                            String detail = "this product is " + product + "and its cost is" + price + "It will expire on" + Exp + "and it has" + offer;
                                            keys.add(barcode);
                                            details.add(detail);
                                        }

                                        mtextToSpeech.speak("Starting camera..", TextToSpeech.QUEUE_ADD, null);
                                        Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                                        cameraIntent.putExtra("keys",keys);
                                        cameraIntent.putExtra("details",details);
                                        startActivity(cameraIntent);
                                        recogTextView.setText("");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Retreive data from guest: "+ e.getMessage() );
                                    }
                                });

                            }
                            else {
                                mtextToSpeech.speak("Such shop couldn't find, please try again",TextToSpeech.QUEUE_ADD,null);
                            }
                        }
                    });
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                recogTextView.setText("onEvent");
            }
        });


        if(allPermissionsGranted()){
            startCameraBtn.setEnabled(true);
            loginBtn.setEnabled(true);
            signUpBtn.setEnabled(true);
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        startCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MainActivity.this,CameraActivity.class);
                startActivity(cameraIntent);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"pressed",Toast.LENGTH_SHORT).show();
                Intent signUpIntent = new Intent(getApplicationContext(),RegisterActivity.class);
                startActivity(signUpIntent);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraBtn.setEnabled(true);
                loginBtn.setEnabled(true);
                signUpBtn.setEnabled(true);
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean allPermissionsGranted() {

        for (String permission: REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        if(mtextToSpeech != null){
            mtextToSpeech.stop();
            mtextToSpeech.shutdown();
        }
        super.onDestroy();

    }
}