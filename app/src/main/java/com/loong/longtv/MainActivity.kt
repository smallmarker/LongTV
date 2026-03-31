package com.loong.longtv

import com.loong.longtv.ui.home.HomeScreen
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.loong.longtv.ui.theme.LongTVTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 权限请求结果回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // 手机调试时即使拒绝本地媒体权限，也允许继续测试网络流程。
        }
        setMainContent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = buildRequiredPermissions()
        if (permissions.isEmpty()) {
            setMainContent()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun setMainContent() {
        setContent {
            LongTVTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }

    private fun buildRequiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> emptyList()
            else -> listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LongTVTheme {
        AppNavGraph()
    }
}
