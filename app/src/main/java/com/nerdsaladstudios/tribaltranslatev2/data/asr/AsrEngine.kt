package com.nerdsaladstudios.tribaltranslatev2.data.asr

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.nerdsaladstudios.tribaltranslatev2.domain.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer

class AsrEngine(private val context: Context) {

    companion object {
        const val TAG = "AsrEngine"
        const val SAMPLE_RATE = 16000
    }

    suspend fun transcribe(
        pcmAudio: ShortArray,
        language: TranslationLanguage
    ): String = withContext(Dispatchers.Default) {
        if (pcmAudio.isEmpty()) {
            Log.w(TAG, "Empty PCM audio provided for ASR transcription")
            return@withContext ""
        }

        Log.d(
            TAG,
            "Loading ASR model for ${language.displayName} (${pcmAudio.size} samples)..."
        )

        val modelFilename = if (language == TranslationLanguage.HINDI) {
            "models/asr/hi_asr.onnx"
        } else {
            "models/asr/sat_asr.onnx"
        }

        val tokensFilename = if (language == TranslationLanguage.HINDI) {
            "models/asr/hi_tokens.txt"
        } else {
            "models/asr/sat_tokens.txt"
        }

        val modelFile = File(context.filesDir, modelFilename)
        val tokensFile = File(context.filesDir, tokensFilename)

        val tokens = if (tokensFile.exists()) {
            try { tokensFile.readLines() } catch (e: Exception) { emptyList() }
        } else emptyList()

        val modelExists = modelFile.exists()
        val modelLength = if (modelExists) modelFile.length() else 0L
        var isPlaceholder = false
        var checkException: String? = null
        
        if (modelExists && modelLength > 100_000) {
            try {
                isPlaceholder = modelFile.inputStream().use { stream ->
                    val buf = ByteArray(64)
                    val read = stream.read(buf)
                    if (read > 0) String(buf, 0, read) else ""
                }.startsWith("OFFLINE_MODEL_PLACEHOLDER")
            } catch (e: Exception) {
                checkException = e.message
            }
        }

        val isRealOnnx = modelExists && modelLength > 100_000 && !isPlaceholder && checkException == null

        if (!isRealOnnx) {
            Log.w(TAG, "ASR Model Validation Failed! Cannot perform inference. Reason:")
            Log.w(TAG, "- exists: $modelExists (path: ${modelFile.absolutePath})")
            Log.w(TAG, "- length: $modelLength (needs > 100000)")
            Log.w(TAG, "- isPlaceholder: $isPlaceholder")
            if (checkException != null) Log.w(TAG, "- exception: $checkException")
            return@withContext ""
        }

        try {
            Log.d(TAG, "Executing ONNX Runtime inference on ${modelFile.name}...")
            val result = runOnnxInference(modelFile, pcmAudio, tokens)
            if (result.isNotBlank()) {
                return@withContext result
            } else {
                Log.w(TAG, "ONNX Runtime inference returned a blank string.")
                return@withContext ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "ONNX Runtime inference failed: ${e.message}", e)
            return@withContext ""
        }
    }

    private fun runOnnxInference(
        modelFile: File,
        pcmAudio: ShortArray,
        tokens: List<String>
    ): String {
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions()
        var session: OrtSession? = null

        try {
            session = env.createSession(modelFile.absolutePath, sessionOptions)

            val inputName = session.inputNames.iterator().next()
            val inputInfo = session.inputInfo[inputName]?.info as? ai.onnxruntime.TensorInfo
            val expectedShape = inputInfo?.shape

            Log.d(TAG, "ONNX Model expects input '$inputName' with shape: ${expectedShape?.contentToString()}")

            val floatAudio = FloatArray(pcmAudio.size) { i ->
                pcmAudio[i].toFloat() / 32768.0f
            }

            // Construct shape based on expected rank
            val shape = if (expectedShape != null && expectedShape.size == 3) {
                // Determine if it's [1, 1, time] or [1, time, 1]
                if (expectedShape[1] == 1L) {
                    longArrayOf(1, 1, floatAudio.size.toLong())
                } else if (expectedShape[2] == 1L) {
                    longArrayOf(1, floatAudio.size.toLong(), 1)
                } else {
                    // Fallback to [1, 1, time] if unknown
                    longArrayOf(1, 1, floatAudio.size.toLong())
                }
            } else {
                longArrayOf(1, floatAudio.size.toLong())
            }

            Log.d(TAG, "Creating tensor with shape: ${shape.contentToString()}")
            val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatAudio), shape)

            // Some models also expect "length" or "audio_signal_length"
            val inputs = mutableMapOf<String, OnnxTensor>()
            inputs[inputName] = tensor
            
            var lengthTensor: OnnxTensor? = null
            for (name in session.inputNames) {
                if (name != inputName && (name.contains("length") || name == "audio_signal_length")) {
                    lengthTensor = OnnxTensor.createTensor(env, longArrayOf(floatAudio.size.toLong()))
                    inputs[name] = lengthTensor
                }
            }

            val results = session.run(inputs)

            val outputTensor = results.get(0).value
            val transcript = parseCtcLogits(outputTensor, tokens)

            tensor.close()
            lengthTensor?.close()
            results.close()
            return transcript
        } finally {
            try { session?.close() } catch (e: Exception) {}
            try { sessionOptions.close() } catch (e: Exception) {}
        }
    }

    private fun parseCtcLogits(logitsObj: Any, tokens: List<String>): String {
        if (tokens.isEmpty()) return ""
        val builder = StringBuilder()
        if (logitsObj is Array<*>) {
            for (row in logitsObj) {
                if (row is FloatArray) {
                    var maxIdx = 0
                    var maxVal = Float.NEGATIVE_INFINITY
                    for (i in row.indices) {
                        if (row[i] > maxVal) {
                            maxVal = row[i]
                            maxIdx = i
                        }
                    }
                    if (maxIdx in tokens.indices) {
                        val token = tokens[maxIdx]
                        if (token != "<blank>" && token != "<pad>" && token != "<s>" && token != "</s>") {
                            builder.append(token)
                        }
                    }
                }
            }
        }
        return builder.toString().trim()
    }
}
