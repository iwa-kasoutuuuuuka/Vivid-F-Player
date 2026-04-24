# 技術仕様書 / Technical Specification

## 1. アプリ概要 / App Overview
Vivid F Playerは、キャンプ場などのオフグリッド環境での動画視聴に特化したAndroid用ビデオプレイヤーです。
Vivid F Player is an Android video player specialized for watching videos in off-grid environments like campsites.

## 2. 主要技術スタック / Key Tech Stack
- **Language**: Kotlin
- **Media Engine**: ExoPlayer (Media3)
- **UI Architecture**: MVVM (ViewModel, LiveData/Flow)
- **View Binding**: DataBinding / ViewBinding
- **Service**: Foreground Service for Background Playback

## 3. バックグラウンド再生の安定化実装 / Background Playback Stability Implementation

### 3.1 CPUスリープ防止 / CPU Sleep Prevention
`PlayerHolder.kt` にて、再生中にCPUがスリープしないよう `WAKE_MODE_LOCAL` を設定しています。
In `PlayerHolder.kt`, `WAKE_MODE_LOCAL` is set to prevent the CPU from sleeping during playback.
```kotlin
setWakeMode(C.WAKE_MODE_LOCAL)
```

### 3.2 サービス生存性の向上 / Improved Service Persistence
`MediaSession` に `SessionActivity` を紐付けることで、OSがサービスを重要なものとして認識し、メモリ不足時に終了されにくくしています。
By linking `SessionActivity` to `MediaSession`, the OS recognizes the service as important, making it less likely to be terminated during low memory.
```kotlin
val sessionIntent = Intent(context, PlayerActivity::class.java)
val pendingIntent = PendingIntent.getActivity(...)
mediaSession = MediaSession.Builder(context, player)
    .setSessionActivity(pendingIntent)
    .build()
```

### 3.3 画面消灯の制御 / Screen Timeout Control
`PlayerActivity.kt` にて、再生中はシステムによる自動消灯を無効化しています。
In `PlayerActivity.kt`, automatic screen timeout by the system is disabled during playback.
```kotlin
binding.playerView.keepScreenOn = true
```

## 4. 端末側の推奨設定 / Recommended Device Settings

OSレベルでのアプリ終了を防ぐため、以下の設定を推奨します。
The following settings are recommended to prevent the OS from killing the app.

1. **バッテリー最適化の解除 / Disable Battery Optimization**:
   「制限なし」または「最適化しない」に設定。 / Set to "Unrestricted" or "Don't optimize".
2. **バックグラウンド動作の許可 / Allow Background Activity**:
   メーカー独自の省電力機能（STAMINAモード等）の対象外に設定。 / Exclude from manufacturer-specific power-saving features.

## 5. UI/UX デザイン / UI/UX Design
- **Color Theme**: Deep Black (#000000) for Player background.
- **Visual Effects**: Glassmorphism for Bottom Sheets.
- **Gestures**: Horizontal swipe for seeking, Vertical swipe for volume/brightness.
