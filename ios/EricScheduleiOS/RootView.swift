import SwiftUI
import UniformTypeIdentifiers

struct RootView: View {
    @StateObject private var store = ScheduleStore()
    @State private var selectedCourse: Course?
    @State private var showImporter = false
    @State private var showOverview = false
    @State private var imported: ParsedSchedule?
    @State private var importStartDate = Date()
    @State private var showImportConfirm = false
    @State private var isImporting = false
    @State private var importError: String?

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            decorativeBackground
            VStack(spacing: 12) {
                header
                weekSelector

                ScrollView(showsIndicators: false) {
                    Group {
                        if store.mode == .agenda {
                            WeekScheduleView(store: store, selectedCourse: $selectedCourse)
                        } else {
                            ListScheduleView(store: store, selectedCourse: $selectedCourse)
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.bottom, 90)
                }
            }
        }
        .safeAreaInset(edge: .bottom) { bottomBar }
        .sheet(item: $selectedCourse) { CourseDetailView(course: $0) }
        .sheet(isPresented: $showOverview) { WeekOverviewView(store: store) }
        .sheet(isPresented: $showImportConfirm) {
            if let imported {
                ImportConfirmationView(parsed: imported, startDate: $importStartDate) {
                    store.replace(with: imported, semesterStart: importStartDate)
                }
            }
        }
        .fileImporter(isPresented: $showImporter, allowedContentTypes: [.pdf], allowsMultipleSelection: false) { result in
            switch result {
            case .success(let urls):
                guard let url = urls.first else { return }
                importPDF(url)
            case .failure(let error):
                importError = error.localizedDescription
            }
        }
        .alert("导入失败", isPresented: Binding(get: { importError != nil }, set: { if !$0 { importError = nil } })) {
            Button("知道了", role: .cancel) { importError = nil }
        } message: {
            Text(importError ?? "未知错误")
        }
        .overlay {
            if isImporting {
                ZStack {
                    Color.black.opacity(0.14).ignoresSafeArea()
                    VStack(spacing: 12) {
                        ProgressView().controlSize(.large)
                        Text("正在识别课表…").font(.headline)
                    }
                    .padding(24)
                    .glassCard(radius: 22)
                }
            }
        }
    }

    private var decorativeBackground: some View {
        GeometryReader { proxy in
            ZStack {
                Circle()
                    .fill(Color.teal.opacity(0.08))
                    .frame(width: 280, height: 280)
                    .blur(radius: 18)
                    .offset(x: -proxy.size.width * 0.36, y: -proxy.size.height * 0.36)
                Circle()
                    .fill(Color.blue.opacity(0.08))
                    .frame(width: 300, height: 300)
                    .blur(radius: 24)
                    .offset(x: proxy.size.width * 0.42, y: -proxy.size.height * 0.16)
            }
        }
        .allowsHitTesting(false)
    }

    private var header: some View {
        VStack(spacing: 12) {
            HStack {
                Button { withAnimation(.snappy) { store.moveDay(-1) } } label: {
                    Image(systemName: "chevron.left")
                        .frame(width: 42, height: 42)
                        .background(Color.white.opacity(0.45), in: RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)

                Spacer()
                VStack(spacing: 3) {
                    Text(store.formatDate(store.selectedDate, pattern: "yyyy年M月d日") + "  " + ScheduleStore.weekdayNames[store.selectedDay - 1])
                        .font(.title3.bold())
                        .foregroundStyle(AppTheme.text)
                        .minimumScaleFactor(0.75)
                    Text("第\(store.selectedWeek)周" + (store.selectedWeek == store.currentRawWeek() ? " · 当前周" : "") + " · 第一周从 \(store.formatDate(store.semesterStart, pattern: "M月d日")) 开始")
                        .font(.caption)
                        .foregroundStyle(AppTheme.muted)
                }
                Spacer()

                Button { withAnimation(.snappy) { store.moveDay(1) } } label: {
                    Image(systemName: "chevron.right")
                        .frame(width: 42, height: 42)
                        .background(Color.white.opacity(0.45), in: RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
            }

            HStack(spacing: 8) {
                headerAction("今天", icon: "location.fill") { withAnimation(.snappy) { store.goToToday() } }
                headerAction("本周", icon: "calendar") { showOverview = true }
                headerAction("导入PDF", icon: "square.and.arrow.down") { showImporter = true }
            }
        }
        .padding(16)
        .glassCard(radius: 26)
        .padding(.horizontal, 14)
        .padding(.top, 8)
    }

    private func headerAction(_ title: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Label(title, systemImage: icon)
                .font(.caption.bold())
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(Color.white.opacity(0.42), in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .foregroundStyle(AppTheme.text)
    }

    private var weekSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(1...store.semesterWeeks, id: \.self) { week in
                    Button {
                        withAnimation(.snappy) { store.selectedWeek = week }
                    } label: {
                        Text("第\(week)周")
                            .font(.caption.bold())
                            .foregroundStyle(week == store.selectedWeek ? Color.white : AppTheme.text)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 9)
                            .background(week == store.selectedWeek ? AppTheme.accent : Color.white.opacity(0.58), in: Capsule())
                            .overlay(Capsule().stroke(week == store.selectedWeek ? AppTheme.accent : AppTheme.border.opacity(0.8), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 14)
        }
    }

    private var bottomBar: some View {
        HStack(spacing: 10) {
            ForEach(ScheduleStore.ViewMode.allCases) { mode in
                Button {
                    withAnimation(.snappy) { store.mode = mode }
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: mode == .agenda ? "calendar.day.timeline.left" : "list.bullet.rectangle")
                        Text(mode.rawValue)
                    }
                    .font(.subheadline.bold())
                    .foregroundStyle(store.mode == mode ? Color.white : AppTheme.muted)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(store.mode == mode ? AppTheme.accent : AppTheme.accent.opacity(0.08), in: RoundedRectangle(cornerRadius: 17))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(9)
        .glassCard(radius: 23)
        .padding(.horizontal, 14)
        .padding(.bottom, 4)
    }

    private func importPDF(_ url: URL) {
        isImporting = true
        let accessing = url.startAccessingSecurityScopedResource()
        DispatchQueue.global(qos: .userInitiated).async {
            defer { if accessing { url.stopAccessingSecurityScopedResource() } }
            do {
                let result = try SchedulePDFParser.parse(url: url)
                DispatchQueue.main.async {
                    imported = result
                    importStartDate = store.semesterStart
                    isImporting = false
                    showImportConfirm = true
                }
            } catch {
                DispatchQueue.main.async {
                    isImporting = false
                    importError = error.localizedDescription
                }
            }
        }
    }
}
