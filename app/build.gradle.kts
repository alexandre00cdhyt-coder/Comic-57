name: Build APK

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.6'
          cache-read-only: false

      - name: Build debug APK
        run: gradle assembleDebug --stacktrace

      - name: Upload APK (Solo arm64)
        uses: actions/upload-artifact@v4
        with:
          name: MangaUniversal-APK
          path: app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
          retention-days: 3
