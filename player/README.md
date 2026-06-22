# Loong Player

Compose-first Android video player module built on Media3 ExoPlayer.

## Features

- Compose entry point: `ComposeVideoPlayer`
- Media3 playback for common VOD/live formats, including MP4, HLS, DASH, FLV, MKV hints
- Custom request headers and cross-protocol redirects
- Play, pause, toggle, seek forward/back, seek to position
- Playback speed, volume, mute, repeat
- Resize modes for fit, fill, zoom, fixed width, and fixed height
- Optional native Media3 controller
- Surface tap callback for custom Compose control overlays
- Shared player session for detail-to-fullscreen transitions
- Progress, playback state, playing state, controller visibility, and error callbacks

## Basic Usage

```kotlin
val controlState = rememberPlayerControlState()

ComposeVideoPlayer(
    modifier = Modifier.fillMaxSize(),
    videoUrl = url,
    requestHeaders = mapOf("User-Agent" to userAgent),
    controlState = controlState,
    onProgressChanged = { positionMs, durationMs ->
        // Persist watch progress or update custom controls.
    }
)

controlState.togglePlayPause()
controlState.seekForward(10_000L)
controlState.setSpeed(1.25f)
controlState.changeResizeMode(PlayerResizeMode.Zoom)
```

## Module Boundary

This module intentionally has no dependency on `:app` or `:spider`. App-specific UI can wrap
`ComposeVideoPlayer` for phone, TV, or fullscreen behavior while keeping the player core reusable.
