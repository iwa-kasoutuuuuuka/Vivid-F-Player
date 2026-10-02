# 技術仕様書 (Technical Specifications) - v1.2.22

## 1. アプリ概要 / App Overview
Vivid F Playerは、キャンプ場などのオフグリッド環境での動画視聴に特化したAndroid用ビデオプレイヤーです。
Vivid F Player is an Android video player specialized for watching videos in off-grid environments like campsites.

## 2. 主要技術スタック / Key Tech Stack
- **Language**: Kotlin
- **Media Engine**: ExoPlayer (Media3)
- **UI Architecture**: MVVM (ViewModel, LiveData/Flow)
- **View Binding**: DataBinding

## バージョン履歴 / Version History

### v1.2.22 (Current)
- **Instant Playback & Buffer Optimization**:
  - `DefaultLoadControl` tuned with `bufferForPlaybackMs = 600` (down from 2500ms) and `bufferForPlaybackAfterRebufferMs = 1500` (down from 5000ms), cutting initial launch latency by ~75%.
  - Added 15-second back-buffer retention (`setBackBuffer(15_000, true)`), allowing instantaneous 0ms rewind on 10s double-tap gestures.
- **Fast ContentResolver Batch Query (LocalVideoRepository)**:
  - Replaced slow iterative `DocumentFile.listFiles()` with a single native `ContentResolver.query()` on `DocumentsContract.buildChildDocumentsUriUsingTree`, accelerating 100+ file folder scans by 10x-30x (seconds down to tens of milliseconds) with safe legacy fallback.
- **In-Memory Subtitle Indexing & Zero-Latency Playlist Switching**:
  - `LocalVideoRepository` and `SmbVideoRepository` build an in-memory subtitle index concurrently during initial folder scan (`getVideoFiles`).
  - `getSubtitleFiles` queries the concurrent in-memory map directly, completely eliminating redundant full directory re-scans and network SMB RTTs during next/previous track changes.
- **Zero-Allocation NaturalOrderComparator**:
  - Replaced `substring`, `takeWhile`, and `BigInteger` instantiations with an in-place index pointer algorithm, eliminating GC allocations during directory sorting and list scrolling.
- **RecyclerView Measure Throttling**:
  - Enabled `setHasFixedSize(true)` and `setItemViewCacheSize(20)` in `FileListFragment` to lock 80dp row heights and eliminate repeated layout passes.

### v1.2.21
- **Settings Stability & RadioGroup Bug Fix**:
  - Explicit IDs assigned to all `RadioButton` components (`rb_speed_*`, `rb_skip_*`, `rb_long_*`) preventing selection desynchronization and `NO_ID` clearing in `SettingsBottomSheet`.
- **Gesture Conflict Prevention**:
  - Multitouch pinch zoom actively isolates single-touch scroll events (`!scaleGestureDetector.isInProgress && event.pointerCount == 1`), eliminating accidental brightness/volume jumps during pinch operations.
- **Pinch Zoom Centering & Video Switch Reset**:
  - Explicitly centers zoom pivot (`pivotX = width / 2f`, `pivotY = height / 2f`).
  - Automatically resets zoom scale to 1.0x on video transition in `playVideo`.
- **AudioFx Voice Boost Lifecycle Hooking**:
  - Automatically re-applies `LoudnessEnhancer` upon `STATE_READY` and `onTracksChanged`, guaranteeing speech boost when audio session ID stabilizes.
- **Pocket Mode Backlight Dimming**:
  - Dims screen brightness to `0.01f` and hides system bars upon entering Pocket Mode, restoring user brightness on exit for true ultra-low battery consumption.
- **Background CPU Throttling**:
  - `statusUpdateHandler` is paused while controls are hidden, eliminating background battery polling during video playback.

### v1.2.20
- **Camping & Power Saving Suite**:
  - **Pocket Mode**: Complete screen blackout overlay (`layout_pocket_mode`) with touch consumption. Allows audio and video playback while walking or in tent/pocket without accidental inputs; single tap wakes up.
  - **Extra Dim (Night Mode)**: 40% opaque dark overlay (`view_night_overlay`) toggled via settings to prevent eye fatigue in dark campsites.
  - **Clock & Battery Monitor**: Real-time display in the top controls bar (`tv_status_info`), polling battery percentage and system time every 10s.
  - **5-Minute Auto-Sleep Protection**: Releases `FLAG_KEEP_SCREEN_ON` 5 minutes after playback pauses to prevent overnight battery drain.
- **Audio Enhancements**:
  - **Voice Boost**: Hardware audio effect `android.media.audiofx.LoudnessEnhancer` targeting speech intelligibility (+8dB gain) over ambient outdoor/bonfire noise. Rebound on track change and properly released on `onDestroy`.
  - **Audio Track Selection**: Top-bar button displaying `TrackSelectionDialog` for `C.TRACK_TYPE_AUDIO`, supporting multi-language and secondary audio tracks.
- **Enhanced Playback Controls & Gestures**:
  - **Pinch Zoom**: `ScaleGestureDetector` seamlessly scales video rendering (`videoSurfaceView` / `exo_content_frame`) from 1.0x to 3.0x without clipping overlays. Double-tapping while zoomed instantly resets scale to 1.0x.
  - **Configurable Double-Tap Skip**: User selectable (5s, 10s, 30s, 60s) stored in `AppSettings` and applied dynamically.
  - **Configurable Fast-Forward Speed**: User selectable (1.5x, 2.0x, 2.5x, 3.0x) on long-press.
- **Video Management**:
  - **Unwatched Badge**: Videos with `pos <= 1000L` display a vibrant "未視聴" (NEW) pill badge in `FileListAdapter`. Once played past 1s, transitions to progress bar or hides upon completion.

### v1.2.19
- **Rewind to Start**:
  - `btnPrevious` incorporates standard 3-second seek rule: if playback position > 3000ms, seeks to 0:00 (`seekTo(0)`) and shows overlay indicator; if <= 3000ms, switches to previous video. Long-press on `btnPrevious` forces previous video switch regardless of position.
  - Left-side triple-tap detection in `setupGestures` allows rapid 3-tap gesture to rewind to 0:00 without delay to double-tap seeking.
  - PiP previous action (`PIP_CONTROL_PREVIOUS`) shares the 3-second smart rewind logic.

### v1.2.18
- **SmbCredentialStore**: SMB URLs never carry user-info. Credentials are stored per `host[:port]/share` in `smb_credentials` prefs, encrypted with an Android Keystore AES-256-GCM key (`vivid_smb_credentials`), and supplied to jcifs through `CIFSContext.withCredentials(NtlmPasswordAuthenticator)`. Domain accounts accept `DOMAIN;user` or `DOMAIN\user`. Legacy credential-bearing URLs in `folder_uris` and `resume_prefs` are migrated on first access. Undecryptable entries (e.g. after restore on another device) are dropped. The prefs file is excluded via `backup_rules.xml` / `data_extraction_rules.xml`.
- **AppSettings**: A process-wide singleton holds the playback settings as `StateFlow`s plus the sleep timer, so every `MainViewModel` instance (one per activity) observes the same values.
- **Foreground service guard**: `PlaybackService` is only started when the player is actually playing or buffering; `playWhenReady` alone stays true in `STATE_ENDED`.

### v1.2.17
- **Picture-in-Picture**: `PlayerActivity` builds `PictureInPictureParams` from the video aspect ratio (clamped to 1:2.39–2.39:1) with Previous / Play-Pause / Next `RemoteAction`s delivered via a non-exported broadcast. Android 12+ uses `setAutoEnterEnabled`; older versions enter from `onUserLeaveHint`. Controlled by the `auto_pip` preference.
- **Playlist logic**: The player holds a single `MediaItem`; repeat-all and shuffle are resolved in `playNext()` / `playPrevious()`. Only `REPEAT_MODE_ONE` is forwarded to ExoPlayer.
- **Background service**: `PlaybackService` calls `addSession()` in `onCreate` so Media3 posts the foreground notification; it is only started while `playWhenReady` is true, and no longer releases the shared player (the activity owns release).
- **Resume**: Position is saved on track change, on `STATE_ENDED`, and in `onPause`; positions within 3s of the end restart from 0.

### v1.2.10
- **Screen Lock**: Added lock button to disable gestures and controls during playback.
- **Aspect Ratio Control**: Cycle through Fit, Fill, Zoom, Fixed Width, and Fixed Height modes.
- **AB Loop**: Set A and B points to loop a specific segment of the video.
- **Subtitle Selection**: Integrated Media3 track selection UI for internal and external subtitles.
- **Improved SMB Support**: Stable credential handling in URIs and concurrent resource loading.
- **Playback Progress UI**: Progress bars in the file list to indicate resume positions.
- **Optimized Lifecycle**: Proper player release in background service and activity.

### v1.2.9
- **Subtitle Support**: Auto-loading of external .srt/.ass/.vtt files.
- **Navigation**: Improved folder navigation and resume logic.

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
