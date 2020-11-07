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
import androidx.core.view.GestureDetectorCompat;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    final float pitch = 1.0f;
    final float speedRate = 1.0f;
    volatile boolean textMode = true;
    volatile boolean barcodeMode = false;
    volatile boolean stopDetections = false;

    TextureView textureView;
    ImageView ivBitmap;

    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    Preview preview;

    GraphicOverlay graphicOverlay;

    private TextToSpeech textToSpeech;

    TextRecognizer textRecognizer;

    TextView textViewBarcode;

    GestureDetectorCompat mGestureDetector;

    private ArrayList<String> barcodes;
    private ArrayList<String> details;
    private Map<String,String> barcodeDetails = new HashMap<>();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


        barcodes = (ArrayList<String>) getIntent().getSerializableExtra("keys");
        details = (ArrayList<String>) getIntent().getSerializableExtra("details");

        for(int i=0;i<barcodes.size();i++){
            barcodeDetails.put(barcodes.get(i), details.get(i));
        }

        textureView = findViewById(R.id.textureView);
        ivBitmap = findViewById(R.id.ivBitmap);

        graphicOverlay = findViewById(R.id.graphicOverlay);

        textViewBarcode = findViewById(R.id.textviewBarcode);

        mGestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if(!stopDetections){
                        stopDetections = true;
                        graphicOverlay.clear();
                        Toast.makeText(getApplicationContext(),"OnFling: Detections stopped",Toast.LENGTH_SHORT).show();
                        textToSpeech.stop();
                        textToSpeech.speak("Both detection modes are disabled",TextToSpeech.QUEUE_FLUSH,null);
                    }
                    else {
                        stopDetections = false;
                        graphicOverlay.clear();
                        textToSpeech.stop();
                        if(textMode && !barcodeMode){
                            Toast.makeText(getApplicationContext(),"OnFling: Text mode continued",Toast.LENGTH_SHORT).show();
                            textToSpeech.speak("Text detection mode is continued",TextToSpeech.QUEUE_FLUSH,null);
                        }
                        else if(!textMode && barcodeMode){
                            graphicOverlay.clear();
                            textToSpeech.speak("Barcode detection mode is continued",TextToSpeech.QUEUE_FLUSH,null);
                            Toast.makeText(getApplicationContext(),"OnFling: Barcode mode continued",Toast.LENGTH_SHORT).show();
                        }

                    }

                return super.onFling(e1, e2, velocityX, velocityY);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                    if(!stopDetections){
                        if(textMode && !barcodeMode){
                            textMode = false;
                            barcodeMode = true;
                            graphicOverlay.clear();
                            Toast.makeText(getApplicationContext(),"DoubleTap: Barcode mode",Toast.LENGTH_SHORT).show();
                            textToSpeech.stop();
                            textToSpeech.speak("Barcode detection mode is enabled",TextToSpeech.QUEUE_FLUSH,null);
                        }
                        else if(!textMode && barcodeMode){
                            textMode = true;
                            barcodeMode = false;
                            graphicOverlay.clear();
                            Toast.makeText(getApplicationContext(),"DoubleTap: Text mode",Toast.LENGTH_SHORT).show();
                            textToSpeech.stop();
                            textToSpeech.speak("Text detection mode is enabled",TextToSpeech.QUEUE_FLUSH,null);
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"DoubleTap: Enable detections first",Toast.LENGTH_SHORT).show();
                        textToSpeech.speak("Enable detections first",TextToSpeech.QUEUE_FLUSH,null);
                    }
                return super.onDoubleTap(e);
            }
        });

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS){
                    int result = textToSpeech.setLanguage(Locale.ENGLISH);
                    if(result==TextToSpeech.LANG_MISSING_DATA|| result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e(TAG, "Text to Speech: Language not supported" );
                    }
                    else{
                        textToSpeech.setPitch(pitch);
                        textToSpeech.setSpeechRate(speedRate);
                        textToSpeech.speak("Text detection mode is enabled but you can interchange modes by double tapping on screen",TextToSpeech.QUEUE_FLUSH,null);
                        textToSpeech.speak("Also if you want to pause the detection engine swipe the screen" +
                                "Thank you",TextToSpeech.QUEUE_ADD,null);
                    }
                }
                else {
                    Log.e(TAG, "Text to speech: initializatiion failed" );
                }
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startCamera();
            }
        },1000);

    }

    private void startCamera() {

        CameraX.unbindAll();
        preview = setPreview();
        imageCapture = setImageCapture();
        imageAnalysis = setImageAnalysis();

        //bind to lifecycle:
        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis);
    }

    private Preview setPreview() {

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });

        return preview;
    }


    private ImageCapture setImageCapture() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCapture = new ImageCapture(imageCaptureConfig);


//        btnCapture.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//
//                imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
//                    @Override
//                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
//                        Bitmap bitmap = textureView.getBitmap();
//                        showAcceptedRejectedButton(true);
//                        ivBitmap.setImageBitmap(bitmap);
//                    }
//
//                    @Override
//                    public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
//                        super.onError(useCaseError, message, cause);
//                    }
//                });
//
//
//                /*File file = new File(
//                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "" + System.currentTimeMillis() + "_JDCameraX.jpg");
//                imgCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
//                    @Override
//                    public void onImageSaved(@NonNull File file) {
//                        Bitmap bitmap = textureView.getBitmap();
//                        showAcceptedRejectedButton(true);
//                        ivBitmap.setImageBitmap(bitmap);
//                    }
//
//                    @Override
//                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
//
//                    }
//                });*/
//            }
//        });

        return imgCapture;
    }


    private ImageAnalysis setImageAnalysis() {

        // Setup image analysis pipeline that computes average pixel luminance
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();


        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setCallbackHandler(new Handler(analyzerThread.getLooper()))
                .setImageQueueDepth(1).build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        //Analyzing live camera feed begins.

                        final Bitmap bitmap = textureView.getBitmap();

                        if(bitmap==null)
                            return;

                        if(!stopDetections){
                            if(textMode && !barcodeMode){
                                TextRecognition(bitmap);
                            }
                            else if(!textMode && barcodeMode){
                                InputImage Curr_image = InputImage.fromBitmap(bitmap, rotationDegrees);
                                scanBarcodes(Curr_image,bitmap);
                            }
                        }
                        else {
                            graphicOverlay.clear();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ivBitmap.setImageBitmap(bitmap);
                            }
                        });

                    }
                });


        return imageAnalysis;

    }


    private void scanBarcodes(InputImage image, final Bitmap bitmap) {
        // [START set_detector_options]
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_ALL_FORMATS)
                        .build();
        // [END set_detector_options]

        graphicOverlay.clear();

        // [START get_detector]
        BarcodeScanner scanner = BarcodeScanning.getClient();
        // Or, to specify the formats to recognize:
        // BarcodeScanner scanner = BarcodeScanning.getClient(options);
        // [END get_detector]

        // [START run_detector]
        Task<List<Barcode>> result = scanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        // Task completed successfully
                        // [START_EXCLUDE]
                        // [START get_barcodes]

                        if(barcodes.isEmpty()){
                            if(!textToSpeech.isSpeaking()){
                                textToSpeech.speak("No barcode detected please try again", TextToSpeech.QUEUE_FLUSH,null);
                            }
                        }
                        else {
                            for (Barcode barcode: barcodes) {
                                final String rawValue = barcode.getRawValue();

                                if(rawValue != null){
                                    stopDetections = true;
                                    textToSpeech.stop();
                                    if(textToSpeech.isSpeaking()){
                                        textToSpeech.speak("Barcode is detected \n " +
                                                "Getting details..",TextToSpeech.QUEUE_FLUSH,null);
                                        Toast.makeText(getApplicationContext(),rawValue,Toast.LENGTH_SHORT).show();
                                        textViewBarcode.setText(rawValue);
                                    }

                                    String currDetail = barcodeDetails.get(rawValue);

                                    if(textToSpeech.isSpeaking()){
                                        textToSpeech.stop();
                                    }
                                    textToSpeech.speak(currDetail,TextToSpeech.QUEUE_ADD,null );
                                    textToSpeech.speak("To detect barcode again swipe the screen",TextToSpeech.QUEUE_ADD,null );
                                }
                            }
                            // [END get_barcodes]
                            // [END_EXCLUDE]
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        Log.e(TAG, "onFailure: barcode detection -> "+ e.toString() );
                        Toast.makeText(getApplicationContext(),"barcode failed"+ e.toString(),Toast.LENGTH_SHORT).show();
                    }
                });
        textViewBarcode.setText("");
        // [END run_detector]
    }

    private void TextRecognition(final Bitmap bitmap){
        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Text Recognition: Detector dependencies are not yet available");
        }
        else {

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> items = textRecognizer.detect(frame);

            if(items.size()==0){
                graphicOverlay.clear();
            }
            else {
                graphicOverlay.clear();

                StringBuilder stringBuilder = new StringBuilder();
                for (int i=0;i<items.size();++i){
                    TextBlock item = items.valueAt(i);
                    stringBuilder.append(item.getValue());
                    stringBuilder.append("\n");

                    Rect rect = item.getBoundingBox();

                    RectOverlay rectOverlay = new RectOverlay(graphicOverlay,rect);
                    graphicOverlay.add(rectOverlay);
                }

                String text = stringBuilder.toString();
                if(!textToSpeech.isSpeaking()){
                    textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
                }
            }

        }

    }


    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    @Override
    protected void onPause() {
        if(textToSpeech != null){
            textToSpeech.stop();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}