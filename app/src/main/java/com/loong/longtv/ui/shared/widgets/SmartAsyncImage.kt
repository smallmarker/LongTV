package com.loong.longtv.ui.shared.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.HttpException
import com.loong.longtv.engine.source.model.PosterImage
import com.loong.longtv.utils.extensions.createPosterImageRequest
import com.loong.longtv.utils.extensions.provideSmartImageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import android.util.Log
import com.loong.longtv.R
import java.net.ConnectException
import java.net.SocketException

/**
 * @author zl
 * @Date 2025/10/15
 **/
@Composable
fun SmartAsyncImage(
    model: Any,
    modifier: Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val imageLoader = remember { provideSmartImageLoader(context) }
    val poster = when (model) {
        is PosterImage -> model.copy(url = model.url.trim())
        is String -> PosterImage(url = model.trim())
        else -> PosterImage(url = model.toString().trim())
    }
    val shouldFallback = poster.url.isBlank()
    var requestUrl by remember(poster.url) { mutableStateOf(poster.url) }
    var retryAttempt by remember(poster.url, poster.headers) { mutableIntStateOf(0) }
    var showFallback by remember(poster.url, poster.headers) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val requestPoster = remember(poster, requestUrl) { poster.copy(url = requestUrl) }
    val imageRequest = remember(requestPoster, retryAttempt) {
        createPosterImageRequest(context, requestPoster, retryAttempt)
    }

    if (shouldFallback) {
        Box(
            modifier = modifier
        ) {
            PosterPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            // fillMaxSize participates in Box measurement. matchParentSize does not, which would
            // collapse this wrapper to zero height when the caller only constrains its width.
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onLoading = {
                showFallback = false
                Log.d(TAG, "Loading poster ${requestPoster.logTarget()} (attempt=${retryAttempt + 1})")
            },
            onSuccess = { state ->
                showFallback = false
                Log.d(TAG, "Poster loaded ${requestPoster.logTarget()} (source=${state.result.dataSource})")
            },
            onError = { state ->
                val error = state.result.throwable
                val statusCode = (error as? HttpException)?.response?.code
                Log.w(
                    TAG,
                    "Poster failed ${requestPoster.logTarget()} (attempt=${retryAttempt + 1}, " +
                        "error=${error.javaClass.simpleName}" +
                        (statusCode?.let { ", httpStatus=$it" } ?: "") + ")"
                )
                // 4xx is a deterministic request rejection; retrying it only creates needless
                // traffic. Network errors and 5xx responses still receive one delayed retry.
                val shouldRetry = statusCode == null || statusCode >= 500
                showFallback = !shouldRetry || retryAttempt >= MAX_RETRY_ATTEMPTS
                if (shouldRetry && retryAttempt < MAX_RETRY_ATTEMPTS) {
                    coroutineScope.launch {
                        delay(RETRY_DELAY_MS)
                        if (error is SocketException || error is ConnectException) {
                            requestUrl = requestUrl.withAlternateHttpScheme()
                        }
                        retryAttempt++
                    }
                }
            }
        )
        if (showFallback) {
            PosterPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PosterPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_poster_placeholder),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(0.58f)
                .padding(6.dp),
            contentScale = ContentScale.Fit
        )
    }
}

private const val TAG = "SmartAsyncImage"
private const val MAX_RETRY_ATTEMPTS = 1
private const val RETRY_DELAY_MS = 600L

private fun PosterImage.logTarget(): String = Uri.parse(url).run {
    "scheme=${scheme.orEmpty()}, host=${host.orEmpty()}, path=${path.orEmpty()}"
}

private fun String.withAlternateHttpScheme(): String = when {
    startsWith("https://", ignoreCase = true) -> replaceFirst("https://", "http://")
    startsWith("http://", ignoreCase = true) -> replaceFirst("http://", "https://")
    else -> this
}
