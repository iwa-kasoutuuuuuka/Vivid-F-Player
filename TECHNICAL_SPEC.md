# 技術仕様書 (Technical Specifications) - v1.2.5

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
val pendingIntent = PendingIntent.getActivity(context, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
mediaSession = MediaSession.Builder(context, player)
    .setSessionActivity(pendingIntent)
    .build()
```

### 3.4 画面スリープ・減光の防止 / Prevention of Screen Sleep and Dimming
`PlayerActivity.kt` にて、再生中はシステムによる自動消灯および減光を強力に防止するため、ViewフラグとWindowフラグの両方を設定しています。
In `PlayerActivity.kt`, both View flags and Window flags are set to strongly prevent automatic screen timeout and dimming by the system during playback.
```kotlin
// Viewレベルでの設定 / View-level setting
binding.playerView.keepScreenOn = true
// Windowレベルでの設定 / Window-level setting
window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
```

## 4. 端末側の推奨設定 / Recommended Device Settings

OSレベルでの強力な省電力機能によるアプリ終了や画面消灯を防ぐため、以下の設定を推奨します。特にXiaomi/MIUI端末では重要です。
The following settings are recommended to prevent the OS from killing the app or turning off the screen due to aggressive power-saving features, especially on Xiaomi/MIUI devices.

1. **バッテリー最適化の解除 / Disable Battery Optimization**:
   「制限なし」または「最適化しない」に設定。 / Set to "Unrestricted" or "Don't optimize".
   *   設定 → アプリ → アプリを管理 → Vivid F Player → バッテリーセーバー → 制限なし
   *   Settings -> Apps -> Manage apps -> Vivid F Player -> Battery saver -> No restrictions

2. **自動起動の許可 / Allow Autostart**:
   設定 → アプリ → アプリを管理 → Vivid F Player → 自動起動 をON。
   Settings -> Apps -> Manage apps -> Vivid F Player -> Autostart -> Toggle ON.

3. **アプリのロック / Lock the App**:
   最近のアプリ画面でアプリを長押し、または下スワイプして「鍵アイコン」をタップ。
   In the Recent Apps screen, long-press or swipe down on the app and tap the "Lock icon".

4. **MIUI最適化のオフ / Turn off MIUI Optimization (Advanced)**:
   開発者オプション内の「MIUI最適化をオンにする」をオフに設定。
   Turn off "Turn on MIUI optimization" in Developer options.

5. **ディスプレイ設定 / Display Settings**:
   設定 → ロック画面 → スリープ を「なし」または長時間に設定。
   Settings -> Lock screen -> Sleep -> Set to "Never" or a long duration.

## 5. UI/UX デザイン / UI/UX Design
- **Color Theme**: Deep Black (#000000) for Player background.
- **Visual Effects**: Glassmorphism for Bottom Sheets.
- **Gestures**: Horizontal swipe for seeking, Vertical swipe for volume/brightness.
