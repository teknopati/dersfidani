package com.dersfidan.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.dersfidan.app.R

/** Uygulamadaki kısa efektleri, hayvan seslerini ve çiftlik müziğini tek yerden yönetir. */
object SesYoneticisi {
    private lateinit var appContext: Context
    private var hazir = false
    private var soundPool: SoundPool? = null
    private val efektler = mutableMapOf<Efekt, Int>()
    private var hayvanOynatici: MediaPlayer? = null
    private var ciftlikMuzigi: MediaPlayer? = null
    private var ciftlikAcik = false
    private var uygulamaOnda = true
    private var dokunmaSerisi = 0L
    private var bastirilanDokunma = -1L
    private val handler = Handler(Looper.getMainLooper())

    private enum class Efekt { BUTON, LEVEL, ALKIS, ONAYLA }

    fun baslat(context: Context) {
        if (hazir) return
        appContext = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build().also { pool ->
            efektler[Efekt.BUTON] = pool.load(appContext, R.raw.butonsesi, 1)
            efektler[Efekt.LEVEL] = pool.load(appContext, R.raw.levelup, 1)
            efektler[Efekt.ALKIS] = pool.load(appContext, R.raw.alkis, 1)
            efektler[Efekt.ONAYLA] = pool.load(appContext, R.raw.onayla, 1)
        }
        hazir = true
    }

    /** Activity ACTION_UP'tan önce çağırır; özel bir ses gelirse bu genel tık iptal edilir. */
    fun butonSesiGecikmeli() {
        if (!hazir) return
        val seri = ++dokunmaSerisi
        handler.postDelayed({
            if (bastirilanDokunma != seri) efektCal(Efekt.BUTON, .72f)
        }, 65L)
    }

    fun levelAtlamaCal() {
        ozelTiklamayiBastir()
        efektCal(Efekt.LEVEL, .92f)
    }

    fun alkisCal() {
        ozelTiklamayiBastir()
        efektCal(Efekt.ALKIS, .95f)
    }

    /** Bir görev sağa kaydırılarak veya ders ayrıntısından tamamlandığında çalar. */
    fun onaylaCal() {
        ozelTiklamayiBastir()
        efektCal(Efekt.ONAYLA, .92f)
    }

    fun hayvanSesiCal(esyaId: String) {
        if (!hazir) return
        val kaynak = hayvanKaynaklari[esyaId] ?: return
        ozelTiklamayiBastir()
        hayvanSesiDurdur()
        runCatching {
            MediaPlayer.create(appContext, kaynak)?.apply {
                setVolume(.9f, .9f)
                setOnCompletionListener { tamamlanan ->
                    runCatching { tamamlanan.release() }
                    if (hayvanOynatici === tamamlanan) hayvanOynatici = null
                }
                hayvanOynatici = this
                start()
            }
        }.onFailure { hayvanOynatici = null }
    }

    /** Hayvan galerisi kapanırken devam eden uzun sesi hemen keser. */
    fun hayvanSesiDurdur() {
        hayvanOynatici?.runCatching { stop() }
        hayvanOynatici?.runCatching { release() }
        hayvanOynatici = null
    }

    fun ciftlikMuzigiBaslat() {
        if (!hazir) return
        ciftlikAcik = true
        if (!uygulamaOnda) return
        val oynatici = ciftlikMuzigi ?: MediaPlayer.create(appContext, R.raw.farmmusic)?.apply {
            isLooping = true
            setVolume(.32f, .32f)
        }?.also { ciftlikMuzigi = it }
        if (oynatici?.isPlaying == false) oynatici.start()
    }

    fun ciftlikMuzigiDurdur() {
        ciftlikAcik = false
        ciftlikMuzigi?.takeIf { it.isPlaying }?.pause()
        ciftlikMuzigi?.seekTo(0)
        hayvanSesiDurdur()
    }

    fun uygulamaArkaPlanda() {
        uygulamaOnda = false
        ciftlikMuzigi?.takeIf { it.isPlaying }?.pause()
    }

    fun uygulamaOnPlanda() {
        uygulamaOnda = true
        if (ciftlikAcik) ciftlikMuzigiBaslat()
    }

    private fun ozelTiklamayiBastir() {
        bastirilanDokunma = dokunmaSerisi
    }

    private fun efektCal(efekt: Efekt, ses: Float) {
        val id = efektler[efekt] ?: return
        soundPool?.play(id, ses, ses, 1, 0, 1f)
    }

    private val hayvanKaynaklari by lazy {
        mapOf(
            "alpaka" to R.raw.alpaca,
            "yarasa" to R.raw.bat,
            "ari" to R.raw.bee,
            "kus" to R.raw.bird,
            "kelebek" to R.raw.butterfly,
            "kedi" to R.raw.cat,
            "civciv" to R.raw.chick,
            "tavuk" to R.raw.chicken,
            "inek" to R.raw.cow,
            "karga" to R.raw.crow,
            "geyik" to R.raw.deer,
            "kopek" to R.raw.dog,
            "esek" to R.raw.donkey,
            "ordek" to R.raw.duck,
            "kartal" to R.raw.eagle,
            "fare" to R.raw.field_mouse,
            "flamingo" to R.raw.flamingo,
            "tilki" to R.raw.fox,
            "keci" to R.raw.goat,
            "at" to R.raw.horse,
            "ugurbocegi" to R.raw.ladybug,
            "aslan" to R.raw.lion,
            "maymun" to R.raw.monkey,
            "baykus" to R.raw.owl,
            "papagan" to R.raw.parrot,
            "tavsan" to R.raw.rabbit,
            "koyun" to R.raw.sheep,
            "sincap" to R.raw.squirrel,
            "hindi" to R.raw.turkey,
            "zebra" to R.raw.zebra,
            "kurbaga" to R.raw.frog
        )
    }
}
