import SwiftUI

struct ImportConfirmationView: View {
    let parsed: ParsedSchedule
    @Binding var startDate: Date
    let onImport: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("识别结果") {
                    LabeledContent("课表", value: parsed.title)
                    LabeledContent("课程时段", value: "\(parsed.courses.count)")
                }
                Section("学期设置") {
                    DatePicker("第一周周一", selection: $startDate, displayedComponents: .date)
                    Text("建议选择教务系统对应学期第 1 周的周一。当前默认仍为 2026 年 8 月 31 日。")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                Section {
                    Button {
                        onImport()
                        dismiss()
                    } label: {
                        Text("导入并替换当前课表")
                            .frame(maxWidth: .infinity)
                    }
                }
            }
            .navigationTitle("导入课表")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarLeading) { Button("取消") { dismiss() } } }
        }
    }
}
