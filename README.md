# <img src="docs/images/app_icon.png" width="48" height="48"> Vivid F Player

モダンなデザインと使いやすさを追求した、Android用ビデオプレイヤーアプリです。
A video player app for Android, designed for modern aesthetics and ease of use.

## バージョン情報 (Version Info)
- **Current Version**: v1.2.19
- **Latest Build**: [`app-debug.apk`](app/build/outputs/apk/debug/app-debug.apk) ([直接ダウンロード / Direct Download](https://github.com/iwa-kasoutuuuuuka/Vivid-F-Player/raw/main/app/build/outputs/apk/debug/app-debug.apk))

Androidの端末内(SDカード含む)のフォルダや、NAS等のSMB共有フォルダを設定して、ファイル名順に動画ファイルを連続再生するだけ。
Simply configure folders within your Android device (including SD cards) or SMB shares (NAS) and play video files continuously in filename order.

電波が悪いキャンプ場で🍺飲みながら垂れ流しで動画を観るアプリ。
An app for "flowing" videos while drinking beer 🍺 at a campsite with poor reception.

複数のフォルダを管理し、ファイル名順での連続再生やレジューム再生に対応しています。
It supports multi-folder management, continuous playback in alphabetical order, and resume playback.

![Main Screen](docs/images/screenshot_main.png)
![Settings Dialog](docs/images/screenshot_settings.png)

## 🚀 主な機能 / Key Features

- **🎬 モダンな再生コントロール / Modern Playback**: YouTube風ジェスチャー（長押し2倍速、ダブルタップスキップ）、画面ロック、ABループ機能。 / YouTube-style gestures, Screen Lock, and AB Loop.
- **📂 マルチフォルダ管理 / Folder Management**: ローカルおよびSMBフォルダを複数登録・管理可能。 / Register multiple local or SMB folders.
- **🌐 SMBストリーミング / SMB Streaming**: NAS等のSMB共有から直接再生。認証情報は Android Keystore で暗号化して保存。 / Play directly from SMB shares; credentials are encrypted with the Android Keystore.
- **📄 字幕サポート / Subtitle Support**: 同一ファイル名の字幕（.srt, .ass, .vtt）の自動読み込みと手動選択。 / Auto-loading and manual selection of subtitles.
- **🌙 Vividデザイン / Vivid Design**: グラスモーフィズムを採用したモダンなUIと、リストでの再生進捗表示。 / Modern glassmorphism UI with progress indicators in lists.
- **🎵 バックグラウンド再生 / Background Play**: 画面オフや他アプリ使用中でも音声再生を継続。 / Continuous audio playback in the background.
- 📺 **ピクチャー・イン・ピクチャー (PiP) / Picture-in-Picture**: 再生中にホームへ戻ると自動で小窓表示（設定でON/OFF）。上部バーのPiPボタンからも移行でき、小窓から前へ/再生・一時停止/次へを操作可能。 / Auto-enters a floating window when you go Home during playback (toggle in Settings), or via the PiP button; the window offers Previous / Play-Pause / Next.
- 🖐️ **ジェスチャーコントロール / Gesture Control**: 明るさ、音量、シークを直感的に操作。 / Intuitively control brightness, volume, and seeking.
- ⚡ **再生速度変更 / Playback Speed Control**: 0.5xから2.0xまで調整可能。 / Adjustable from 0.5x to 2.0x.
- 🔖 **レジューム再生 / Resume Playback**: 続きから再生。 / Resume from where you left off.

## 🎮 操作方法・ジェスチャーガイド / Controls & Gestures

動画再生画面では、直感的なタッチ操作とジェスチャーに対応しています。
The video player screen supports intuitive touch operations and gestures.

| 操作 / Operation | アクション / Action | 説明 / Description |
| :--- | :--- | :--- |
| **画面左側 上下スワイプ / Left Vertical Swipe** | 輝度調整 / Brightness | 画面の明るさを 0% 〜 100% でスムーズに微調整 / Smoothly adjust screen brightness from 0% to 100% |
| **画面右側 上下スワイプ / Right Vertical Swipe** | 音量調整 / Volume | メディア音量を 0% 〜 100% で直感的に調整 / Intuitively adjust media volume from 0% to 100% |
| **左側ダブルタップ / Left Double-Tap** | 10秒巻き戻し / 10s Rewind | 動画を10秒前へシーク / Seek 10 seconds backward |
| **左側トリプルタップ / Left Triple-Tap** | 動画の先頭に戻す / Rewind to Start | 素早く3回タップで動画の先頭（0:00）に即座に戻る / Rapid 3 taps immediately rewinds to the beginning of the video (0:00) |
| **右側ダブルタップ / Right Double-Tap** | 10秒早送り / 10s Forward | 動画を10秒先へシーク / Seek 10 seconds forward |
| **「前へ」ボタン / Previous Button** | 先頭復帰 / 前の動画 / Rewind / Previous | 3秒以上再生中は「動画の先頭」へ戻る。3秒以内または長押しで「前の動画」へ移動 / Seeks to start if playing > 3s; goes to previous video if <= 3s or on long-press |
| **画面長押し / Long Press** | 2.0倍速再生 / 2.0x Fast Forward | 押している間だけ2.0倍速で再生、離すと元の速度に復帰 / Plays at 2.0x while holding, restores original speed on release |
| **画面ロック / Screen Lock** | 誤操作防止 / Lock Touch | コントロールバーの鍵アイコンでジェスチャーとタップ操作をロック / Lock gestures and controls via the lock icon on the top bar |
| **ABループ / AB Loop** | 区間繰り返し / Segment Repeat | A点とB点を指定して特定シーンのみを繰り返しループ再生 / Loop a specific scene by setting A and B points |
| **アスペクト比切替 / Aspect Ratio** | 画面サイズ変更 / Resize | フィット、全画面拡大、固定幅・高さなどの表示比率を順次切り替え / Cycle through Fit, Fill, Zoom, Fixed Width, and Fixed Height |

---

## 🔒 SMB共有の設定とセキュリティ / SMB Setup & Security

自宅のNASやPCの共有フォルダを追加し、Wi-Fi経由で大容量の動画を直接ストリーミング再生できます。
You can add shared folders from your home NAS or PC and stream videos directly over Wi-Fi.

- **接続手順 / How to Connect**:
  1. フォルダ一覧の「SMB共有を追加」をタップ
  2. サーバーIPまたはホスト名（例: `192.168.1.100` または `mynas.local`）を入力
  3. 共有名（例: `video` や `movies`）を入力
  4. 認証が必要な場合はユーザー名・パスワードを入力（ドメイン指定時は `DOMAIN\user` 形式も対応）
- **堅牢な暗号化セキュリティ (v1.2.18以降) / Keystore Encryption**:
  - パスワードはURLなどの平文文字列には一切保存されません。
  - **Android Keystore (AES-256-GCM)** を用いて端末内のセキュアストレージに暗号化保存されます。
  - クラウドバックアップや機種変更時のデータ移行からも自動除外され、認証情報の外部流出を防止します。
- **入力アシスト / Input Sanitization**:
  - 日本語IME（全角・かな入力）によるアドレスの誤入力を自動でNFKC正規化・フィルタリングします。

---

## 📲 APKのインストール方法 / How to Install APK

1. [最新の app-debug.apk](https://github.com/iwa-kasoutuuuuuka/Vivid-F-Player/raw/main/app/build/outputs/apk/debug/app-debug.apk) をAndroid端末でダウンロードします。
2. ダウンロード完了通知またはファイルマネージャーから APK ファイルをタップします。
3. 初回インストール時は「設定」画面が表示されます。「この提供元のアプリを許可」を有効にしてください。
4. 「インストール」をタップすれば完了です。

---

## 📁 フォルダ構成 / Directory Structure

```text
Vivid-F-Player/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/videoplayer/
│   │   │   │   ├── data/          # データモデル、リポジトリ、履歴管理 / Data models, repositories, history
│   │   │   │   ├── player/        # ExoPlayerの管理、バックグラウンド再生サービス / ExoPlayer management, background service
│   │   │   │   ├── ui/            # アクティビティ、フラグメント、UIコンポーネント / Activities, fragments, UI components
│   │   │   │   └── util/          # 自然順ソートなどのユーティリティ / Utilities like natural sort
│   │   │   └── res/               # レイアウト、アイコン、テーマ設定 / Layouts, icons, themes
│   └── build.gradle.kts           # モジュールレベルのビルド設定 / Module-level build config
├── build.gradle.kts               # プロジェクトレベルのビルド設定 / Project-level build config
└── settings.gradle.kts            # プロジェクト設定 / Project settings
```

## 🛠 セットアップとビルド / Setup and Build

1. Android Studio を開き、本プロジェクトをインポートします。 / Open Android Studio and import this project.
2. プロジェクト同期（Gradle Sync）を完了させます。 / Complete Gradle Sync.
3. `Run` ボタンを押して実機またはエミュレータで実行します。 / Press the `Run` button to execute on a device or emulator.

コマンドラインでビルドする場合（JDK 17 が必要） / To build from the command line (requires JDK 17):

```bash
./gradlew assembleDebug   # Windows: gradlew.bat assembleDebug
```

APK は `app/build/outputs/apk/debug/app-debug.apk` に出力されます。 / The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## 🔋 バックグラウンド再生の安定化について / Background Playback Stability

v1.2.2以降、以下の対策を講じていますが、端末側の設定が必要な場合があります。
Since v1.2.2, the following measures have been implemented, but device-side settings may still be required.

### ソフトウェア側の対策 / Software Measures
- ExoPlayerの `WakeMode (WAKE_MODE_LOCAL)` 設定によるCPUスリープ防止 / CPU sleep prevention via ExoPlayer's `WakeMode`.
- MediaSessionへの `SessionActivity` 設定によるサービス生存性向上 / Improved service persistence via `SessionActivity` in MediaSession.
- フォアグラウンドサービス（`START_STICKY`）による再起動保証 / Restart guarantee via Foreground Service (`START_STICKY`).

### バックグラウンド再生が停止する場合の確認事項 / Troubleshooting Background Playback
Androidのバージョンや端末（Xperia, Samsung, AQUOSなど）によっては、ソフト側の対策だけでは不十分な場合があります。以下の設定を確認してみてください。
Depending on the Android version or device (Xperia, Samsung, AQUOS, etc.), software measures may not be enough. Please check the following settings:

1. **バッテリー最適化の解除 / Disable Battery Optimization** (最も重要 / Most Important):
   端末の「設定」 > 「アプリ」 > 「Vivid F Player」 > 「バッテリー」にて、「制限なし」または「最適化しない」に設定してください。 / Go to "Settings" > "Apps" > "Vivid F Player" > "Battery" and set to "Unrestricted" or "Don't optimize".
   - **さらに / Further**: 「設定」 > 「バッテリー」 > 「アプリのバッテリーセーバー」 > 「Vivid F Player」 > 「制限なし (No restrictions)」を選択。 / "Settings" > "Battery" > "App battery saver" > "Vivid F Player" > "No restrictions".

2. **自動起動の許可 / Allow Autostart**:
   端末の「設定」 > 「アプリ」 > 「権限」 > 「自動起動」にて、本アプリをONにしてください。 / Go to "Settings" > "Apps" > "Permissions" > "Autostart" and turn ON for this app.

3. **Xiaomi特有の設定 / Xiaomi Specific Settings**:
   - **アプリのロック / Lock App**: 最近使用したアプリ画面で本アプリを下にスワイプし、鍵アイコンをタップしてロックしてください。 / Swipe down on the app in the Recents screen and tap the lock icon.
   - **MIUI最適化のオフ / Disable MIUI Optimization**: 開発者オプションにて「MIUI最適化」をOFFにしてください（※上級者向け）。 / Disable "MIUI Optimization" in Developer Options (Advanced users only).

4. **通知の許可 / Allow Notifications**:
   バックグラウンド再生の制御には通知権限が必要です。 / Notification permission is required for background playback control.

## 🔄 更新履歴 / Update History

### v1.2.19 (2026-10-02)
- **「動画の先頭に戻す」機能の追加 / Rewind to Start**:
  * **「前へ」ボタンのスマート化**: 3秒以上再生中に「前へ」ボタンを押すと、動画の先頭（0:00）にシーク。3秒以内または長押しで「前の動画」へ移動。PiP操作にも連動。 / Smart Previous button: rewinds to 0:00 if playing > 3s; goes to previous file if <= 3s or long-pressed. Works in PiP as well.
  * **左側トリプルタップ操作**: 画面左側を素早く3回タップするだけで、一瞬で動画の先頭（0:00）に戻るジェスチャーを追加。 / Added left-side rapid triple-tap gesture to immediately rewind to the beginning (0:00).
  * **インジケーター表示**: 先頭復帰時に画面中央へ「0:00」の視覚的フィードバックを表示。 / Visual indicator feedback ("0:00") when rewinding to start.
  * **最新APK提供 / Latest APK**: 最新ビルド `app-debug.apk` を更新。 / Updated bundled `app-debug.apk`.

### v1.2.18 (2026-09-30)
- **SMB認証情報の暗号化 / Encrypted SMB Credentials**:
  * パスワードをURLに含めて平文保存していた方式を廃止し、Android Keystore (AES-GCM) で暗号化して保存するように変更。 / Credentials are no longer stored in plaintext inside the share URL; they are encrypted with an Android Keystore AES-GCM key.
  * 以前のバージョンで登録したSMBフォルダは初回起動時に自動移行（レジューム情報に残っていたパスワードも除去）。 / Folders registered with older versions are migrated automatically on first launch, including scrubbing passwords from resume data.
  * 暗号化済み認証情報はバックアップ/端末移行の対象外に。 / Encrypted credentials are excluded from backup and device transfer.
- **設定の即時反映 / Settings Sync**:
  * 一覧画面で変更した設定（再生速度・リピート・バックグラウンド再生など）が、開いている再生画面にも即座に反映されるように修正。おやすみタイマーもどの画面から設定しても再生を停止するように。 / Settings changed on the list screen now apply immediately to an open player; the sleep timer stops playback no matter which screen it was set from.
- **バグ修正 / Bug Fixes**:
  * 最後のファイルの再生が終わった状態でアプリを離れるとクラッシュする問題を修正（バックグラウンド再生ON時）。 / Fixed a crash when leaving the app after the last file finished playing with Background Playback enabled.
  * SMBフォルダ追加ダイアログで日本語キーボードの全角/かな入力によりアドレスが壊れる問題を修正。 / Fixed the SMB dialog accepting full-width/kana input from Japanese keyboards, which broke addresses.
- **動作確認 / Verified**: ローカルのSMBサーバーを使い、一覧取得・ストリーミング再生・字幕・連続再生・誤ったパスワード時の動作を確認。 / Tested listing, streaming, subtitles, continuous playback and wrong-password handling against a local SMB server.
- **最新APK提供 / Latest APK**: リポジトリ内に最新ビルド `app-debug.apk` を同梱・更新。 / Bundled latest `app-debug.apk` in the repository for direct download.

### v1.2.17 (2026-09-30)
- **ピクチャー・イン・ピクチャー (PiP) の実装 / Picture-in-Picture Implemented**:
  * 再生中にホームへ戻ると自動でPiPに移行（Android 12以降はスムーズな自動移行）。設定の「ホームでPiP表示」でON/OFF可能。 / Auto-enters PiP when going Home during playback (seamless on Android 12+); toggle via "Picture-in-Picture on Home" in Settings.
  * 上部バーにPiPボタンを追加。PiPウィンドウから前へ/再生・一時停止/次へを操作可能。 / Added a PiP button to the top bar; the PiP window has Previous / Play-Pause / Next controls.
  * PiPウィンドウを閉じた時は、バックグラウンド再生がOFFなら一時停止。 / Closing the PiP window pauses playback unless Background Playback is enabled.
- **ビルドエラーの修正 / Build Fix**:
  * v1.2.14 のアイコン差し替えで残っていた古いリソース参照（`ic_loop`, `ic_aspect_ratio`）を修正し、ビルドできない状態を解消。 / Fixed stale icon references left over from v1.2.14 that prevented the project from compiling.
- **連続再生・リピートの修正 / Continuous Playback & Repeat Fixes**:
  * リピート「ALL」で同じ動画がループし続け、次のファイルへ進まない不具合を修正。フォルダ末尾から先頭へ戻るように。 / Fixed "Repeat ALL" looping the current video forever; it now wraps from the last file back to the first.
  * シャッフル再生がフォルダ内のファイル間で機能するように修正。 / Shuffle now actually picks random files within the folder.
- **バックグラウンド再生の安定化 / Background Playback Stability**:
  * MediaSessionをサービスに登録し、フォアグラウンド通知が確実に表示されるように修正（通知が出ずにサービスが強制終了される問題を解消）。 / Registered the MediaSession with the service so the foreground notification is always posted.
  * 一時停止中にホームへ戻った際のクラッシュを修正。 / Fixed a crash when leaving the app while paused with Background Playback enabled.
  * 戻るボタン/おやすみタイマーで再生画面を閉じた場合は、バックグラウンド再生ONでも再生を停止するように変更。 / Closing the player (Back button / sleep timer) now stops playback even with Background Playback enabled.
  * 設定変更後に再生画面へ戻るとプレイヤーが解放されていて再生できない不具合を修正。 / Fixed the player being released under a still-open player screen.
- **レジューム再生の改善 / Resume Improvements**:
  * 動画の切り替え時にも再生位置を保存し、長時間の連続再生後も正しく「続きから」再生できるように。 / Position is now saved on every track change, so resume works after long continuous sessions.
  * 最後まで視聴した動画は先頭から再生。 / Fully watched videos restart from the beginning.
  * 再生画面から戻った時に、一覧の進捗バーと「前回の続き」カードを更新。 / Progress bars and the "last played" card refresh when returning from the player.
- **UI修正 / UI Fixes**:
  * 再生画面のタイトルがステータスバーに重なり、再生ボタンが画面下で切れる問題を修正。 / Fixed the title overlapping the status bar and the play button being clipped at the bottom.
  * 再生開始直後のコントロールも3秒後に自動で隠れるように。 / Controls shown at launch now auto-hide after 3 seconds.
- **SMB / その他 / SMB & Misc**:
  * パスワード等に `@ : / #` を含むSMB認証情報でURLが壊れる問題を修正。 / Fixed SMB credentials containing `@ : / #` breaking the share URL.
  * Gradle Wrapper を追加し、コミットされていたビルド成果物 (`app/build`) を管理対象から除外。 / Added the Gradle Wrapper and stopped tracking the committed `app/build` output.

### v1.2.16 (2026-05-08)
- **ボタン操作の反応性向上 / Improved Button Responsiveness**:
  * 再生・一時停止、次へ、前へボタンのタッチ有効範囲を大幅に拡大。 / Significantly expanded touch targets for Play/Pause, Next, and Previous buttons.
  * ボタン押下時に「沈み込む」アニメーションを追加し、視覚的なフィードバックを強化。 / Added "sink-in" scale animation on touch for enhanced visual feedback.
  * 誤操作によるコントロール非表示を防ぐため、ボタン周囲の判定を最適化。 / Optimized hit detection around buttons to prevent accidental UI hiding.

### v1.2.15 (2026-05-08)
- **デバッグとクリーンアップ / Debug & Cleanup**:
  * 未使用コードの削除とビルド警告の解消。 / Removed unused code and resolved build warnings.
  * 全体的な動作安定性の向上。 / Improved overall stability.

### v1.2.14 (2026-05-08)
- **「白い四角」不具合の根本解決 / Final Fix for "White Square" Bug**:
  * PNG形式のアイコンを廃止し、Android標準のベクター形式（XML）に完全に差し替え。 / Replaced PNG icons with standard Android Vector Drawables (XML).
  * これにより、一部の端末でアイコンが白い四角形で表示される問題が完全に解消されました。 / This completely resolves the issue where icons appeared as white squares on some devices.
  * ホワイトティントを再適用し、すべてのボタンのデザインを統一。 / Re-applied white tint for a unified design across all buttons.

### v1.2.13 (2026-05-08)
- **ジェスチャー操作の改善 / Gesture Responsiveness Improvements**:
  * 音量と輝度調整の感度を大幅に改善。 / Significantly improved sensitivity for volume and brightness adjustments.
  * スクロール中の計算ロジックを刷新し、指の動きに正確に追従するように修正。 / Refined calculation logic to accurately track finger movement.
  * 画面の端から端までスワイプすることで全範囲を調整可能に。 / Full range adjustment possible by swiping across the screen height.

### v1.2.12 (2026-05-08)
- **アイコン表示の修正 / Icon Visibility Fixes**:
  * 一部のボタン（字幕、ロック、アスペクト比、ABループ）が「白い四角」として表示される不具合を修正。 / Fixed bug where some buttons appeared as "white squares".
  * PNG形式のアイコンに対する不適切なカラーティントの設定を解除しました。 / Removed incorrect color tints from PNG-based icons.

### v1.2.11 (2026-05-08)
- **イマーシブモード（フルスクリーン）の対応 / Immersive Mode Support**:
  * 動画再生中にナビゲーションバーとステータスバーを自動的に非表示にする機能を追加。 / Automatically hide navigation and status bars during playback.
  * 画面端からのスワイプで一時的にシステムバーを表示可能。 / System bars can be shown temporarily by swiping from the edge.
- **UI表示バグの修正 / UI Visibility Fixes**:
  * タップ時にコントロールボタンが表示されない（二重トグル）不具合を修正。 / Fixed bug where control buttons wouldn't show due to double-toggling.
  * 再生・一時停止ボタン等に明示的なホワイトカラーのティントを適用し、視認性を向上。 / Applied explicit white tints to player buttons for better visibility.
  * 画面ロック中にタップしても即座にロック解除されないようロジックを改善。 / Improved lock logic to prevent accidental unlocking.


### v1.2.7 (2026-04-25)
- **YouTube風操作の追加 / YouTube-style Gestures**:
  * 画面長押しで **2.0倍速再生**（指を離すと戻る）。 / Long press for **2.0x playback speed** (restores on release).
  * 左右ダブルタップで **10秒スキップ** (進む/戻る)。 / Double tap on left/right for **10s skip** (forward/backward).
  * 視覚的なフィードバック（倍速インジケーター、スキップアイコン）を追加。 / Added visual feedback (speed indicator, skip icons).
- **SMB 再生ロジックの修正 / SMB Playback Fix**:
  * `SmbDataSource` の移譲不備を修正し、ストリーミングが途切れる問題を解消。 / Fixed delegation in `SmbDataSource` to resolve streaming issues.
- **バグ修正 / Bug Fixes**:
  * `FolderListFragment` の構文エラーを修正。 / Fixed syntax error in `FolderListFragment`.
  * `PlayerActivity` のリポジトリ参照を最新化。 / Updated repository references in `PlayerActivity`.

### v1.2.6 (2026-04-24)
- **SMB対応とハードウェア最適化 / SMB Support & HW Optimization**:
  * `jcifs-ng` による SMBv2/v3 ストリーミング再生に対応。 / Added SMBv2/v3 streaming support via `jcifs-ng`.
  * ハードウェアデコーダーを優先し、高ビットレート動画の再生を安定化。 / Prioritized HW decoders for stable high-bitrate playback.
  * ネットワーク遅延に対応するためバッファサイズを拡張。 / Expanded buffer sizes for network latency.

### v1.2.5
- Xiaomi端末での画面消灯・スリープ対策を強化 (Enforced screen timeout prevention for Xiaomi devices)
- `FLAG_KEEP_SCREEN_ON`をWindowレベルで適用し、再生中の画面消灯を抑制
- ドキュメントの更新（ビルド手順とトラブルシューティング）

### v1.2.4 (2026-04-24)
- **画面消灯・減光の防止強化 / Enhanced Prevention of Screen Timeout & Dimming**:
  - `FLAG_KEEP_SCREEN_ON` をWindowに追加し、Xiaomi端末等での自動減光を抑制。 / Added `FLAG_KEEP_SCREEN_ON` to the Window to prevent automatic dimming on Xiaomi devices.
  - CPU WakeLockの実装により、スリープによる中断を防止。 / Implemented CPU WakeLock to prevent interruption by system sleep.
  - リピート再生・シャッフル再生機能を追加。 / Added Repeat and Shuffle playback modes.
  - おやすみタイマー機能を追加。 / Added Sleep Timer functionality.

### v1.2.3 (2026-04-24)
- **再生継続の改善 / Improved Continuity**:
  - 再生中に画面が自動消灯しないよう `keepScreenOn` を有効化。 / Enabled `keepScreenOn` to prevent automatic screen timeout during playback.
- **ドキュメントの多言語化 / Bilingual Documentation**:
  - READMEと技術仕様書に英語併記を追加。 / Added English translations to README and technical specs.

### v1.2.2 (2026-04-24)
- **バックグラウンド再生の安定化 / Background Stability**:
  - `ExoPlayer` の `WakeMode` 有効化、`MediaSession` の改善など。 / Enabled `WakeMode`, improved `MediaSession`, etc.
- **UI/UX 改善 / UI/UX Improvements**:
  - 再生画面の背景色を黒に固定。 / Fixed player background color to black.

## 📜 ライセンス / License

このプロジェクトは MIT ライセンスの下で公開されています。
This project is licensed under the MIT License.
