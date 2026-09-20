package com.dersfidan.app.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PhotoUtils {
    fun yeniFotoUri(context: Context): Pair<File, Uri> {
        val dizin = File(context.filesDir, "notlar").apply { mkdirs() }
        val zaman = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dosya = File(dizin, "not_$zaman.jpg")
        val uri = FileProvider.getUriForFile(context, "com.dersfidan.app.fileprovider", dosya)
        return dosya to uri
    }

    fun yeniProfilFotoUri(context: Context): Pair<File, Uri> {
        val dizin = File(context.filesDir, "profil").apply { mkdirs() }
        val dosya = File(dizin, "profil_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "com.dersfidan.app.fileprovider", dosya)
        return dosya to uri
    }
}
