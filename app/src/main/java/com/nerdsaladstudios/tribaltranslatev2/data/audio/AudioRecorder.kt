package com.nerdsaladstudios.tribaltranslatev2.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorder(
    private val context: Context
) {

    companion object {
        const val TAG = "AudioRecorder"

        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording = false

    private val outputStream = ByteArrayOutputStream()

    fun startRecording(): Boolean {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return false
        }

        if (isRecording) {
            Log.w(TAG, "Already recording")
            return false
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (
            minBufferSize == AudioRecord.ERROR ||
            minBufferSize == AudioRecord.ERROR_BAD_VALUE
        ) {
            Log.e(TAG, "Invalid AudioRecord buffer size: $minBufferSize")
            return false
        }

        val bufferSize = maxOf(
            minBufferSize * 2,
            4096
        )

        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                recorder.release()
                return false
            }

            synchronized(outputStream) {
                outputStream.reset()
            }

            recorder.startRecording()

            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord did not enter RECORDSTATE_RECORDING")
                recorder.release()
                return false
            }

            audioRecord = recorder
            isRecording = true

            recordingThread = Thread {

                val buffer = ShortArray(1024)

                Log.d(TAG, "Recording thread started")

                while (isRecording) {

                    val read = try {
                        recorder.read(
                            buffer,
                            0,
                            buffer.size
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "AudioRecord.read() failed", e)
                        break
                    }

                    if (read > 0) {

                        synchronized(outputStream) {

                            val byteBuffer = ByteBuffer
                                .allocate(read * 2)
                                .order(ByteOrder.LITTLE_ENDIAN)

                            for (i in 0 until read) {
                                byteBuffer.putShort(buffer[i])
                            }

                            outputStream.write(
                                byteBuffer.array()
                            )
                        }

                    } else if (read < 0) {
                        Log.e(
                            TAG,
                            "AudioRecord.read() returned error: $read"
                        )
                        break
                    }
                }

                Log.d(TAG, "Recording thread stopped")
            }

            recordingThread?.start()

            Log.d(
                TAG,
                "Recording started: sampleRate=$SAMPLE_RATE, bufferSize=$bufferSize"
            )

            return true

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start recording",
                e
            )

            isRecording = false
            audioRecord?.release()
            audioRecord = null

            return false
        }
    }

    suspend fun stopRecording(): ShortArray =
        withContext(Dispatchers.IO) {

            if (!isRecording) {
                Log.w(TAG, "stopRecording() called while not recording")
                return@withContext ShortArray(0)
            }

            isRecording = false

            val recorder = audioRecord

            try {
                recorder?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AudioRecord", e)
            }

            try {
                recordingThread?.join(1000)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Interrupted while waiting for recording thread", e)
                Thread.currentThread().interrupt()
            }

            try {
                recorder?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release AudioRecord", e)
            }

            audioRecord = null
            recordingThread = null

            val bytes = synchronized(outputStream) {
                outputStream.toByteArray()
            }

            outputStream.reset()

            if (bytes.isEmpty()) {
                Log.w(TAG, "No audio bytes captured")
                return@withContext ShortArray(0)
            }

            val shortBuffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()

            val samples = ShortArray(
                shortBuffer.remaining()
            )

            shortBuffer.get(samples)

            val durationSeconds =
                samples.size.toDouble() / SAMPLE_RATE

            Log.d(
                TAG,
                "Recording stopped: samples=${samples.size}, duration=${"%.2f".format(durationSeconds)}s"
            )

            samples
        }

    fun isRecording(): Boolean {
        return isRecording
    }
}