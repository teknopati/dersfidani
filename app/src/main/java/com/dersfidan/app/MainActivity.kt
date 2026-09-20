package com.dersfidan.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dersfidan.app.data.ProfilPrefs
import com.dersfidan.app.data.BulutHesap
import com.dersfidan.app.audio.SesYoneticisi
import com.dersfidan.app.ui.*
import com.dersfidan.app.ui.theme.DersFidanTheme
import com.dersfidan.app.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val bildirimIzniLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sonuç önemli değil, kullanıcı reddederse ana ekran yine açılır */ }

    private var dokunmaX = 0f
    private var dokunmaY = 0f
    private var dokunmaZamani = 0L
    private var suruklendi = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dokunmaX = event.x
                dokunmaY = event.y
                dokunmaZamani = event.eventTime
                suruklendi = false
            }
            MotionEvent.ACTION_MOVE -> {
                val esik = ViewConfiguration.get(this).scaledTouchSlop
                if (kotlin.math.abs(event.x - dokunmaX) > esik || kotlin.math.abs(event.y - dokunmaY) > esik) {
                    suruklendi = true
                }
            }
            MotionEvent.ACTION_UP -> if (!suruklendi && event.eventTime - dokunmaZamani < 700L) {
                SesYoneticisi.butonSesiGecikmeli()
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        SesYoneticisi.uygulamaOnPlanda()
    }

    override fun onPause() {
        SesYoneticisi.uygulamaArkaPlanda()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bildirimIzniLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                } catch (e: Exception) { /* bazı cihazlarda bu ekran yok, sorun değil */ }
            }
        }

        setContent {
            DersFidanTheme {
                UygulamaKoku(vm)
            }
        }
    }
}

@Composable
fun UygulamaKoku(vm: AppViewModel) {
    val context = LocalContext.current
    // Hem yerel kurulum tamamlanmış HEM de bulut oturumu hâlâ açık olmalı; uygulama silinince
    // (veya çıkış yapılınca) ikisi de sıfırlanır ve tekrar Giriş/Kayıt ekranı gösterilir.
    var kurulumTamam by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        kurulumTamam = when {
            !BulutHesap.girisliMi() -> false
            ProfilPrefs.kurulumTamamMi(context) -> true
            else -> try {
                val bulutProfil = vm.bulutGirisSonrasiYukle()
                val varsayilanAd = BulutHesap.mevcutEposta()?.substringBefore('@')?.takeIf { it.isNotBlank() } ?: "Öğrenci"
                ProfilPrefs.kaydet(
                    context,
                    bulutProfil?.isim?.takeIf { it.isNotBlank() } ?: varsayilanAd,
                    bulutProfil?.avatar?.takeIf { it.isNotBlank() } ?: ProfilPrefs.avatarSecenekleri.first()
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.cikisYapildiOlayi.collect { kurulumTamam = false }
    }

    when (kurulumTamam) {
    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    false -> {
        GirisEkrani(
            onBulutRestoreDene = { vm.bulutGirisSonrasiYukle() },
            onGirisYapildi = { isim, avatar ->
                ProfilPrefs.kaydet(context, isim, avatar)
                vm.profilYedekle()
                kurulumTamam = true
            }
        )
    }
    true -> AnaEkran(vm)
    }
}

@Composable
fun AnaEkran(vm: AppViewModel) {
    val navController = rememberNavController()
    var kutlamaGoster by remember { mutableStateOf(false) }
    var kutlamaPuan by remember { mutableStateOf(0) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val mevcutRota = backStackEntry?.destination
    val rota = mevcutRota?.route
    val anaRotalar = setOf(Ekran.Bugun.route, Ekran.Gecmis.route, Ekran.Ciftlik.route, Ekran.Dersler.route, Ekran.Profil.route)

    fun sekmeyeGit(hedef: String) {
        if (rota == hedef) vm.sekmeyeTekrarBasildi(hedef)
        else navController.navigate(hedef) { populateArgs(navController) }
    }

    // Telefonun "geri" tuşu: Ders Detayı'ndaysa Dersler listesine döner;
    // diğer sekmelerdeyse (Bugün hariç) uygulamadan çıkmak yerine Bugün
    // sekmesine döner; zaten Bugün'deyse varsayılan davranış (çıkış) kalır.
    BackHandler(enabled = mevcutRota?.route != Ekran.Bugun.route) {
        if (rota == Ekran.DersDetay.route || rota == Ekran.Gun.route) {
            navController.popBackStack()
        } else {
            navController.navigate(Ekran.Bugun.route) { populateArgs(navController) }
        }
    }

    LaunchedEffect(Unit) {
        vm.gunTamamlandiOlayi.collect { bonus ->
            kutlamaPuan = bonus
            kutlamaGoster = true
            SesYoneticisi.alkisCal()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (rota in anaRotalar) {
                val ciftlikMenusu = mevcutRota?.hierarchy?.any { it.route == Ekran.Ciftlik.route } == true
                val altMenuRenkleri = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (ciftlikMenusu) Color(0xFF3E2718) else MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = if (ciftlikMenusu) Color(0xFFFFF0CE) else MaterialTheme.colorScheme.onSurface,
                    indicatorColor = if (ciftlikMenusu) Color(0xFF69C43F) else MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = if (ciftlikMenusu) Color(0xFFFFE7B8) else MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = if (ciftlikMenusu) Color(0xFFFFD69A) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    tonalElevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    color = if (ciftlikMenusu) Color(0xFF744124) else MaterialTheme.colorScheme.surface,
                    border = if (ciftlikMenusu) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD9A15C)) else null
                ) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    ) {
                        NavigationBarItem(
                            selected = mevcutRota?.hierarchy?.any { it.route == Ekran.Bugun.route } == true,
                            onClick = { sekmeyeGit(Ekran.Bugun.route) },
                            icon = { Icon(Icons.Default.Today, contentDescription = null) },
                            label = { AltMenuEtiketi(Ekran.Bugun.etiket, ciftlikMenusu) }, colors = altMenuRenkleri
                        )
                        NavigationBarItem(
                            selected = mevcutRota?.hierarchy?.any { it.route == Ekran.Gecmis.route } == true,
                            onClick = { sekmeyeGit(Ekran.Gecmis.route) },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            label = { AltMenuEtiketi(Ekran.Gecmis.etiket, ciftlikMenusu) }, colors = altMenuRenkleri
                        )
                        NavigationBarItem(
                            selected = mevcutRota?.hierarchy?.any { it.route == Ekran.Ciftlik.route } == true,
                            onClick = { sekmeyeGit(Ekran.Ciftlik.route) },
                            icon = { Icon(Icons.Default.Park, contentDescription = null) },
                            label = { AltMenuEtiketi(Ekran.Ciftlik.etiket, ciftlikMenusu) }, colors = altMenuRenkleri
                        )
                        NavigationBarItem(
                            selected = mevcutRota?.hierarchy?.any { it.route == Ekran.Dersler.route } == true,
                            onClick = { sekmeyeGit(Ekran.Dersler.route) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            label = { AltMenuEtiketi(Ekran.Dersler.etiket, ciftlikMenusu) }, colors = altMenuRenkleri
                        )
                        NavigationBarItem(
                            selected = mevcutRota?.hierarchy?.any { it.route == Ekran.Profil.route } == true,
                            onClick = { sekmeyeGit(Ekran.Profil.route) },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { AltMenuEtiketi(Ekran.Profil.etiket, ciftlikMenusu) }, colors = altMenuRenkleri
                        )
                    }
                }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Ekran.Bugun.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Ekran.Bugun.route) { BugunScreen(vm) }
                composable(Ekran.Gecmis.route) {
                    GecmisScreen(vm) { tarih -> navController.navigate(Ekran.Gun.rota(tarih)) }
                }
                composable(Ekran.Ciftlik.route) { CiftlikScreen(vm) }
                composable(Ekran.Dersler.route) {
                    DerslerScreen(vm) { navController.navigate(Ekran.DersDetay.route) }
                }
                composable(Ekran.Profil.route) {
                    ProfilScreen(vm)
                }
                composable(Ekran.DersDetay.route) {
                    DersDetayScreen(vm) { navController.popBackStack() }
                }
                composable(
                    Ekran.Gun.route,
                    arguments = listOf(navArgument("tarih") { type = NavType.StringType })
                ) { giris ->
                    val tarih = giris.arguments?.getString("tarih") ?: vm.bugununTarihi
                    BugunScreen(vm, tarihOverride = tarih) { navController.popBackStack() }
                }
            }
        }

        GunTamamlandiKutlama(
            goster = kutlamaGoster,
            bonusPuan = kutlamaPuan,
            onKapat = { kutlamaGoster = false }
        )
    }
}

@Composable
private fun AltMenuEtiketi(metin: String, ciftlikTemasi: Boolean) {
    Text(
        text = metin,
        maxLines = 1,
        fontSize = if (ciftlikTemasi) 9.sp else 12.sp,
        fontWeight = if (ciftlikTemasi) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
    )
}

private fun NavOptionsBuilder.populateArgs(navController: NavHostController) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
