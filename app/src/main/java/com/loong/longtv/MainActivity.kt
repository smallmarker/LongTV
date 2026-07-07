package com.loong.longtv

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.loong.longtv.nav.AppNavGraph
import com.loong.longtv.ui.adaptive.AutoSizeLayout
import com.loong.longtv.ui.common.DeviceClass
import com.loong.longtv.ui.common.currentDeviceClass
import com.loong.longtv.ui.theme.LongTVAppBackground
import com.loong.longtv.ui.theme.LongTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyDeviceOrientation()
        applyImmersiveSystemBars()
        setMainContent()
    }

    private fun setMainContent() {
        setContent {
            LongTVTheme {
                AutoSizeLayout {
                    LongTVAppBackground(modifier = Modifier.fillMaxSize()) {
                        AppNavGraph()
                    }
                }
            }
        }
    }

    private fun applyDeviceOrientation() {
        requestedOrientation = when (applicationContext.currentDeviceClass()) {
            DeviceClass.PHONE -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            DeviceClass.TV -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LongTVTheme {
        AutoSizeLayout {
            AppNavGraph()
        }
    }
}
