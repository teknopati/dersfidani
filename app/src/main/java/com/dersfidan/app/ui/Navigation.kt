package com.dersfidan.app.ui

sealed class Ekran(val route: String, val etiket: String) {
    object Bugun : Ekran("bugun", "Bugün")
    object Gecmis : Ekran("gecmis", "Geçmiş")
    object Ciftlik : Ekran("ciftlik", "Çiftliğim")
    object Dersler : Ekran("dersler", "Derslerim")
    object Profil : Ekran("profil", "Profilim")
    object DersDetay : Ekran("ders_detay", "Ders")
    object Gun : Ekran("gun/{tarih}", "Gün") {
        fun rota(tarih: String) = "gun/$tarih"
    }
}
