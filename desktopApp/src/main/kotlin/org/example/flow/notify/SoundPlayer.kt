package org.example.flow.notify

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.Line
import javax.sound.sampled.Mixer

/**
 * 异步播放 WAV 音效，不阻塞 UI 线程。
 * 使用 JDK 内置 javax.sound.sampled，零额外依赖。
 */
object SoundPlayer {

    private val TARGET_FORMAT = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f,  // sample rate
        16,      // sample size in bits
        2,       // channels (stereo)
        4,       // frame size (2 bytes × 2 channels)
        44100f,  // frame rate
        false,   // little-endian (WAV standard)
    )

    /**
     * 在独立线程中播放 WAV 文件。
     * 播放失败时仅打印日志，不影响主流程。
     */
    fun play(file: File) {
        Thread {
            var clip: javax.sound.sampled.Clip? = null
            var stream: javax.sound.sampled.AudioInputStream? = null
            try {
                println("[SoundPlayer] Loading: ${file.name} (${file.length()} bytes)")

                // Step 1: 列出所有音频混音器（排查输出设备）
                listMixers()

                // Step 2: 打开原始音频流
                stream = AudioSystem.getAudioInputStream(file)
                val originalFormat = stream.format
                val durationMs = if (originalFormat.sampleRate > 0 && originalFormat.frameSize > 0) {
                    stream.frameLength * 1000L / originalFormat.sampleRate.toLong()
                } else -1L
                println("[SoundPlayer] Original format: encoding=${originalFormat.encoding}, " +
                        "rate=${originalFormat.sampleRate}, bits=${originalFormat.sampleSizeInBits}, " +
                        "channels=${originalFormat.channels}, frames=${stream.frameLength}, " +
                        "duration=${durationMs}ms")

                // Step 3: 尝试获取 Clip 并直接打开
                clip = AudioSystem.getClip()
                val clipInfo = clip.lineInfo
                println("[SoundPlayer] Clip line info: ${clipInfo}")

                try {
                    clip.open(stream)
                    println("[SoundPlayer] Direct open succeeded")
                } catch (e: IllegalArgumentException) {
                    println("[SoundPlayer] Direct open failed: ${e.message}")
                    println("[SoundPlayer] Converting to PCM_SIGNED 44100Hz 16bit stereo...")
                    stream.close()
                    stream = AudioSystem.getAudioInputStream(file)
                    val converted = AudioSystem.getAudioInputStream(TARGET_FORMAT, stream)
                    clip.open(converted)
                    println("[SoundPlayer] Converted and opened successfully")
                }

                // Step 4: 检查并设置音量到最大
                setVolumeToMax(clip)

                // Step 5: 检查 clip 是否真的可以播放
                println("[SoundPlayer] Clip isOpen=${clip.isOpen}, isActive=${clip.isActive}, " +
                        "microseconds=${clip.microsecondLength}, " +
                        "bufferSize=${clip.bufferSize}, framePosition=${clip.framePosition}")

                // Step 6: 播放
                println("[SoundPlayer] ▶ Starting playback of ${file.name}...")
                clip.start()

                // 等待播放开始（确保音频线真正开始输出）
                Thread.sleep(50)
                println("[SoundPlayer] Clip isRunning=${clip.isRunning}, isActive=${clip.isActive}, " +
                        "framePosition=${clip.framePosition}")

                clip.drain()
                println("[SoundPlayer] ✓ Finished: ${file.name}")

            } catch (e: Exception) {
                println("[SoundPlayer] ✗ FAILED: ${file.name}")
                println("[SoundPlayer]   Exception: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
            } finally {
                try { clip?.close() } catch (_: Exception) {}
                try { stream?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    /** 列出系统中所有音频混音器 */
    private fun listMixers() {
        val mixerInfos = AudioSystem.getMixerInfo()
        println("[SoundPlayer] System has ${mixerInfos.size} audio mixer(s):")
        mixerInfos.forEach { info ->
            val mixer = AudioSystem.getMixer(info)
            val srcLines = mixer.sourceLines
            println("[SoundPlayer]   - ${info.name} (${info.vendor}, ${info.description})")
            println("[SoundPlayer]     source lines: ${srcLines?.size ?: 0}")
        }
    }

    /** 尝试将 Clip 音量设到最大 */
    private fun setVolumeToMax(clip: javax.sound.sampled.Clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val max = gain.maximum
                val min = gain.minimum
                val current = gain.value
                gain.value = max
                println("[SoundPlayer] Volume: min=${min}dB, max=${max}dB, was=${current}dB → set to ${max}dB")
            } else {
                println("[SoundPlayer] Volume: MASTER_GAIN not supported on this clip")
            }
        } catch (e: Exception) {
            println("[SoundPlayer] Volume: control failed - ${e.message}")
        }
    }
}
