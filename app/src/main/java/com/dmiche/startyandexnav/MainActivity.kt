@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.dmiche.startyandexnav

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.dmiche.startyandexnav.ui.theme.StartYandexNavTheme

data class AppDisplayQueue(val packageName: String, val display: Int)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displays = (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).displays
        val installedApps = getInstalledApps(this@MainActivity)

        setContent {
            val listState = rememberLazyListState()
            var selectedIndex by remember { mutableIntStateOf(displays.size - 1) }

            getFromSP(KEY_SCREEN, -1).takeIf { it >= 0 }?.let { value ->
                selectedIndex = value
            } ?: saveToSP(KEY_SCREEN, selectedIndex)
            StartYandexNavTheme {
                Column {
                    LazyRow(state = listState) {
                        for (i in displays.indices) {
                            item {
                                TextButton({ }) {
                                    Text(
                                        if (i == selectedIndex) "Display $i ✓" else "Display $i",
                                        style = TextStyle(fontWeight = FontWeight.Bold),
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .selectable(selected = i == selectedIndex, onClick = {
                                                selectedIndex = i
                                                saveToSP(KEY_SCREEN, selectedIndex)
                                            })
                                    )
                                }
                            }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp)
                    ) {
                        items(installedApps) { app ->
                            val title = app.label.toString()
                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .height(96.dp)
                                    .width(96.dp),
                                onClick = {
                                    saveToSP(KEY_APP, app.applicationInfo.packageName)
                                    startAppsChain(
                                        listOf(
                                            AppDisplayQueue(
                                                app.applicationInfo.packageName,
                                                selectedIndex
                                            ),
                                            CAR_LAUNCHER
                                        )
                                    )
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier.padding(2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Image(
                                            bitmap = app.getIcon(resources.configuration.densityDpi)
                                                .toBitmap().asImageBitmap(),
                                            contentDescription = title,
                                            modifier = Modifier
                                                .height(32.dp)
                                                .width(32.dp)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            title,
                                            style = TextStyle(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getFromSP(KEY_SCREEN, -1).takeIf { it >= 0 }?.let { screen ->
            getFromSP(KEY_APP, null)?.let { app ->
                startAppsChain(
                    listOf(
                        AppDisplayQueue(
                            app, screen
                        ),
                        CAR_LAUNCHER
                    )
                )
            }
        }
    }

    private fun saveToSP(key: String, value: String) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun saveToSP(key: String, value: Int) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun getFromSP(key: String, default_value: Int = -1): Int {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return default_value
        with(sharedPref) {
            return getInt(key, default_value)
        }
    }

    private fun getFromSP(key: String, default_value: String? = null): String? {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return default_value
        with(sharedPref) {
            return getString(key, default_value)
        }
    }

    private fun startApplication(index: Int, packageName: String = "ru.yandex.yandexnavi") {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
                val displays =
                    (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).displays
                val options = ActivityOptions.makeBasic()
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                options.launchDisplayId = displays.getOrNull(index)?.displayId ?: return
                startActivity(intent, options.toBundle())
            }
        } catch (ex: Exception) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun getInstalledApps(context: Context): List<LauncherActivityInfo> {
        val result: ArrayList<LauncherActivityInfo> = arrayListOf()
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        for (profile in userManager.userProfiles) {
            for (app in launcherApps.getActivityList(null, profile)) {
                result.add(app)
            }
        }

        return result
    }

    private fun startAppsChain(list: List<AppDisplayQueue>, finishOnEnd: Boolean = true) {
        list.forEach { (packageName, screen) ->
            startApplication(screen, packageName)
        }

        if (finishOnEnd) finish()
    }

    companion object {
        private const val KEY_SCREEN = "KEY_SCREEN"
        private const val KEY_APP = "KEY_APP"
        private val CAR_LAUNCHER = AppDisplayQueue("com.desaysv.launcher", 0)
    }
}
