package com.dersfidan.app.data

import android.content.Context

/**
 * Tek kullanıcılı yerel profil bilgisi (isim + avatar).
 * Sunucu/hesap gerektirmez; sadece cihazda "giriş/kurulum" hissi verir.
 */
object ProfilPrefs {
    private const val DOSYA = "profil_prefs"
    private const val ANAHTAR_ISIM = "isim"
    private const val ANAHTAR_AVATAR = "avatar"
    private const val ANAHTAR_KURULUM_TAMAM = "kurulum_tamam"
    private const val ANAHTAR_FOTO = "profil_foto"
    private const val ANAHTAR_FOTO_URL = "profil_foto_url"

    /** İlk kurulumda seçilebilecek ve Profilim'den sonradan değiştirilebilecek ortak avatar listesi. */
    val avatarSecenekleri = listOf(
        "🌱", "🌿", "🌻", "🦉", "🐰", "🦋", "🐝", "🦔", "🐦", "🌳",
        "🧑", "👩", "👨", "🧑‍🎓", "👩‍🎓", "👨‍🎓", "🧑‍🌾", "👩‍🌾", "👨‍🌾",
        "🦊", "🐻", "🐼", "🐱", "🐶", "🦁", "🐸", "🐨", "🐯"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)

    fun kurulumTamamMi(context: Context): Boolean =
        prefs(context).getBoolean(ANAHTAR_KURULUM_TAMAM, false)

    fun isim(context: Context): String =
        prefs(context).getString(ANAHTAR_ISIM, "") ?: ""

    fun avatar(context: Context): String =
        prefs(context).getString(ANAHTAR_AVATAR, "🌱") ?: "🌱"

    fun foto(context: Context): String? = prefs(context).getString(ANAHTAR_FOTO, null)
    fun fotoUrl(context: Context): String? = prefs(context).getString(ANAHTAR_FOTO_URL, null)

    fun kaydet(context: Context, isim: String, avatar: String) {
        prefs(context).edit()
            .putString(ANAHTAR_ISIM, isim)
            .putString(ANAHTAR_AVATAR, avatar)
            .putBoolean(ANAHTAR_KURULUM_TAMAM, true)
            .apply()
    }

    /** Profilim ekranından fotoğraf/avatar tek başına değiştirildiğinde. */
    fun avatarKaydet(context: Context, avatar: String) {
        prefs(context).edit().putString(ANAHTAR_AVATAR, avatar).remove(ANAHTAR_FOTO).remove(ANAHTAR_FOTO_URL).apply()
    }

    fun fotoKaydet(context: Context, yerel: String?, bulutUrl: String?) {
        prefs(context).edit().putString(ANAHTAR_FOTO, yerel).putString(ANAHTAR_FOTO_URL, bulutUrl).apply()
    }

    fun cikisYap(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
