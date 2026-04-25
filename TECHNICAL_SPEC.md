# 技術仕様書 (Technical Specifications) - v1.2.7

## 1. アプリ概要 / App Overview
Vivid F Playerは、キャンプ場などのオフグリッド環境での動画視聴に特化したAndroid用ビデオプレイヤーです。
Vivid F Player is an Android video player specialized for watching videos in off-grid environments like campsites.

## 2. 主要技術スタック / Key Tech Stack
- **Language**: Kotlin
- **Media Engine**: ExoPlayer (Media3)
- **UI Architecture**: MVVM (ViewModel, LiveData/Flow)
- **View Binding**: DataBinding

## バージョン履歴 / Version History

### v1.2.7 (2026-04-25)
- **YouTube風操作の追加 / YouTube-style Gestures**: 画面長押しで2倍速、ダブルタップで10秒スキップ機能を実装。
- **視覚的フィードバック / Visual Feedback**: 倍速インジケーターとスキップアイコンのオーバーレイ表示。
- **コア・デバッグ / Core Debugging**: SMB再生時のDataSource移譲不備の修正とUIバグの解消。

### v1.2.6 (2026-04-25)
- **ネットワーク再生の追加 / Added Network Playback (SMB)**: `jcifs-ng`を統合し、NASや共有フォルダからの再生に対応。
- **ハードウェア・デコード最適化 / Hardware Decoding Optimization**: 高ビットレート動画向けにデコーダ設定とバッファ制御を調整。
- **UI改善 / UI Improvements**: SMBフォルダ追加用のダイアログとアイコンを追加。

### v1.2.5 (2026-04-23)
- **安定性の向上 / Stability Improvements**: Xiaomi端末等でのバックグラウンド再生とスリープ時の安定性を強化。
- **APIの現代化 / API Modernization**: Android 13 (SDK 33) 以降のParcelable取得処理を最適化。
- **ビルドの自動化 / Build Automation**: GitHubへの自動デプロイ・スクリプトを更新。

---

## 技術的詳細 / Technical Details

### 1. ネットワーク再生 (SMB) / Network Playback (SMB)
- **ライブラリ / Library**: `com.github.codelibs:jcifs-ng:2.1.31`
- **実装 / Implementation**: 
    - `SmbVideoRepository`: SMB共有内のファイルをリストアップ。
    - `SmbDataSource`: ExoPlayer(Media3)でSMBプロトコルを直接ストリーミングするためのカスタムデータソース。
    - `CompositeVideoRepository`: URIスキーム(`content://` vs `smb://`)に基づいてリポジトリを切り替え。

### 2. ハードウェア最適化 / Hardware Optimization
- **デコーダ / Decoder**: `DefaultRenderersFactory` で `EXTENSION_RENDERER_MODE_ON` を設定し、ハードウェアデコーダを優先。
- **バッファ制御 / Buffer Control**: `DefaultLoadControl` をカスタマイズし、最小30秒、最大60秒のバッファを確保。ネットワーク遅延や高負荷時の再生を安定化。

### 3. 電源管理 / Power Management
- `WAKE_MODE_LOCAL` および `FLAG_KEEP_SCREEN_ON` を使用し、動画再生中の画面消灯を防止。
- `PlaybackService` で `WakeLock` を適切に保持。

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
- **Gestures**:
    - **Vertical swipe**: Left side for brightness, Right side for volume.
    - **Double tap**: 10s skip backward (left) or forward (right).
    - **Long press**: 2.0x playback speed while holding.
- **Feedback**: Overlay indicators for speed, volume, and brightness.
