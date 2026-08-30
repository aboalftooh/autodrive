package com.autodrive.app.feature.home.presentation.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * أصوات بنزين — مُولَّدة برمجياً بدون ملفات صوتية.
 * تعادل Web Audio API في benzeen_pump.html
 */
object BenzineSound {

    private const val RATE = 44100
    private val scope = CoroutineScope(Dispatchers.IO)

    // ═══════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════

    /** طقة واحدة جافة — ما في رصيد جديد */
    fun playEmptyTick() {
        scope.launch { playBuffer(genClick(2600f, 0.06f)) }
    }

    /**
     * سلسلة طقطقات تتباطأ مع تقدم العداد + ka-thunk في النهاية.
     * [durationMs] = مدة حركة العداد
     */
    fun playPumpFill(durationMs: Int) {
        scope.launch {
            val totalMs = durationMs + 350
            val buf     = ShortArray(RATE * totalMs / 1000)

            // طقطقات بمعدل يتباطأ كلما اقتربنا من الرقم النهائي
            // مثل HTML: clickInterval = 50 + progress * 120
            var tMs = 0
            while (tMs < durationMs) {
                val progress = tMs.toFloat() / durationMs
                val freq     = (2400f + (Math.random() * 400).toFloat())
                mixInto(buf, tMs * RATE / 1000, genClick(freq, 0.08f))
                val interval = (50 + progress * 120).toInt().coerceAtLeast(30)
                tMs += interval
            }

            // ka-thunk عند توقف العداد
            mixInto(buf, durationMs * RATE / 1000, genKaThunk())
            playBuffer(buf)
        }
    }

    /** نغمات صاعدة احتفالية — التنك اكتمل */
    fun playTankFull() {
        scope.launch { playBuffer(genCelebration()) }
    }

    // ═══════════════════════════════════════════
    // Waveform generators
    // ═══════════════════════════════════════════

    /**
     * طقة معدنية — square wave بتردد يهبط سريعاً.
     * يعادل playClick() في الـ HTML
     */
    private fun genClick(startFreq: Float, amplitude: Float): ShortArray {
        val n   = RATE * 55 / 1000   // 55ms
        val buf = ShortArray(n)
        var ph  = 0.0
        for (i in 0 until n) {
            val t    = i.toDouble() / RATE
            val freq = startFreq * exp(-t * 30.0)   // sweep تنازلي للتردد
            ph      += 2 * PI * freq / RATE
            val wave = if (sin(ph) >= 0) 1.0 else -1.0   // square wave
            val env  = exp(-t * 80.0) * amplitude
            buf[i]   = (wave * env * 32767).toInt().toShort()
        }
        return buf
    }

    /**
     * الدقة الثقيلة عند إيقاف الطلمبة.
     * يعادل playKaThunk() في الـ HTML
     */
    private fun genKaThunk(): ShortArray {
        val n   = RATE * 280 / 1000   // 280ms
        val buf = ShortArray(n)

        // الجزء الثقيل — sine يهبط من 120 إلى ~10 Hz
        var ph1 = 0.0
        for (i in 0 until n) {
            val t    = i.toDouble() / RATE
            val freq = 120.0 * exp(-t * 8.0)
            ph1     += 2 * PI * freq / RATE
            val env  = exp(-t * 14.0) * 0.4
            buf[i]   = mix(buf[i], (sin(ph1) * env * 32767).toInt())
        }

        // طقة مزدوجة فوقه — square wave 1500 Hz
        val clickN = RATE * 55 / 1000
        var ph2    = 0.0
        for (i in 0 until clickN) {
            val t    = i.toDouble() / RATE
            ph2     += 2 * PI * 1500.0 / RATE
            val wave = if (sin(ph2) >= 0) 1.0 else -1.0
            val env  = exp(-t * 80.0) * 0.1
            buf[i]   = mix(buf[i], (wave * env * 32767).toInt())
        }
        return buf
    }

    /**
     * أربع نغمات صاعدة: C5 → E5 → G5 → C6.
     * يعادل playTankFull() في الـ HTML
     */
    private fun genCelebration(): ShortArray {
        val notes     = listOf(523.25, 659.25, 783.99, 1046.50)  // C5 E5 G5 C6
        val noteMs    = 420
        val staggerMs = 80
        val totalMs   = staggerMs * (notes.size - 1) + noteMs + 100
        val buf       = ShortArray(RATE * totalMs / 1000)

        notes.forEachIndexed { i, freq ->
            val start = i * staggerMs * RATE / 1000
            val n     = noteMs * RATE / 1000
            var ph    = 0.0
            for (j in 0 until n) {
                if (start + j >= buf.size) break
                val t       = j.toDouble() / RATE
                ph         += 2 * PI * freq / RATE
                val attack  = minOf(1.0, t / 0.02)      // صعود 20ms
                val decay   = exp(-t * 5.0)              // تلاشٍ تدريجي
                val env     = attack * decay * 0.15
                buf[start + j] = mix(buf[start + j], (sin(ph) * env * 32767).toInt())
            }
        }
        return buf
    }

    // ═══════════════════════════════════════════
    // Audio utilities
    // ═══════════════════════════════════════════

    private fun mixInto(dest: ShortArray, offset: Int, src: ShortArray) {
        for (i in src.indices) {
            val idx = offset + i
            if (idx >= dest.size) break
            dest[idx] = mix(dest[idx], src[i].toInt())
        }
    }

    private fun mix(a: Short, b: Int): Short =
        (a.toInt() + b).coerceIn(-32768, 32767).toShort()

    private fun playBuffer(buf: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buf, 0, buf.size)
            track.play()
            Thread.sleep(buf.size.toLong() * 1000 / RATE + 150)
            track.stop()
            track.release()
        }
    }
}
