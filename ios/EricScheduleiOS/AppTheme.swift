import SwiftUI

struct CoursePalette {
    let background: Color
    let border: Color
    let foreground: Color
}

enum AppTheme {
    static let accent = Color(red: 0.12, green: 0.57, blue: 0.53)
    static let accentDark = Color(red: 0.06, green: 0.42, blue: 0.39)
    static let text = Color(red: 0.15, green: 0.23, blue: 0.26)
    static let muted = Color(red: 0.44, green: 0.55, blue: 0.58)
    static let border = Color(red: 0.84, green: 0.91, blue: 0.91)

    static var background: LinearGradient {
        LinearGradient(
            colors: [
                Color(red: 0.94, green: 0.98, blue: 0.97),
                Color(red: 0.91, green: 0.96, blue: 0.99)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    static func palette(for name: String) -> CoursePalette {
        if name.contains("电机") {
            return .init(background: Color(red: 0.92, green: 0.97, blue: 1.00), border: Color(red: 0.69, green: 0.84, blue: 0.97), foreground: Color(red: 0.18, green: 0.45, blue: 0.71))
        }
        if name.contains("现代控制") {
            return .init(background: Color(red: 0.90, green: 0.97, blue: 0.95), border: Color(red: 0.66, green: 0.87, blue: 0.83), foreground: Color(red: 0.13, green: 0.49, blue: 0.42))
        }
        if name.contains("传感器") {
            return .init(background: Color(red: 0.95, green: 0.98, blue: 0.91), border: Color(red: 0.77, green: 0.89, blue: 0.65), foreground: Color(red: 0.36, green: 0.55, blue: 0.20))
        }
        if name.contains("微机") {
            return .init(background: Color(red: 0.95, green: 0.94, blue: 1.00), border: Color(red: 0.80, green: 0.76, blue: 0.96), foreground: Color(red: 0.38, green: 0.32, blue: 0.72))
        }
        if name.contains("职业") || name.contains("形势") {
            return .init(background: Color(red: 1.00, green: 0.97, blue: 0.91), border: Color(red: 0.96, green: 0.84, blue: 0.67), foreground: Color(red: 0.66, green: 0.44, blue: 0.17))
        }
        return .init(background: Color(red: 0.95, green: 0.97, blue: 0.98), border: Color(red: 0.80, green: 0.84, blue: 0.88), foreground: Color(red: 0.28, green: 0.34, blue: 0.41))
    }
}

struct GlassCardModifier: ViewModifier {
    var radius: CGFloat = 24
    func body(content: Content) -> some View {
        content
            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .stroke(Color.white.opacity(0.65), lineWidth: 0.8)
            )
            .shadow(color: Color.black.opacity(0.045), radius: 16, y: 8)
    }
}

extension View {
    func glassCard(radius: CGFloat = 24) -> some View {
        modifier(GlassCardModifier(radius: radius))
    }
}
