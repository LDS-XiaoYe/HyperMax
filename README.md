<p align="center">
  <img src="imgs/hypermax-icon.png" width="132" alt="HyperMax">
</p>

<h1 align="center">HyperMax</h1>

<p align="center">
  HyperOS 4 桌面增强模块 · 布局、Dock、文件夹与手势
</p>

---

## 简介

HyperMax 是一个面向 **HyperOS 4（Android 17）** 的 Xposed 模块，专注桌面本身的体验：
把系统桌面里那些不能调的东西——网格、间距、图标大小、文件夹、Dock 背景、手势热区——
变成可以逐项调整的参数。

它不修改系统应用，也不替换桌面，所有改动都在运行时通过 Hook 完成；关闭模块即恢复原状。

## 功能

**桌面布局**

- 网格行列数、图标大小、工作区边距
- 搜索栏宽度与底部间距、页面指示器间距
- Dock 高度、上下边距、图标标题显示

**文件夹**

- 文件夹列数、大文件夹图标容量
- 标题字号、颜色、居中位置
- 自动关闭、返回动画几何、图标缩放

**Dock**

- 柔光玻璃背景（跟随壁纸明暗采样）
- 解锁飞入动画、图标方向跟随
- 图标标题

**手势**

- 返回手势热区的高度与宽度
- 桌面扩展手势、快速切换
- 最近任务内存信息

**其它**

- 负一屏小部件序列化桥
- 状态栏秒针时钟、控制中心天气
- 内置日志查看器：logcat 与 Xposed 日志合并、按级别过滤、导出

## 支持范围

| 项 | 要求 |
|---|---|
| 系统 | HyperOS 4 / Android 17 |
| 架构 | arm64-v8a |
| 框架 | LSPosed（libxposed API 102） |
| 注入 | Zygisk Next ≥ 1.5.0，LSPosed ≥ 7846 |

作用域只需要四项：

```
com.miui.home
com.android.systemui
com.miui.personalassistant
system
```

## 安装

1. 安装 APK
2. 在 LSPosed 中启用 HyperMax
3. 勾选上面的四个作用域
4. 重启桌面（或重启设备）

> 升级模块后如果桌面没有生效，先确认 LSPosed 的作用域没有掉，再强制停止桌面一次。

## 构建

需要 JDK 25、Android SDK 37、NDK 28.2.13676358：

```bash
./gradlew :app:assembleRelease
```

产物在 `app/build/outputs/apk/release/`。

签名可选：复制 `signing.properties.example` 为 `signing.properties` 并填写
`storeFile` / `storePassword` / `keyAlias` / `keyPassword`；不填则回退到 debug 签名。

## 已知限制

- 只适配 HyperOS 4；HyperOS 2/3 的桌面实现不同，未做兼容
- 只支持 arm64-v8a
- 桌面是 Flutter + Rust 实现，布局类功能依赖运行时符号解析，桌面版本大改后可能需要重新适配

## 致谢与许可

本项目基于 [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler) 的部分代码基础继续开发，
感谢原作者与贡献者。

以 **GNU Affero General Public License v3.0** 发布，见 [LICENSE](LICENSE)。
文件头中的原始版权声明予以保留。
