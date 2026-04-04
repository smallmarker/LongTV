package com.loong.longtv.ui.tv.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.loong.longtv.ui.shared.settings.SettingsViewModel
import com.loong.longtv.ui.shared.settings.SettingsContent
import com.loong.longtv.ui.shared.widgets.AppScreen
import com.loong.longtv.ui.shared.widgets.TvSurface
import com.loong.longtv.ui.theme.AppCardOverlay
import com.loong.longtv.ui.theme.AppSurface
import com.loong.longtv.ui.theme.AppTextPrimary
import com.loong.longtv.ui.theme.BrandPrimary

@Composable
fun SettingsTvScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppScreen {
        SettingsTvTopBar(
            onBack = { navController.popBackStack() }
        )
        SettingsContent(
            state = state,
            compact = false,
            phoneLayout = false,
            onDefault = viewModel::setDefaultSite,
            onToggle = viewModel::toggleSite
        )
    }
}

@Composable
private fun SettingsTvTopBar(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TvSurface(
            onClick = onBack,
            modifier = Modifier.width(116.dp),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AppCardOverlay.copy(alpha = 0.86f),
                focusedContainerColor = BrandPrimary.copy(alpha = 0.16f),
                pressedContainerColor = BrandPrimary.copy(alpha = 0.10f),
                disabledContainerColor = AppSurface.copy(alpha = 0.45f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "返回",
                    color = AppTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "播放设置",
            color = AppTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(116.dp))
    }
}
