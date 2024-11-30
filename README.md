# RFC3161TimeStampWeb

RFC-3161形式のタイムスタンプツール(Webアプリ版)のリポジトリです。PDFファイルへのタイムスタンプ埋込み(PAdES)に対応しています。

- 著者: 風来坊(@furaibo)
- 日時: 2024/11/2

下記の技術同人誌向けのサンプルとして作成しました。

- 書籍名: **自分でつくる！電帳法対応タイムスタンプツール**
- 販売URL: https://techbookfest.org/product/hRX58ECcTNDh3jZUTN16yA?productVariantID=6E7cNnimv1WgSC6fsSHuCw


## ツールの提供機能

各種機能をWebサービスとして提供しています。

* PDFファイルへのタイムスタンプ埋め込み
* PDFファイルへの添付ファイル追加
* タイムスタンプ検証結果のCSVファイル出力
* タイムスタンプ付与済みファイルの管理およびダウンロード


## 使用ライブラリ

* SpringBoot Starter ... 3.3.2
* Thymeleaf
* FlywayDB ... 10.17.0
* BouncyCastle ... 1.78.1
* Apache PDFBox ... 3.0.3
* SQLite JDBC ... 3.46.0.1
* Apache Commons CSV ... 1.11.0


## 動作サーバの設定

`resources/application.properties` ファイル内の下記項目を編集してください。

```
# server config
server.address=(サーバのドメイン or IPアドレス)
server.port=(ポート番号, デフォルト:8080)
```


## タイムスタンプ局(TSA)情報の設定

`resources/application.properties` ファイル内の下記項目を編集してください。

```
# timestamp authority(TSA) info
tsa.service.url=[タイムスタンプ局の接続先URL]
tsa.service.user.account=[BASIC認証・アカウント]
tsa.service.user.password=[BASIC認証・パスワード]
```
