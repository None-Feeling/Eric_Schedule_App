import SwiftUI

struct ListScheduleView: View {
    @ObservedObject var store: ScheduleStore
    @Binding var selectedCourse: Course?

    var body: some View {
        let list = store.courses(for: store.selectedWeek, day: store.selectedDay)
        VStack(alignment: .leading, spacing: 12) {
            VStack(alignment: .leading, spacing: 5) {
                Text(store.dayStatus(for: list, date: store.selectedDate))
                    .font(.headline)
                    .foregroundStyle(AppTheme.text)
                Text("轻量列表视图，方便快速查看时间、教师和教室。")
                    .font(.footnote)
                    .foregroundStyle(AppTheme.muted)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassCard(radius: 22)

            if list.isEmpty {
                VStack(spacing: 10) {
                    Image(systemName: "leaf")
                        .font(.system(size: 34))
                        .foregroundStyle(AppTheme.accent)
                    Text("今天没有课")
                        .font(.title3.bold())
                    Text("可以安排自习、运动、社团或者休息。")
                        .font(.footnote)
                        .foregroundStyle(AppTheme.muted)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 54)
                .glassCard(radius: 24)
            } else {
                ForEach(list) { course in
                    CourseListCard(course: course)
                        .onTapGesture { selectedCourse = course }
                }
            }
        }
    }
}

private struct CourseListCard: View {
    let course: Course

    var body: some View {
        let palette = AppTheme.palette(for: course.name)
        HStack(spacing: 14) {
            VStack(spacing: 3) {
                Text(ScheduleStore.periodStart[course.startPeriod - 1])
                    .font(.headline)
                Text("—")
                    .foregroundStyle(AppTheme.muted)
                Text(ScheduleStore.periodEnd[course.endPeriod - 1])
                    .font(.subheadline.bold())
                Text("第\(course.startPeriod)–\(course.endPeriod)节")
                    .font(.caption2)
                    .foregroundStyle(AppTheme.muted)
            }
            .frame(width: 68)

            Rectangle()
                .fill(palette.border)
                .frame(width: 1)

            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .top) {
                    Text(course.name)
                        .font(.headline)
                        .foregroundStyle(AppTheme.text)
                    Spacer(minLength: 8)
                    Text(course.type)
                        .font(.caption2.bold())
                        .foregroundStyle(palette.foreground)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.65), in: Capsule())
                }
                Label(course.room, systemImage: "mappin.and.ellipse")
                    .font(.subheadline)
                Label(course.teacher, systemImage: "person")
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.muted)
                Text(course.weekDescription)
                    .font(.caption)
                    .foregroundStyle(AppTheme.muted)
            }
        }
        .padding(14)
        .background(palette.background.opacity(0.96), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(palette.border, lineWidth: 1))
    }
}
