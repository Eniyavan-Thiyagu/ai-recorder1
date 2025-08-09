package com.example.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private val sampleRate = 16000
    private var isRecording = false
    private var recordJob: Job? = null
    private lateinit var audioEngine: AudioEngine
    private lateinit var visualizerView: VisualizerView
    private lateinit var outFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioEngine = AudioEngine(sampleRate)
        visualizerView = findViewById(R.id.visualizer)

        val btnRecord = findViewById<Button>(R.id.btnRecord)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val btnPlay = findViewById<Button>(R.id.btnPlay)
        val spinner = findViewById<Spinner>(R.id.spinnerEffects)
        val btnShare = findViewById<Button>(R.id.btnShare)

        val effects = listOf("Normal", "Chipmunk", "Deep", "Robot", "Denoise")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, effects)

        btnRecord.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopRecording() }
        btnPlay.setOnClickListener { val effect = spinner.selectedItem.toString(); playEffect(effect) }
        btnShare.setOnClickListener { shareRecording() }

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECORD_AUDIO)
        if (perms.isNotEmpty()) ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)
    }

    private fun startRecording() {
        if (isRecording) return
        isRecording = true
        outFile = File(getExternalFilesDir(null), "recording_denoised.wav")
        recordJob = CoroutineScope(Dispatchers.IO).launch {
            audioEngine.startRecording { buffer ->
                visualizerView.post { visualizerView.updateWaveform(buffer) }
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        audioEngine.stopAndSave(outFile)
        recordJob?.cancel()
    }

    private fun playEffect(effect: String) {
        if (!::outFile.isInitialized || !outFile.exists()) { runOnUiThread { Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show() }; return }
        CoroutineScope(Dispatchers.IO).launch {
            when (effect) {
                "Chipmunk" -> audioEngine.playWithPitch(outFile, 1.6f)
                "Deep" -> audioEngine.playWithPitch(outFile, 0.75f)
                "Robot" -> audioEngine.playRobot(outFile)
                "Denoise" -> audioEngine.play(outFile)
                else -> audioEngine.play(outFile)
            }
        }
    }

    private fun shareRecording() {
        if (!::outFile.isInitialized || !outFile.exists()) { Toast.makeText(this, "No recording to share", Toast.LENGTH_SHORT).show(); return }
        val uri = androidx.core.content.FileProvider.getUriForFile(this, packageName + ".fileprovider", outFile)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "audio/wav"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(share, "Share recording"))
    }
}
