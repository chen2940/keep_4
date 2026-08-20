# Keep4

让联想小新 Pad Pro 12.7（TB371FC）在刷入第三方 ROM 后保持四扬声器播放声音。

## 原理

- 使用“扬声器 + 录音（VOICE_COMMUNICATION）一起工作”的原理，把系统顶进四个扬声器全部驱动的音频路由；
- 应用以前台服务方式在后台保活：检测到媒体播放时自动启动触发（默认用 AudioRecord 读数据后直接丢弃，不再写 3gp 文件）；
- 界面提供“免录音模式（通信模式）”开关，不占用麦克风，改用 `MODE_IN_COMMUNICATION + 免提` 路由触发（部分 ROM 无效，需实测）。

## 使用

1. 同意应用申请的所有权限（麦克风、通知），打开“开启四扬声器”开关；
2. 在任务管理界面将本应用加入后台锁（下滑锁定），可开启允许自启、关闭电量优化；
3. 播放音乐即可触发四扬声器；开关状态已持久化，开机后若开关开启会自动拉起服务。

## 保活模式

设置页可切换四种保活方式（依次更强）：

1. **前台服务**（默认）：常驻通知 + 前台服务
2. **无障碍保活**：注册无障碍服务，系统强守护，被清理/崩溃后自动拉起四扬服务
3. **Shizuku 保活**：通过 Shizuku 写入后台/电池白名单（需安装 Shizuku 并授权）
4. **Root 保活（Magisk/KernelSU）**：通过 su（Magisk / KernelSU 均兼容）写入后台/电池白名单（弹出 root 管理器授权）

> 白名单命令：`dumpsys deviceidle whitelist` + `cmd appops set ... RUN_IN_BACKGROUND / RUN_ANY_IN_BACKGROUND / RUN_FOREGROUND_SERVICE / START_FOREGROUND / BATTERY allow`

## 构建

- Android Studio：Otter 3 Feature Drop 或更新（AGP 9.3.0 / Gradle 9.6.1 / Kotlin 2.4.0 / compileSdk 37 / minSdk 24）
- 或命令行：`./gradlew assembleDebug`（需要 JDK 17+ 与 Android SDK 37）；`./gradlew assembleRelease` 会启用 R8 混淆与资源压缩，产出体积更小的 APK

## 致谢

- 本项目基于 [aimmarc/keep_4](https://github.com/aimmarc/keep_4) 改进而来，感谢原作者的开源分享；
- Root 与后台保活相关功能参考了 [ZxsRegards/keep4.1](https://github.com/ZxsRegards/keep4.1) 的实现思路。

## 免责声明

完全开源、完全免费，无需捐赠；本应用仅供该机型在第三方 ROM 下强制开启四扬声器使用，
可能引起耗电增加、系统稳定性问题，如介意请卸载。谢绝盗用或用于违法行为。随缘更新。

## 下载 / Release

<a href="https://github.com/chen2940/keep_4/releases">点击查看 GitHub Releases</a>