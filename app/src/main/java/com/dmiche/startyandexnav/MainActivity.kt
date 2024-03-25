package com.dmiche.startyandexnav

import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.dmiche.startyandexnav.ui.theme.StartYandexNavTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val displays = (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).displays
        setContent {
            StartYandexNavTheme {
                Column(
                    Modifier.fillMaxWidth()
                ) {
                    for (i in 0..displays.size - 1) {
                        Row(
                            Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = {
                                startYandex(i)
                            }, Modifier.fillMaxWidth()) {
                                Text("Display ${i + 1}")
                            }
                        }
                    }
                    for (i in 0..3) {
                        Row(
                            Modifier.fillMaxWidth()
                        ) {
                            Button(onClick = {
                                killService(i)
                            }, Modifier.fillMaxWidth()) {
                                Text("Kill Service ${i + 1}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getAppUid(packageName: String): Int {
        val pidsTask = (getSystemService(ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses
        for (i in pidsTask.indices) {
            if (pidsTask[i].processName == packageName) {
                return pidsTask[i].uid
            }
        }
        return -1
    }

    override fun onStart() {
        super.onStart()
// TODO: uncomment next line for autostart
//        startYandex(1)
    }

    private fun killService(type: Int) {
        when (type) {
            0 -> killApp("com.astrob.turbodog.NaviAIDLService")
            1 -> killApp("com.astrob.turbodog")
            2 -> killService2()
            3 -> killService3()
        }
    }

    private fun killService2() {
        try {
                val intent = Intent("com.astrob.turbodog.NAVI_AIDL_SERVICE")
                intent.setComponent(
                    ComponentName(
                        "com.astrob.turbodog",
                        "com.astrob.turbodog.NaviAIDLService"
                    )
                )
                stopService(intent);
        } catch (ex: Exception) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }

    private fun killService3() {
        try {
            val intent = Intent()
            intent.setComponent(
                ComponentName(
                    "com.astrob.turbodog",
                    "com.astrob.turbodog.NaviAIDLService"
                )
            )
            stopService(intent);
        } catch (ex: Exception) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show()
            ex.printStackTrace()
        }
    }

    private fun killApp(packageName: String) {
        try {
//            Runtime.getRuntime().exec("am force-stop $packageName")
            val pUID = getAppUid(packageName)
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)

            Toast.makeText(this, "pUID: $pUID", Toast.LENGTH_SHORT).show()
            if (pUID >= 0)
                android.os.Process.killProcess(pUID);
        } catch (ex: Exception) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startYandex(index: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent =
                    packageManager.getLaunchIntentForPackage("ru.yandex.yandexnavi") ?: return
                val displays =
                    (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).displays
                val options = ActivityOptions.makeBasic()
                val text = displays.map { "${it.displayId}, ${it.name}" }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                options.launchDisplayId =
                    displays.getOrNull(index)?.displayId ?: return
                startActivity(intent, options.toBundle())
                Toast.makeText(this, text.getOrNull(index) ?: "", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (ex: Exception) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}
