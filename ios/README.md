# Eric Schedule · iOS

这是 Eric 课表 App 的原生 iOS / SwiftUI 版本，按 iPhone Pro 尺寸与 iOS 毛玻璃风格设计。

## v1.0

- 默认：2026-08-31（周一）= 第 1 周。
- 周视图日程：周一到周日 × 1–14 节。
- 日程 / 列表双视图。
- 单周、双周、指定周自动过滤。
- 自动定位当前周与今天。
- 点击课程查看教室、教师、周次、课程类型与教学班。
- 本周总览。
- 支持导入学校教务系统导出的 PDF 课表。
- PDF 解析使用 PDFKit 获取字符坐标，按星期列恢复课程。
- 导入后可重新选择“第一周周一”。
- 课程数据离线保存在本机。

## UI

主色为清新的薄荷绿、浅天蓝和低饱和课程色，使用 SwiftUI Material 做轻量毛玻璃，不使用偏粉风格。

## 本地运行

1. macOS 安装 Xcode。
2. 安装 XcodeGen：`brew install xcodegen`。
3. 在 `ios` 目录运行：`xcodegen generate`。
4. 打开 `EricScheduleiOS.xcodeproj`。
5. Signing & Capabilities 中选择你自己的 Apple Team。
6. 连接 iPhone 后点击 Run。

普通 Apple ID 可以用于个人设备开发签名；TestFlight / 长期分发需要 Apple Developer Program。

## CI

GitHub Actions 会在 macOS runner 上自动生成 Xcode 工程，并执行 iPhone Simulator 无签名构建验证。
