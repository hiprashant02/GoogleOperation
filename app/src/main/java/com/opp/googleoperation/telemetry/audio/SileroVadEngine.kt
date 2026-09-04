package com.opp.googleoperation.telemetry.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer

class SileroVadEngine(private val ctx: Context) {

    companion object {
        private const val TAG = "SileroVadEngine"
        private const val MODEL_ASSET = "silero_vad.onnx"
        const val SAMPLE_RATE = 16000
        const val WINDOW_SIZE_SAMPLES = 512 // 32ms at 16kHz
        private const val CONTEXT_SIZE = 64 // required context tail for Silero v5
        private const val STATE_SIZE = 2 * 1 * 128 // LSTM hidden state shape: [2, 1, 128]
    }

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    // Context buffer (last 64 samples of previous frame)
    private val contextBuffer = FloatArray(CONTEXT_SIZE)

    // Hidden state for recurrent layers
    private var stateBuffer = FloatArray(STATE_SIZE)

    fun initialize(): Boolean {
        if (isInitialized) return true
        return try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = ctx.assets.open(MODEL_ASSET).use { it.readBytes() }
            val sessionOptions = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(1)
                setInterOpNumThreads(1)
            }
            ortSession = ortEnv?.createSession(modelBytes, sessionOptions)
            reset()
            isInitialized = true
            Log.d(TAG, "Silero VAD v5 ONNX engine initialized successfully (${modelBytes.size / 1024} KB)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "failed to initialize Silero VAD ONNX engine", e)
            isInitialized = false
            false
        }
    }

    fun processFrame(pcmChunk512: ShortArray): Float {
        if (!isInitialized || ortSession == null || ortEnv == null) {
            return 0.0f
        }

        try {
            val env = ortEnv ?: return 0.0f
            val session = ortSession ?: return 0.0f

            // 1. Build 576-sample input: 64 context samples + 512 new normalized samples
            val inputFloats = FloatArray(CONTEXT_SIZE + WINDOW_SIZE_SAMPLES)
            System.arraycopy(contextBuffer, 0, inputFloats, 0, CONTEXT_SIZE)

            for (i in 0 until WINDOW_SIZE_SAMPLES) {
                val sample = if (i < pcmChunk512.size) pcmChunk512[i] else 0
                // Normalize 16-bit PCM short to [-1.0f, 1.0f]
                inputFloats[CONTEXT_SIZE + i] = (sample / 32768.0f).coerceIn(-1.0f, 1.0f)
            }

            // 2. Update context buffer with the last 64 samples for next iteration
            System.arraycopy(inputFloats, WINDOW_SIZE_SAMPLES, contextBuffer, 0, CONTEXT_SIZE)

            // 3. Create input tensors
            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(inputFloats),
                longArrayOf(1, (CONTEXT_SIZE + WINDOW_SIZE_SAMPLES).toLong())
            )

            val stateTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(stateBuffer),
                longArrayOf(2, 1, 128)
            )

            val srTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(longArrayOf(SAMPLE_RATE.toLong())),
                longArrayOf(1)
            )

            // 4. Run ONNX model inference
            val inputs = mapOf(
                "input" to inputTensor,
                "state" to stateTensor,
                "sr" to srTensor
            )

            val results = session.run(inputs)

            // 5. Extract output speech probability
            var speechProb = 0.0f
            val outputTensor = results.get(0)
            if (outputTensor is OnnxTensor) {
                val outputBuffer = outputTensor.floatBuffer
                if (outputBuffer.hasRemaining()) {
                    speechProb = outputBuffer.get()
                }
            }

            // 6. Update hidden state tensor from stateN output
            if (results.size() > 1) {
                val nextStateTensor = results.get(1)
                if (nextStateTensor is OnnxTensor) {
                    val nextStateBuffer = nextStateTensor.floatBuffer
                    nextStateBuffer.get(stateBuffer)
                }
            }

            // Clean up native tensors
            inputTensor.close()
            stateTensor.close()
            srTensor.close()
            results.close()

            return speechProb
        } catch (e: Exception) {
            Log.w(TAG, "inference error", e)
            return 0.0f
        }
    }

    fun reset() {
        contextBuffer.fill(0.0f)
        stateBuffer.fill(0.0f)
    }

    fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (_: Exception) {}
        ortSession = null
        ortEnv = null
        isInitialized = false
    }
}
