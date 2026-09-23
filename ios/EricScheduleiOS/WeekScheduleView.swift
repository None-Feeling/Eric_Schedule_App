import SwiftUI

struct WeekScheduleView: View {
    @ObservedObject var store: ScheduleStore
    @Binding var selectedCourse: Course?

    private let timeWidth: CGFloat = 72
    private let dayWidth: CGFloat = 112
    private let slotHeight: CGFloat = 78
    private let headerHeight: CGFloat = 70

    var body: some View {
        VStack(spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("课表日程")
                        .font(.title3.bold())
                        .foregroundStyle(AppTheme.text)
                    Text(store.isToday(store.selectedDate) ? "今天 · 周视图课表" : "\(store.formatDate(store.selectedDate, pattern: "M月d日 EEEE")) · 周视图课表")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.muted)
                }
                Spacer()
                Image(systemName: "sparkles")
                    .foregroundStyle(AppTheme.accent)
            }

            ScrollView([.horizontal, .vertical], showsIndicators: false) {
                VStack(spacing: 8) {
                    headerRow
                    timetableBody
                }
                .padding(10)
            }
            .frame(maxHeight: 760)
            .glassCard(radius: 26)
        }
    }

    private var headerRow: some View {
        HStack(spacing: 6) {
            Color.clear.frame(width: timeWidth, height: headerHeight)
            ForEach(1...7, id: \.self) { day in
                let date = store.date(for: store.selectedWeek, day: day)
                Button {
                    withAnimation(.snappy) { store.selectedDay = day }
                } label: {
                    VStack(spacing: 4) {
                        Text(ScheduleStore.weekdayShort[day - 1])
                            .font(.headline)
                        Text(store.formatDate(date, pattern: "M.d") + (store.isToday(date) ? " · 今" : ""))
                            .font(.caption2)
                    }
                    .foregroundStyle(day == store.selectedDay ? AppTheme.accentDark : AppTheme.text)
                    .frame(width: dayWidth, height: headerHeight)
                    .background(day == store.selectedDay ? AppTheme.accent.opacity(0.11) : Color.white.opacity(0.35), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .stroke(day == store.selectedDay ? AppTheme.accent.opacity(0.25) : Color.clear, lineWidth: 1)
                    )
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var timetableBody: some View {
        let gridHeight = slotHeight * 14
        let gridWidth = dayWidth * 7
        return ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(Color.white.opacity(0.34))
                .frame(width: timeWidth + gridWidth, height: gridHeight)

            Rectangle()
                .fill(AppTheme.accent.opacity(0.08))
                .frame(width: dayWidth, height: gridHeight)
                .offset(x: timeWidth + CGFloat(store.selectedDay - 1) * dayWidth)

            ForEach(0..<14, id: \.self) { row in
                VStack(spacing: 2) {
                    Text("\(row + 1)")
                        .font(.headline)
                        .foregroundStyle(AppTheme.text)
                    Text(ScheduleStore.periodStart[row])
                        .font(.system(size: 9, weight: .medium, design: .rounded))
                    Text(ScheduleStore.periodEnd[row])
                        .font(.system(size: 9, weight: .medium, design: .rounded))
                }
                .foregroundStyle(AppTheme.muted)
                .frame(width: timeWidth, height: slotHeight)
                .offset(y: CGFloat(row) * slotHeight)

                Rectangle()
                    .fill(Color.gray.opacity(0.13))
                    .frame(width: gridWidth, height: 0.7)
                    .offset(x: timeWidth, y: CGFloat(row) * slotHeight)
            }

            ForEach(0...7, id: \.self) { column in
                Rectangle()
                    .fill(Color.gray.opacity(0.11))
                    .frame(width: 0.7, height: gridHeight)
                    .offset(x: timeWidth + CGFloat(column) * dayWidth)
            }

            ForEach(store.coursesForWeek(store.selectedWeek)) { course in
                compactCourseCard(course)
                    .frame(width: dayWidth - 8, height: CGFloat(course.endPeriod - course.startPeriod + 1) * slotHeight - 8, alignment: .topLeading)
                    .offset(
                        x: timeWidth + CGFloat(course.day - 1) * dayWidth + 4,
                        y: CGFloat(course.startPeriod - 1) * slotHeight + 4
                    )
                    .onTapGesture {
                        store.selectedDay = course.day
                        selectedCourse = course
                    }
            }
        }
        .frame(width: timeWidth + gridWidth, height: gridHeight)
    }

    private func compactCourseCard(_ course: Course) -> some View {
        let palette = AppTheme.palette(for: course.name)
        return VStack(alignment: .leading, spacing: 4) {
            Text(course.name)
                .font(.system(size: 11, weight: .bold, design: .rounded))
                .lineLimit(2)
            Text("@\(course.room)")
                .font(.system(size: 9, weight: .semibold, design: .rounded))
                .lineLimit(1)
            Text(course.teacher)
                .font(.system(size: 9, design: .rounded))
                .lineLimit(1)
            Spacer(minLength: 0)
            Text("\(course.startPeriod)-\(course.endPeriod)节")
                .font(.system(size: 8, weight: .medium, design: .rounded))
        }
        .foregroundStyle(palette.foreground)
        .padding(8)
        .background(palette.background, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 15, style: .continuous).stroke(palette.border, lineWidth: 1))
    }
}
