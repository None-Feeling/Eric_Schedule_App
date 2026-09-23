import SwiftUI

struct CourseDetailView: View {
    let course: Course
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Label(course.name, systemImage: "book.closed.fill")
                        .font(.headline)
                    Label("\(ScheduleStore.periodStart[course.startPeriod - 1])–\(ScheduleStore.periodEnd[course.endPeriod - 1]) · 第\(course.startPeriod)–\(course.endPeriod)节", systemImage: "clock")
                    Label(course.room, systemImage: "mappin.and.ellipse")
                    Label(course.teacher, systemImage: "person.fill")
                    Label(course.weekDescription, systemImage: "calendar")
                    Label(course.type, systemImage: "rectangle.stack")
                }
                if !course.className.isEmpty {
                    Section("教学班") { Text(course.className) }
                }
            }
            .navigationTitle("课程详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("完成") { dismiss() } } }
        }
        .presentationDetents([.medium])
    }
}
