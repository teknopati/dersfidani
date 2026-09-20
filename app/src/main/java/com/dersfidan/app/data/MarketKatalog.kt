package com.dersfidan.app.data

import kotlin.math.ceil

enum class EsyaTuru { HAYVAN, YAPI }

data class MarketEsyasi(
    val id: String,
    val isim: String,
    val emoji: String,
    val fiyat: Int,
    val aciklama: String,
    val tur: EsyaTuru = EsyaTuru.HAYVAN,
    val modelDosyasi: String,
    val geceCanlisi: Boolean = false,
    val ucucu: Boolean = false,
    val sahneBoyu: Float = 1f
)

/** Market, koleksiyon ve resimli galeri için hayvan/yapı kataloğu. */
object MarketKatalog {
    const val HAYVAN_ADET_SINIRI = 5

    private fun hayvan(
        id: String, isim: String, emoji: String, fiyat: Int, model: String,
        aciklama: String, boy: Float = 1f, ucucu: Boolean = false, gece: Boolean = false
    ) = MarketEsyasi(id, isim, emoji, fiyat, aciklama, EsyaTuru.HAYVAN,
        "models/animals/$model", gece, ucucu, boy)

    private val hayvanListesi = listOf(
        hayvan("alpaka", "Alpaka", "🦙", 120, "alpaca.glb", "Yumuşacık yünüyle dolaşır.", 1.25f),
        hayvan("yarasa", "Yarasa", "🦇", 85, "bat.glb", "Geceleri habitatında uçar.", .65f, true, true),
        hayvan("ari", "Arı", "🐝", 15, "bee.glb", "Çiçeklerin çevresinde uçar.", .32f, true),
        hayvan("kus", "Kuş", "🐦", 25, "bird.glb", "Ağaçların çevresinde dolaşır.", .5f, true),
        hayvan("kelebek", "Kelebek", "🦋", 10, "butterfly.glb", "Çiçekten çiçeğe süzülür.", .3f, true),
        hayvan("kedi", "Kedi", "🐱", 50, "cat.glb", "Güneşlenir, yürür ve dinlenir.", .72f),
        hayvan("civciv", "Civciv", "🐤", 20, "chick.glb", "Küçük adımlarla gezer.", .35f),
        hayvan("tavuk", "Tavuk", "🐔", 30, "chicken.glb", "Gagalayarak dolaşır.", .62f),
        hayvan("inek", "İnek", "🐄", 110, "cow.glb", "Otlakta yürür.", 1.45f),
        hayvan("karga", "Karga", "🐦‍⬛", 55, "crow.glb", "Parlak şeylerin çevresinde uçar.", .55f, true),
        hayvan("geyik", "Geyik", "🦌", 170, "deer.glb", "Ağaçlık alanda dolaşır.", 1.35f),
        hayvan("kopek", "Köpek", "🐶", 95, "dog.glb", "Çiftliği bekler ve oyun oynar.", .85f),
        hayvan("esek", "Eşek", "🫏", 100, "donkey.glb", "Çiftlik yollarında yürür.", 1.25f),
        hayvan("ordek", "Ördek", "🦆", 45, "duck.glb", "Gölet çevresinde gezinir.", .52f),
        hayvan("kartal", "Kartal", "🦅", 180, "eagle.glb", "Yüksekten daireler çizer.", .8f, true),
        hayvan("fare", "Tarla Faresi", "🐭", 30, "field_mouse.glb", "Tahılların çevresinde koşar.", .28f),
        hayvan("flamingo", "Flamingo", "🦩", 130, "flamingo.glb", "Gölet kıyısında bekler.", 1.1f),
        hayvan("tilki", "Tilki", "🦊", 140, "fox.glb", "Ormanlık köşede dolaşır.", .82f),
        hayvan("keci", "Keçi", "🐐", 75, "goat.glb", "Merakla çevresini araştırır.", .95f),
        hayvan("at", "At", "🐴", 180, "horse.glb", "Geniş çayırlıkta yürür.", 1.5f),
        hayvan("ugurbocegi", "Uğur Böceği", "🐞", 20, "ladybug.glb", "Bitkilerin arasında gezinir.", .22f),
        hayvan("aslan", "Aslan", "🦁", 220, "lion.glb", "Çiftliğin özel konuğu.", 1.25f),
        hayvan("maymun", "Maymun", "🐒", 160, "monkey.glb", "Ağaçların yanında oyun oynar.", .82f),
        hayvan("baykus", "Baykuş", "🦉", 90, "owl.glb", "Geceleri uçar, gündüz dinlenir.", .62f, true, true),
        hayvan("panda", "Panda", "🐼", 230, "panda.glb", "Bambu köşesinde dinlenir.", 1.05f),
        hayvan("tavsan", "Tavşan", "🐰", 60, "rabbit.glb", "Çimlerde zıplar.", .48f),
        hayvan("koyun", "Koyun", "🐑", 70, "sheep.glb", "Çimlerde otlar.", .9f),
        hayvan("salyangoz", "Salyangoz", "🐌", 25, "snail.glb", "Yağmurdan sonra dolaşır.", .25f),
        hayvan("sincap", "Sincap", "🐿️", 40, "squirrel.glb", "Ağaçların çevresinde koşar.", .5f),
        hayvan("hindi", "Hindi", "🦃", 65, "turkey.glb", "Kabararak gezer.", .8f),
        hayvan("zebra", "Zebra", "🦓", 210, "zebra.glb", "Geniş çayırlıkta dolaşır.", 1.35f),
        hayvan("papagan", "Papağan", "🦜", 100, "parrot.glb", "Rengârenk tüyleriyle uçar.", .58f, true),
        hayvan("kaplumbaga", "Kaplumbağa", "🐢", 70, "turtle.glb", "Sakin sakin yürür.", .45f),
        hayvan("kurbaga", "Kurbağa", "🐸", 35, "frog.glb", "Gölet kenarında zıplar.", .34f)
    )

    private fun yapi(id: String, isim: String, emoji: String, fiyat: Int, aciklama: String, model: String, boy: Float = 2.2f) =
        MarketEsyasi(id, isim, emoji, fiyat, aciklama, EsyaTuru.YAPI, model, sahneBoyu = boy)

    private val yapiListesi = listOf(
        yapi("ahir", "Ahır", "🏠", 80, "Hayvanların sıcak yuvası.", "models/buildings/Wall_Plaster_WoodGrid.gltf", 3.2f),
        yapi("fener", "Fener", "🏮", 45, "Geceleri sıcak ışık verir.", "models/props/Lantern_Wall.gltf", 1.2f),
        yapi("samanyigin", "Saman ve Yem Alanı", "🌾", 35, "Hayvanların yem köşesi.", "models/props/Barrel_Apples.gltf", 1.4f),
        yapi("cit", "Çit", "🪵", 25, "Çiftlik bölgelerini ayırır.", "models/buildings/Prop_WoodenFence_Single.gltf", 2.2f),
        yapi("cesme", "Çeşme", "⛲", 70, "Su başında dinlenme alanı.", "models/props/Bucket_Wooden_1.gltf", 1.4f),
        yapi("ruzgargulu", "Rüzgâr Bayrağı", "🎏", 55, "Rüzgârda dalgalanır.", "models/props/Banner_1.gltf", 2.8f),
        yapi("cadir", "Çadır", "⛺", 100, "Dinlenme köşesi.", "models/props/Stall_Empty.gltf", 2.8f),
        yapi("degirmen", "Yel Değirmeni", "🌬️", 150, "Çiftliğin yüksek yapısı.", "models/buildings/Roof_Tower_RoundTiles.gltf", 3.6f),
        yapi("kumes", "Kümes", "🐓", 60, "Tavukların evi.", "models/buildings/Wall_Plaster_Door_Round.gltf", 2.6f),
        yapi("kovan", "Arı Kovanı", "🍯", 50, "Arıların bal yuvası.", "models/props/Crate_Wooden.gltf", 1.5f),
        yapi("kopekkulubesi", "Köpek Kulübesi", "🏚️", 40, "Sadık bekçinin evi.", "models/buildings/Roof_Log.gltf", 1.7f),
        yapi("silo", "Tahıl Silosu", "🛢️", 120, "Yem stoklanır.", "models/props/Barrel.gltf", 2.4f),
        yapi("kopru", "Ahşap Köprü", "🌉", 90, "Küçük dereyi geçer.", "models/buildings/Floor_WoodDark.gltf", 3.2f),
        yapi("bahce", "Sebze Bahçesi", "🥕", 65, "Sıra sıra ekili yataklar.", "models/props/FarmCrate_Carrot.gltf", 2.1f),
        yapi("gulbahce", "Çiçek Bahçesi", "🌹", 75, "Rengârenk çiçekler.", "models/trees/Flower_4_Group.gltf", 2.2f),
        yapi("skalar", "Arabalık", "🛖", 55, "Alet ve araba deposu.", "models/props/Stall_Cart_Empty.gltf", 3f),
        yapi("hamak", "Dinlenme Alanı", "🏝️", 45, "Gölgede dinlenme köşesi.", "models/props/Bench.gltf", 1.8f),
        yapi("gunes_pili", "Güneş Enerjisi Alanı", "🔆", 140, "Çiftliğe enerji sağlar.", "models/buildings/Roof_RoundTiles_4x4.gltf", 2.8f),
        yapi("havuz", "Küçük Gölet", "🏞️", 110, "Su hayvanlarının bölgesi.", "models/trees/RockPath_Round_Wide.gltf", 3.2f),
        yapi("bayrak", "Bayrak Direği", "🚩", 30, "Çiftliğin simgesi.", "models/props/Banner_1.gltf", 3f),
        yapi("sera", "Sera", "🏡", 130, "Fideleri sıcak tutar.", "models/buildings/Wall_Plaster_Window_Wide_Round.gltf", 3f),
        yapi("traktor_garaji", "Traktör Garajı", "🚜", 145, "Çiftlik araçlarının evi.", "models/buildings/Wall_Plaster_Door_Round.gltf", 3.4f),
        yapi("sut_evi", "Süt Evi", "🥛", 115, "Süt ürünleri hazırlanır.", "models/buildings/Wall_Plaster_Straight.gltf", 3f),
        yapi("firin", "Taş Fırın", "🧱", 95, "Sıcak ekmek kokar.", "models/buildings/Prop_Chimney.gltf", 2.4f),
        yapi("gozetleme", "Gözetleme Kulesi", "🗼", 160, "Tüm çiftlik görünür.", "models/buildings/Roof_Tower_RoundTiles.gltf", 4f),
        yapi("su_kulesi", "Su Kulesi", "💧", 140, "Sulama suyunu depolar.", "models/props/Barrel.gltf", 3f),
        yapi("misafir_evi", "Misafir Evi", "🏘️", 175, "Çiftlik ziyaretçileri için.", "models/buildings/Wall_Plaster_WoodGrid.gltf", 3.4f),
        yapi("odunluk", "Odunluk", "🪵", 55, "Kışlık odunları korur.", "models/props/Stall_Empty.gltf", 2.6f),
        yapi("kompost", "Kompost Alanı", "♻️", 50, "Toprağı zenginleştirir.", "models/props/FarmCrate_Empty.gltf", 1.8f),
        yapi("saat_kulesi", "Saat Kulesi", "🕰️", 190, "Çiftliğin merkez yapısı.", "models/buildings/Roof_Tower_RoundTiles.gltf", 4.2f)
    )

    val tumEsyalar: List<MarketEsyasi> = hayvanListesi + yapiListesi
    fun bul(id: String): MarketEsyasi? = tumEsyalar.find { it.id == id }
    fun hayvanlar(): List<MarketEsyasi> = hayvanListesi
    fun yapilar(): List<MarketEsyasi> = yapiListesi

    /** Her hayvan türünün ayrı kayıt, kapasite ve görünüme sahip habitat adı. */
    fun habitatAdi(id: String): String = when (id) {
        "alpaka" -> "Alpaka Çayırı"; "yarasa" -> "Yarasa Mağarası"; "ari" -> "Arı Bahçesi"
        "kus" -> "Kuş Korusu"; "kelebek" -> "Kelebek Bahçesi"; "kedi" -> "Kedi Oyun Evi"
        "civciv" -> "Civciv Yuvası"; "tavuk" -> "Tavuk Kümesi"; "inek" -> "İnek Merası"
        "karga" -> "Karga Korusu"; "geyik" -> "Geyik Ormanı"; "kopek" -> "Köpek Bahçesi"
        "esek" -> "Eşek Padoku"; "ordek" -> "Ördek Göleti"; "kartal" -> "Kartal Kayalığı"
        "fare" -> "Tarla Faresi Yuvası"; "flamingo" -> "Flamingo Lagünü"; "tilki" -> "Tilki İni"
        "keci" -> "Keçi Kayalığı"; "at" -> "At Ahırı"; "ugurbocegi" -> "Uğur Böceği Bahçesi"
        "aslan" -> "Aslan Savanı"; "maymun" -> "Maymun Ormanı"; "baykus" -> "Baykuş Korusu"
        "panda" -> "Panda Bambu Bahçesi"; "tavsan" -> "Tavşan Bahçesi"; "koyun" -> "Koyun Otlaklığı"
        "salyangoz" -> "Salyangoz Bahçesi"; "sincap" -> "Sincap Ormanı"; "hindi" -> "Hindi Kümesi"
        "zebra" -> "Zebra Savanı"; "papagan" -> "Papağan Uçuş Evi"; "kaplumbaga" -> "Kaplumbağa Göleti"
        "kurbaga" -> "Kurbağa Göleti"; else -> "${bul(id)?.isim ?: "Hayvan"} Yaşam Alanı"
    }

    /** 50 → 63 → 79: her adım önceki fiyattan hesaplanır ve yukarı yuvarlanır. */
    fun guncelFiyat(esya: MarketEsyasi, mevcutAdet: Int): Int {
        if (esya.tur != EsyaTuru.HAYVAN) return esya.fiyat
        var sonuc = esya.fiyat
        repeat(mevcutAdet.coerceAtLeast(0)) { sonuc = ceil(sonuc * 1.25).toInt() }
        return sonuc
    }
}
