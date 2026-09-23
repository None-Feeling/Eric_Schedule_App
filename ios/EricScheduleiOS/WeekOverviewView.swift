import SwiftUI

struct WeekOverviewView: View {
    @ObservedObject var store: ScheduleStore
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 18) {
                    ForEach(1...7, id: \.self) { day in
                        let date = store.date(for: store.selectedWeek, day: day)
                        let list = store.courses(for: store.selectedWeek, day: day)
                        VStack(alignment: .leading, spacing: 8) {
                            Text("\(ScheduleStore.weekdayNames[day - 1])  \(store.formatDate(date, pattern: "M月d日"))")
                                .font(.headline)
                            if list.isEmpty {
                                Text("无课").foregroundStyle(.secondary)
                            } else {
                                ForEach(list) { course in
                                    let palette = AppTheme.palette(for: course.name)
                                    Text("\(ScheduleStore.periodStart[course.startPeriod - 1])  \(course.name) · \(course.room)")
                                        .font(.subheadline)
                                        .padding(10)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .background(palette.background, in: RoundedRectangle(cornerRadius: 12))
                                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(palette.border, lineWidth: 1))
                                }
                            }
                        }
                    }
                }
                .padding()
            }
            .navigationTitle("第 \(store.selectedWeek) 周总览")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("完成") { dismiss() } } }
        }
    }
}
