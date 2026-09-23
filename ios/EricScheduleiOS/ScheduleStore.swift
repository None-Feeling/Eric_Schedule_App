import Foundation
import SwiftUI

@MainActor
final class ScheduleStore: ObservableObject {
    enum ViewMode: String, CaseIterable, Identifiable {
        case agenda = "日程"
        case list = "列表"
        var id: String { rawValue }
    }

    static let periodStart = ["08:00", "08:55", "10:00", "10:55", "12:10", "13:05", "14:00", "14:55", "15:50", "16:55", "17:50", "19:20", "20:15", "21:10"]
    static let periodEnd   = ["08:45", "09:40", "10:45", "11:40", "12:55", "13:50", "14:45", "15:40", "16:35", "17:40", "18:35", "20:05", "21:00", "21:55"]
    static let weekdayNames = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
    static let weekdayShort = ["一", "二", "三", "四", "五", "六", "日"]

    @Published var courses: [Course]
    @Published var semesterStart: Date
    @Published var selectedWeek: Int = 1
    @Published var selectedDay: Int = 1
    @Published var mode: ViewMode = .agenda

    private let calendar: Calendar
    private let defaults = UserDefaults.standard
    private let coursesKey = "ios.schedule.courses.v1"
    private let startKey = "ios.schedule.start.v1"

    init() {
        var c = Calendar(identifier: .gregorian)
        c.locale = Locale(identifier: "zh_CN")
        c.timeZone = .current
        calendar = c

        let fallback = c.date(from: DateComponents(year: 2026, month: 8, day: 31)) ?? Date()
        semesterStart = defaults.object(forKey: startKey) as? Date ?? fallback

        if let data = defaults.data(forKey: coursesKey), let saved = try? JSONDecoder().decode([Course].self, from: data), !saved.isEmpty {
            courses = saved
        } else {
            courses = Course.defaults
        }
        goToToday()
    }

    var semesterWeeks: Int {
        max(16, courses.map(\.endWeek).max() ?? 16)
    }

    var selectedDate: Date {
        date(for: selectedWeek, day: selectedDay)
    }

    func startOfDay(_ date: Date) -> Date { calendar.startOfDay(for: date) }

    func weekStart(_ week: Int) -> Date {
        calendar.date(byAdding: .day, value: (week - 1) * 7, to: startOfDay(semesterStart)) ?? semesterStart
    }

    func date(for week: Int, day: Int) -> Date {
        calendar.date(byAdding: .day, value: day - 1, to: weekStart(week)) ?? semesterStart
    }

    func currentRawWeek(on date: Date = Date()) -> Int {
        let days = calendar.dateComponents([.day], from: startOfDay(semesterStart), to: startOfDay(date)).day ?? 0
        return Int(floor(Double(days) / 7.0)) + 1
    }

    func goToToday() {
        let rawWeek = currentRawWeek()
        selectedWeek = min(max(rawWeek, 1), semesterWeeks)
        let weekday = calendar.component(.weekday, from: Date())
        selectedDay = weekday == 1 ? 7 : weekday - 1
        if rawWeek < 1 || rawWeek > semesterWeeks { selectedDay = 1 }
    }

    func moveDay(_ delta: Int) {
        guard let moved = calendar.date(byAdding: .day, value: delta, to: selectedDate) else { return }
        let days = calendar.dateComponents([.day], from: startOfDay(semesterStart), to: startOfDay(moved)).day ?? 0
        let week = Int(floor(Double(days) / 7.0)) + 1
        selectedWeek = min(max(week, 1), semesterWeeks)
        let weekStartDate = weekStart(selectedWeek)
        let dayOffset = calendar.dateComponents([.day], from: weekStartDate, to: startOfDay(moved)).day ?? 0
        selectedDay = min(max(dayOffset + 1, 1), 7)
    }

    func courses(for week: Int, day: Int) -> [Course] {
        courses
            .filter { $0.day == day && $0.isActive(in: week) }
            .sorted { lhs, rhs in
                if lhs.startPeriod != rhs.startPeriod { return lhs.startPeriod < rhs.startPeriod }
                return lhs.name < rhs.name
            }
    }

    func coursesForWeek(_ week: Int) -> [Course] {
        courses
            .filter { $0.isActive(in: week) }
            .sorted {
                if $0.day != $1.day { return $0.day < $1.day }
                return $0.startPeriod < $1.startPeriod
            }
    }

    func replace(with parsed: ParsedSchedule, semesterStart newStart: Date) {
        courses = parsed.courses
        semesterStart = startOfDay(newStart)
        persist()
        goToToday()
    }

    func resetToBuiltIn() {
        courses = Course.defaults
        semesterStart = calendar.date(from: DateComponents(year: 2026, month: 8, day: 31)) ?? Date()
        persist()
        goToToday()
    }

    func persist() {
        if let data = try? JSONEncoder().encode(courses) {
            defaults.set(data, forKey: coursesKey)
        }
        defaults.set(semesterStart, forKey: startKey)
    }

    func formatDate(_ date: Date, pattern: String) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "zh_CN")
        f.dateFormat = pattern
        return f.string(from: date)
    }

    func isToday(_ date: Date) -> Bool {
        calendar.isDateInToday(date)
    }

    func dayStatus(for list: [Course], date: Date) -> String {
        guard !list.isEmpty else { return "全天无课" }
        guard calendar.isDateInToday(date) else {
            return "共 \(list.count) 个课程时段 · \(Self.periodStart[list[0].startPeriod - 1]) 开始"
        }

        let components = calendar.dateComponents([.hour, .minute], from: Date())
        let nowMinutes = (components.hour ?? 0) * 60 + (components.minute ?? 0)
        for course in list {
            let start = Self.minutes(Self.periodStart[course.startPeriod - 1])
            let end = Self.minutes(Self.periodEnd[course.endPeriod - 1])
            if nowMinutes < start {
                return "下一节 \(Self.periodStart[course.startPeriod - 1]) · \(course.name)"
            }
            if nowMinutes <= end {
                return "正在上课 · \(course.name) · \(course.room)"
            }
        }
        return "今天的课已结束"
    }

    private static func minutes(_ value: String) -> Int {
        let parts = value.split(separator: ":").compactMap { Int($0) }
        return (parts.first ?? 0) * 60 + (parts.dropFirst().first ?? 0)
    }
}
