package com.myraa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.myraa.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // Standard Android speech recognition permission flow
    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening() else showError(getString(R.string.mic_permission_denied))
        }

    // MediaProjection is how Android grants screen-capture access. On some devices/OS builds
    // (emulators without GPU support, very old OEM skins, certain work-profile restrictions)
    // this API is genuinely unavailable — that's the real-world cause of "Not supported".
    private val screenCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                hideError()
                // Hand off projection intent + resultCode to ScreenCaptureService here.
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, result.data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                showError(getString(R.string.capture_error_message))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.micButton.setOnClickListener { onMicTapped() }
        binding.closeButton.setOnClickListener { stopListening() }
        binding.txtDismiss.setOnClickListener { hideError() }
    }

    private fun onMicTapped() {
        if (isListening) {
            stopListening()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showError("Speech recognition is not available on this device.")
            return
        }

        hideError()
        isListening = true
        binding.txtPrompt.text = getString(R.string.listening_prompt)

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(recognitionListener)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        isListening = false
        binding.txtPrompt.text = getString(R.string.idle_prompt)
        speechRecognizer?.stopListening()
    }

    /** Call this wherever MYRAA's "see the screen" feature is triggered. */
    private fun requestScreenCapture() {
        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager

        if (projectionManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            showError(getString(R.string.capture_error_message))
            return
        }
        screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun showError(message: String) {
        binding.txtErrorMessage.text = message
        binding.errorCard.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.errorCard.visibility = View.GONE
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
            binding.txtPrompt.text = getString(R.string.idle_prompt)
        }

        override fun onError(error: Int) {
            isListening = false
            binding.txtPrompt.text = getString(R.string.idle_prompt)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) {
                // TODO: forward `text` to MYRAA's response/LLM pipeline
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
