# Vivid F Player - 技術仕様書 (Technical Specifications)

## 1. 概要
本アプリケーションは、Android Media3 (ExoPlayer) をベースとした高性能ビデオプレイヤーです。ローカルストレージのフォルダ単位での管理と、効率的な視聴体験を提供することを目的としています。

## 2. 技術スタック
- **言語**: Kotlin
- **最小SDK**: API 24 (Android 7.0)
- **ターゲットSDK**: API 34 (Android 14)
- **主要ライブラリ**:
  - `androidx.media3:media3-exoplayer`: 再生エンジン
  - `androidx.media3:media3-ui`: プレイヤーUIコンポーネント
  - `androidx.media3:media3-session`: バックグラウンド再生およびメディアセッション管理
  - `androidx.lifecycle:lifecycle-viewmodel-ktx`: アーキテクチャコンポーネント
  - `androidx.documentfile:documentfile`: ストレージアクセス (Scoped Storage 対応)

## 3. 主要コンポーネントの設計

### 3.1 フォルダ管理
`FileRepository` は `DocumentFile` API を使用して、ユーザーが選択したフォルダツリー内の動画ファイルをスキャンします。
`Scoped Storage` の制限に対応するため、`Intent.ACTION_OPEN_DOCUMENT_TREE` を通じて持続的なパーミッションを取得します。

### 3.2 再生制御 (`PlayerManager`)
ExoPlayer インスタンスを一元管理します。以下の設定を適用しています：
- `setPlaybackSpeed`: 再生速度の動的変更
- `MediaItem`: URIベースのメディア読み込み

### 3.3 連続再生とソート
`NaturalOrderComparator` により、ファイル名に含まれる数字を数値として比較します。
例：`episode_2.mp4` < `episode_10.mp4`

### 3.4 状態管理
`ResumeManager` は `SharedPreferences` を使用して以下の情報を永続化します：
- 各フォルダの最後に再生したファイル名
- 各ファイルの最終再生位置 (ミリ秒)

## 4. ジェスチャー・インターフェース
`GestureDetector` を使用して、プレイヤー画面上のタッチ操作を解析します。
- `onScroll`: スワイプの方向と移動距離に基づいて、音量、明るさ、または再生位置を調整します。
- `onSingleTapConfirmed`: オーバーレイコントロール（戻るボタン、シークバー等）の表示/非表示を切り替えます。

## 5. バックグラウンド再生
`PlaybackService` (MediaSessionService) により、アプリがバックグラウンドに移動しても音声の再生を継続可能です。システム通知領域からのコントロールもサポートしています。
