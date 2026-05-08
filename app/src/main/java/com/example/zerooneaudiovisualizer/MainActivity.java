package com.example.zerooneaudiovisualizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chibde.visualizer.LineBarVisualizer;

public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    private LineBarVisualizer lineBarVisualizer;
    private boolean isRecording = false;
    private Thread recordingThread;

    // checks for permissions
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startVisualizer();
                } else {
                    Toast.makeText(this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        lineBarVisualizer = findViewById(R.id.visualizer);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVisualizer();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startVisualizer() {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
        );

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            int audioSessionId = audioRecord.getAudioSessionId();
            
            lineBarVisualizer.setColor(Color.parseColor("#F7AC36"));
            lineBarVisualizer.setDensity(70);
            
            audioRecord.startRecording();
            isRecording = true;

            recordingThread = new Thread(() -> {
                byte[] data = new byte[bufferSize];
                while (isRecording) {
                    audioRecord.read(data, 0, bufferSize);
                }
            });
            recordingThread.start();

            try {
                lineBarVisualizer.setPlayer(audioSessionId);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Visualizer initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Failed to initialize AudioRecord", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRecording = false;
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordingThread = null;
        }
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
        if (lineBarVisualizer != null) {
            lineBarVisualizer.release();
        }
    }
}