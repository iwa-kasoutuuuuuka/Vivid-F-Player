# <img src="docs/images/app_icon.png" width="48" height="48"> Vivid F Player

モダンなデザインと使いやすさを追求した、Android用ビデオプレイヤーアプリです。
A video player app for Android, designed for modern aesthetics and ease of use.

Androidの端末内(SDカード含む)のフォルダを設定して、ファイル名順に動画ファイルを連続再生するだけ。
Simply configure folders within your Android device (including SD cards) and play video files continuously in filename order.

電波が悪いキャンプ場で🍺飲みながら垂れ流しで動画を観るアプリ。
An app for "flowing" videos while drinking beer 🍺 at a campsite with poor reception.

複数のフォルダを管理し、ファイル名順での連続再生やレジューム再生に対応しています。
It supports multi-folder management, continuous playback in alphabetical order, and resume playback.

![Main Screen](docs/images/screenshot_main.png)
![Settings Dialog](docs/images/screenshot_settings.png)

## 🚀 主な機能 / Key Features

| 機能 / Feature | アイコン / Icon | 説明 / Description |
| :--- | :---: | :--- |
| **マルチフォルダ管理**<br>Multi-Folder Management | <img src="docs/images/folder_icon.png" width="32"> | 複数の動画保存場所を登録・管理。 / Register and manage multiple video storage locations. |
| **自然順連続再生**<br>Natural Sort Playback | <img src="docs/images/video_icon.png" width="32"> | ファイル名の数字を考慮したインテリジェントな連続再生。 / Intelligent continuous playback considering numbers in filenames. |
| **設定 & カスタマイズ**<br>Settings & Customization | <img src="docs/images/settings_icon.png" width="32"> | 再生速度やバックグラウンド再生の設定。 / Configure playback speed and background play. |
| **直感的な操作**<br>Intuitive Controls | <img src="docs/images/play_icon.png" width="24"> <img src="docs/images/pause_icon.png" width="24"> | 鮮やかなボタンとジェスチャーで操作。 / Operate with vivid buttons and gestures. |

- 🔄 **自然順連続再生 / Natural Sort Playback**: ファイル名の数字を考慮したインテリジェントな連続再生。 / Intelligent continuous playback considering numbers in filenames.
- 📺 **ピクチャー・イン・ピクチャー (PiP) / Picture-in-Picture**: 他のアプリを使いながら動画を視聴。 / Watch videos while using other apps.
- 🎧 **バックグラウンド音声再生 / Background Audio Playback**: 画面を閉じても音声のみ継続可能（設定で切替）。 / Continue audio playback even when the screen is closed (configurable).
- 🎨 **Vivid & Premium UI**: グラスモーフィズムと鮮やかなグラデーションを採用したモダンなデザイン。 / Modern design using glassmorphism and vibrant gradients.
- 🖐️ **ジェスチャーコントロール / Gesture Control**: 明るさ、音量、シークを直感的に操作。 / Intuitively control brightness, volume, and seeking.
- ⚡ **再生速度変更 / Playback Speed Control**: 0.5xから2.0xまで調整可能。 / Adjustable from 0.5x to 2.0x.
- 🔖 **レジューム再生 / Resume Playback**: 続きから再生。 / Resume from where you left off.

## 📁 フォルダ構成 / Directory Structure

```text
f:/app/Android/
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
