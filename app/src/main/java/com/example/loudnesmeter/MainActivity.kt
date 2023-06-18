package com.example.loudnesmeter

import ThresholdDialogFragment
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import androidx.core.content.ContextCompat
import com.example.loudnesmeter.databinding.ActivityMainBinding
import kotlin.math.log10

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 123
    private val SAMPLE_RATE = 44100
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    private var isRecording = false
    private var currentDb = 0.0 // Variable to store the current dB value
    private lateinit var vibrator: Vibrator // Vibrator instance
    private var warningThreshold = 70 // Default warning threshold

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Request microphone permission
        requestPermission()

        // Start/Stop recording button click listener
        binding.buttonStartStop.setOnClickListener {
            if (isRecording) {
                stopRecording()
                binding.buttonStartStop.text = "Start"
            } else {
                if (checkPermission()) {
                    startRecording()
                    binding.buttonStartStop.text = "Stop"
                }
            }
        }
        binding.buttonConfigureThreshold.setOnClickListener {
            showThresholdDialog()
        }
    }


    private fun requestPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        val granted = PackageManager.PERMISSION_GRANTED

        if (ContextCompat.checkSelfPermission(this, permission) != granted) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkPermission(): Boolean {
        val permission = Manifest.permission.RECORD_AUDIO
        val granted = PackageManager.PERMISSION_GRANTED

        return ContextCompat.checkSelfPermission(this, permission) == granted
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, start recording
            startRecording()
            binding.buttonStartStop.text = "Stop"
        }
    }

    private fun startRecording() {
        try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE
            )

            val buffer = ShortArray(BUFFER_SIZE)

            audioRecord.startRecording()
            isRecording = true

            Thread {
                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, BUFFER_SIZE)
                    if (read > 0) {
                        val amplitude = buffer.maxOrNull()?.toDouble() ?: 0.0
                        val db = 20 * log10(amplitude / Short.MAX_VALUE)
                        runOnUiThread {
                            currentDb = db // Update the current dB value
                            binding.textViewDb.text = String.format("%.1f dB", currentDb)

                            // Check if loudness exceeds 70dB
                            if (currentDb > 70) {
                                binding.textViewDb.setTextColor(ContextCompat.getColor(this, R.color.warningColor))
                                val toast = Toast.makeText(this, "Warning: The volume exceeds a safe threshold", Toast.LENGTH_SHORT)
                                toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 100)
                                toast.show()
                                // Vibrate for a short duration
                                val vibrationEffect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                                vibrator.vibrate(vibrationEffect)
                            } else {
                                binding.textViewDb.setTextColor(ContextCompat.getColor(this, R.color.textColor))
                            }
                        }
                    }
                }

                audioRecord.stop()
                audioRecord.release()
            }.start()
        } catch (e: SecurityException) {
            // Handle the case when permission is not granted
            binding.textViewDb.text = "Permission denied"
        }
    }

    private fun stopRecording() {
        isRecording = false
        runOnUiThread {
            currentDb = 0.0 // Reset the current dB value
            binding.textViewDb.text = String.format("%.1f dB", currentDb) // Update the loudness meter
            binding.textViewDb.setTextColor(ContextCompat.getColor(this, R.color.textColor))
        }
    }

    private fun showThresholdDialog() {
        val dialog = ThresholdDialogFragment(warningThreshold)
        dialog.onThresholdSetListener = { threshold ->
            warningThreshold = threshold
        }
        dialog.show(supportFragmentManager, "ThresholdDialog")
    }
}
