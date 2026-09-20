package com.dersfidan.app.data

import android.content.Context

/** Hesap değişimini saptamak için profil verisinden ayrı tutulan son UID. */
object HesapPrefs {
    private const val DOSYA = "hesap_prefs"
    private const val SON_UID = "son_uid"
    private fun prefs(context: Context) = context.getSharedPreferences(DOSYA, Context.MODE_PRIVATE)
    fun sonUid(context: Context): String? = prefs(context).getString(SON_UID, null)
    fun uidKaydet(context: Context, uid: String) = prefs(context).edit().putString(SON_UID, uid).apply()
}
