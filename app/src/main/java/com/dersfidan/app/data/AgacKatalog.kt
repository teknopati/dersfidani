package com.dersfidan.app.data

/**
 * Resimli galeride tür numarasıyla eşleşen 100 ağaçlık katalog.
 * Aynı tür her cihazda aynı ad ve görünüme sahiptir.
 */
object AgacKatalog {
    val adlar = listOf(
        "Elma Ağacı", "Armut Ağacı", "Kiraz Ağacı", "Vişne Ağacı", "Şeftali Ağacı",
        "Kayısı Ağacı", "Erik Ağacı", "Ayva Ağacı", "Nar Ağacı", "İncir Ağacı",
        "Dut Ağacı", "Ceviz Ağacı", "Badem Ağacı", "Fındık Ağacı", "Kestane Ağacı",
        "Zeytin Ağacı", "Limon Ağacı", "Portakal Ağacı", "Mandalina Ağacı", "Greyfurt Ağacı",
        "Meşe Ağacı", "Çınar Ağacı", "Ihlamur Ağacı", "Akçaağaç", "Dişbudak Ağacı",
        "Kayın Ağacı", "Karaağaç", "Gürgen Ağacı", "Kavak Ağacı", "Söğüt Ağacı",
        "Huş Ağacı", "Kızılağaç", "Sığla Ağacı", "Keçiboynuzu Ağacı", "Defne Ağacı",
        "Manolya Ağacı", "Erguvan Ağacı", "Akasya Ağacı", "Mimoza Ağacı", "Oya Ağacı",
        "Sakura Ağacı", "Jakaranda Ağacı", "Paulownia Ağacı", "Lale Ağacı", "Katalpa Ağacı",
        "Sekoya Ağacı", "Sedir Ağacı", "Göknar Ağacı", "Ladin Ağacı", "Sarıçam",
        "Karaçam", "Fıstık Çamı", "Servi Ağacı", "Ardıç Ağacı", "Mazı Ağacı",
        "Palmiye", "Hurma Ağacı", "Hindistan Cevizi", "Muz Ağacı", "Mango Ağacı",
        "Avokado Ağacı", "Kakao Ağacı", "Kahve Ağacı", "Papaya Ağacı", "Guava Ağacı",
        "Liçi Ağacı", "Ejder Meyvesi Ağacı", "Ekmek Ağacı", "Baobab", "Bambu Ağacı",
        "Mor Salkım Ağacı", "Alev Ağacı", "Şişe Ağacı", "Maymun Çıkmaz Ağacı", "Demir Ağacı",
        "Kâğıt Ağacı", "Tespih Ağacı", "Sabun Ağacı", "Kauçuk Ağacı", "Okaliptüs Ağacı",
        "Ginkgo Ağacı", "Kızılağaç Sekoyası", "Mercan Ağacı", "Fener Ağacı", "Altın Yağmur Ağacı",
        "Kırmızı Akçaağaç", "Şeker Akçaağacı", "Mavi Ladin", "Ağlayan Söğüt", "Sütun Servi",
        "Bodur Elma", "Bodur Kiraz", "Süs Eriği", "Süs Elması", "Süs Armudu",
        "Beyaz Dut", "Kara Dut", "Kan Portakalı", "Kamkat Ağacı", "Yıldız Meyvesi Ağacı"
    )

    fun ad(turId: Int): String = adlar[Math.floorMod(turId, adlar.size)]

    /** Önce sahip olunmayan türlerden deterministik olmayan bir seçim, 100'den sonra tekrar. */
    fun yeniTur(mevcutTurler: Set<Int>): Int {
        val bos = (adlar.indices).filterNot(mevcutTurler::contains)
        return (if (bos.isNotEmpty()) bos else adlar.indices.toList()).random()
    }
}
