<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="96" alt="Slate App Icon" />

# Slate

**A ~6 MB, zero-bloat Material 3 Expressive task and calendar workspace for Android.**

[![Latest Release](https://img.shields.io/github/v/release/Siddharth-R-Shettigar/Slate?style=for-the-badge&color=AEC6FF&labelColor=12151C)](https://github.com/Siddharth-R-Shettigar/Slate/releases/latest)
[![Platform](https://img.shields.io/badge/Android-13%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=12151C)](https://github.com/Siddharth-R-Shettigar/Slate/releases/latest)
[![Size](https://img.shields.io/badge/APK%20Size-~6%20MB-8AB4F8?style=for-the-badge&labelColor=12151C)](https://github.com/Siddharth-R-Shettigar/Slate/releases/latest)

[Download Latest APK](https://github.com/Siddharth-R-Shettigar/Slate/releases/latest) · [Report a Bug](https://github.com/Siddharth-R-Shettigar/Slate/issues)

</div>

---

## Preview

<p align="center">
  <img src="screenshots/List Pane.png" width="23%" alt="Lists Workspace" />
  <img src="screenshots/Calendar Screen.png" width="23%" alt="Calendar Sync" />
  <img src="screenshots/Search Screen.png" width="23%" alt="Expressive Interactions" />
  <img src="screenshots/Settings Page.png" width="23%" alt="Settings & About" />
</p>

## Why Slate?

Most task apps either feel bloated with subscriptions and cloud accounts or lack modern tactile motion. Slate is built natively in **Kotlin & Jetpack Compose** with GPU-deferred animations (`graphicsLayer` & `drawWithContent`) so it stays lightweight, instant, and smooth even on mid-range hardware.

### Key Highlights
* **Layered Floating Workspace:** A draggable foreground task sheet layered over a 3-tab horizontal workspace (`Lists`, `Calendar`, `Search`) with full Android predictive back support.
* **Native Two-Way Calendar Sync:** Reads and writes directly via Android's `CalendarContract`—tap any event to expand full timings, notes, and clickable Google Meet / Zoom / Teams links, or send an event to your task list on demand.
* **Material 3 Expressive Micro-UI:** Event-driven perimeter glow sweeps on search, task completion, and deletion, paired with long-press background blur menus and choreographed haptic ticks.
* **One-Handed Reachability:** Collapsible One UI-inspired headers pull controls into thumb reach, alongside a Pure AMOLED Black surface toggle and themed monochrome adaptive icon support.
* **100% Offline-First & Private:** Zero ads, zero analytics SDKs, and zero background trackers.

---

## Installation

1. Go to the [**Releases**](https://github.com/Siddharth-R-Shettigar/Slate/releases/latest) page.
2. Download **`Slate-v1.0.0.apk`** under **Assets**.
3. Open the APK on your Android device and tap **Install** (allow *Install unknown apps* if prompted).

## Tech Stack
* **UI:** Jetpack Compose, Material 3, Custom `graphicsLayer` & `drawWithContent` shaders/glows
* **Local Persistence:** Room Database & Preferences DataStore
* **System Integration:** Native Android `CalendarContract` Provider, Predictive Back API, Adaptive Monochrome Vector Icon
* **CI/CD:** Automated R8-minified & signed release builds via GitHub Actions

---

<div align="center">
  <sub>Designed and built by <a href="https://github.com/Siddharth-R-Shettigar">Siddharth R Shettigar</a></sub>
</div>
