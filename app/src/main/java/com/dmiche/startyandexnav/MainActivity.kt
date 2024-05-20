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


class MainActivity : ComponentActivity() {
    private var selectedApplication: String? = null
    private var selectedDisplay: Int = 0

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displays = (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).displays
        val installedApps = getInstalledApps(this@MainActivity)

        setContent {
            val listState = rememberLazyListState()
            var selectedIndex by remember { mutableIntStateOf(displays.size - 1) }
            selectedDisplay = selectedIndex
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
                                                selectedDisplay = selectedIndex
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
                                    startApplication(
                                        selectedIndex, app.applicationInfo.packageName
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

    private fun startApplication(index: Int, packageName: String = "ru.yandex.yandexnavi") {
        selectedApplication = packageName
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
                val displays =
                    (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).displays
                val options = ActivityOptions.makeBasic()
                val text = displays.map { "${it.displayId}, ${it.name}" }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                options.launchDisplayId = displays.getOrNull(index)?.displayId ?: return
                startActivity(intent, options.toBundle())
                Toast.makeText(this, text.getOrNull(index) ?: "", Toast.LENGTH_SHORT).show()
                finish()
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
}
