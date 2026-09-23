import Foundation
import PDFKit

struct ParsedSchedule {
    var courses: [Course]
    var title: String
}

enum SchedulePDFParserError: LocalizedError {
    case unreadable
    case noText
    case noCourses

    var errorDescription: String? {
        switch self {
        case .unreadable: return "无法读取所选 PDF。"
        case .noText: return "PDF 没有可提取文字；截图或扫描版暂不支持自动识别。"
        case .noCourses: return "没有识别到课程，请使用学校教务系统直接导出的原始课表 PDF。"
        }
    }
}

enum SchedulePDFParser {
    private static let periodPattern = #"\((\d+)\s*-\s*(\d+)节\)\s*(\d+)(?:\s*-\s*(\d+))?周(?:\((单|双)\))?"#
    private static let roomPattern = #"[/／]场地[:：]([^/／]+)"#
    private static let teacherPattern = #"[/／]教师[:：]([^/／]+)"#
    private static let classPattern = #"[/／]教学班[:：](.*?)(?:[/／]教学班组成|[/／]考核方式|[/／]选课备注|$)"#

    static func parse(url: URL) throws -> ParsedSchedule {
        guard let document = PDFDocument(url: url), document.pageCount > 0 else {
            throw SchedulePDFParserError.unreadable
        }

        var pages: [PageData] = []
        var plain = ""
        for index in 0..<document.pageCount {
            guard let page = document.page(at: index) else { continue }
            if let string = page.string { plain += string + "\n" }
            pages.append(PageData(page: page))
        }
        guard let first = pages.first, !first.glyphs.isEmpty else {
            throw SchedulePDFParserError.noText
        }

        let centers = findWeekdayCenters(first) ?? fallbackCenters(first)
        let boundaries = makeBoundaries(centers, pageWidth: first.pageWidth)
        var dayLines = Array(repeating: [String](), count: 7)

        for page in pages {
            for line in page.lines {
                for day in 0..<7 {
                    let text = cleanLine(line.text(in: boundaries[day]..<boundaries[day + 1]))
                    if !text.isEmpty { dayLines[day].append(text) }
                }
            }
        }

        var courses: [Course] = []
        var seen = Set<String>()
        for day in 0..<7 {
            parseDay(day + 1, lines: dayLines[day], output: &courses, seen: &seen)
        }
        guard !courses.isEmpty else { throw SchedulePDFParserError.noCourses }

        courses.sort {
            if $0.day != $1.day { return $0.day < $1.day }
            if $0.startPeriod != $1.startPeriod { return $0.startPeriod < $1.startPeriod }
            return $0.startWeek < $1.startWeek
        }

        var title = "导入课表"
        if let regex = try? NSRegularExpression(pattern: #"(20\d{2}-20\d{2}学年第[12]学期)"#),
           let match = regex.firstMatch(in: plain, range: NSRange(plain.startIndex..., in: plain)),
           let range = Range(match.range(at: 1), in: plain) {
            title = String(plain[range])
        }
        return ParsedSchedule(courses: courses, title: title)
    }

    private static func parseDay(_ day: Int, lines: [String], output: inout [Course], seen: inout Set<String>) {
        var i = 0
        while i < lines.count {
            let line = lines[i]
            guard let marker = firstMarker(in: line) else { i += 1; continue }

            var currentTitle = String(line[..<marker.index]).trimmingCharacters(in: .whitespacesAndNewlines)
            if i > 0, shouldPrefixTitle(previous: lines[i - 1], current: currentTitle) {
                currentTitle = lines[i - 1].trimmingCharacters(in: .whitespacesAndNewlines) + currentTitle
            }
            currentTitle = normalizeTitle(currentTitle)
            if currentTitle.isEmpty || currentTitle.count > 40 { i += 1; continue }

            var record = String(line[line.index(after: marker.index)...])
            var j = i + 1
            while j < lines.count, firstMarker(in: lines[j]) == nil {
                record += lines[j]
                j += 1
            }
            let body = record.replacingOccurrences(of: " ", with: "").replacingOccurrences(of: "　", with: "")

            guard let values = matchGroups(pattern: periodPattern, in: body), values.count >= 5 else { i += 1; continue }
            let startPeriod = Int(values[0]) ?? 1
            let endPeriod = Int(values[1]) ?? startPeriod
            let startWeek = Int(values[2]) ?? 1
            let endWeek = values[3].isEmpty ? startWeek : (Int(values[3]) ?? startWeek)
            let parity: Course.Parity = values[4] == "单" ? .odd : (values[4] == "双" ? .even : .all)

            guard startPeriod >= 1, endPeriod <= 14, startPeriod <= endPeriod,
                  startWeek >= 1, endWeek >= startWeek, endWeek <= 30 else {
                i += 1; continue
            }

            let room = trimField(firstGroup(pattern: roomPattern, in: body) ?? "未安排")
            let teacher = trimField(firstGroup(pattern: teacherPattern, in: body) ?? "未安排")
            let className = trimField(firstGroup(pattern: classPattern, in: body) ?? "")
            let type = marker.character == "◇" ? "讲课" : "实验/实践"

            let key = [currentTitle, String(day), String(startPeriod), String(endPeriod), String(startWeek), String(endWeek), String(parity.rawValue), room, teacher].joined(separator: "|")
            if seen.insert(key).inserted {
                output.append(Course(name: currentTitle, type: type, day: day, startPeriod: startPeriod, endPeriod: endPeriod, startWeek: startWeek, endWeek: endWeek, parity: parity, room: room, teacher: teacher, className: className))
            }
            i += 1
        }
    }

    private struct Marker {
        var index: String.Index
        var character: Character
    }

    private static func firstMarker(in line: String) -> Marker? {
        let a = line.firstIndex(of: "◇")
        let b = line.firstIndex(of: "◆")
        switch (a, b) {
        case (nil, nil): return nil
        case let (x?, nil): return Marker(index: x, character: "◇")
        case let (nil, y?): return Marker(index: y, character: "◆")
        case let (x?, y?): return x < y ? Marker(index: x, character: "◇") : Marker(index: y, character: "◆")
        }
    }

    private static func shouldPrefixTitle(previous: String, current: String) -> Bool {
        guard !previous.isEmpty, !current.isEmpty, previous.count <= 24, current.count <= 12 else { return false }
        if previous.contains("/") || previous.contains(":") || previous.contains("：") || previous.contains("周") || previous.contains("节") { return false }
        if firstMarker(in: previous) != nil { return false }
        return previous.range(of: #"[\u{4e00}-\u{9fa5}]"#, options: .regularExpression) != nil
    }

    private static func normalizeTitle(_ input: String) -> String {
        var s = input.replacingOccurrences(of: " ", with: "").replacingOccurrences(of: "　", with: "").replacingOccurrences(of: "\n", with: "")
        s = s.replacingOccurrences(of: #"^[0-9:：\-—]+"#, with: "", options: .regularExpression)
        s = s.replacingOccurrences(of: #"[◇◆]+$"#, with: "", options: .regularExpression)
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func cleanLine(_ value: String) -> String {
        value.replacingOccurrences(of: #"\s+"#, with: "", options: .regularExpression).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func trimField(_ value: String) -> String {
        var s = value.replacingOccurrences(of: "\n", with: "").replacingOccurrences(of: "\r", with: "").replacingOccurrences(of: "　", with: " ").trimmingCharacters(in: .whitespacesAndNewlines)
        while s.hasSuffix("/") { s.removeLast(); s = s.trimmingCharacters(in: .whitespacesAndNewlines) }
        return s
    }

    private static func matchGroups(pattern: String, in text: String) -> [String]? {
        guard let regex = try? NSRegularExpression(pattern: pattern),
              let match = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)) else { return nil }
        var groups: [String] = []
        for i in 1..<match.numberOfRanges {
            let r = match.range(at: i)
            if r.location == NSNotFound { groups.append(""); continue }
            groups.append((text as NSString).substring(with: r))
        }
        return groups
    }

    private static func firstGroup(pattern: String, in text: String) -> String? {
        matchGroups(pattern: pattern, in: text)?.first
    }

    private static func findWeekdayCenters(_ page: PageData) -> [CGFloat]? {
        let names = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
        var centers: [CGFloat] = []
        for name in names {
            guard let center = page.lines.compactMap({ $0.center(of: name) }).first else { return nil }
            centers.append(center)
        }
        for i in 1..<centers.count where centers[i] <= centers[i - 1] { return nil }
        return centers
    }

    private static func fallbackCenters(_ page: PageData) -> [CGFloat] {
        let left = page.pageWidth * 0.145
        let right = page.pageWidth * 0.995
        let col = (right - left) / 7.0
        return (0..<7).map { left + col * (CGFloat($0) + 0.5) }
    }

    private static func makeBoundaries(_ centers: [CGFloat], pageWidth: CGFloat) -> [CGFloat] {
        var b = Array(repeating: CGFloat.zero, count: 8)
        for i in 1..<7 { b[i] = (centers[i - 1] + centers[i]) / 2 }
        let firstGap = centers[1] - centers[0]
        let lastGap = centers[6] - centers[5]
        b[0] = max(0, centers[0] - firstGap / 2)
        b[7] = min(pageWidth, centers[6] + lastGap / 2)
        return b
    }

    private struct Glyph {
        var x: CGFloat
        var y: CGFloat
        var width: CGFloat
        var text: String
        var centerX: CGFloat { x + width / 2 }
    }

    private struct Line {
        var y: CGFloat
        var glyphs: [Glyph]

        func text(in range: Range<CGFloat>) -> String {
            glyphs
                .filter { range.contains($0.centerX) }
                .sorted { $0.x < $1.x }
                .map(\.text)
                .joined()
        }

        func center(of phrase: String) -> CGFloat? {
            let sorted = glyphs.sorted { $0.x < $1.x }
            let target = Array(phrase).map(String.init)
            guard !target.isEmpty, sorted.count >= target.count else { return nil }
            for start in 0...(sorted.count - target.count) {
                var matches = true
                for offset in target.indices where sorted[start + offset].text != target[offset] {
                    matches = false
                    break
                }
                if matches {
                    let first = sorted[start]
                    let last = sorted[start + target.count - 1]
                    return (first.x + last.x + last.width) / 2
                }
            }
            return nil
        }
    }

    private struct PageData {
        var glyphs: [Glyph]
        var lines: [Line]
        var pageWidth: CGFloat

        init(page: PDFPage) {
            let bounds = page.bounds(for: .mediaBox)
            pageWidth = bounds.width
            guard let text = page.string else {
                glyphs = []; lines = []; return
            }
            let ns = text as NSString
            var collected: [Glyph] = []
            for index in 0..<ns.length {
                let character = ns.substring(with: NSRange(location: index, length: 1))
                if character == "\n" || character == "\r" { continue }
                let rect = page.characterBounds(at: index)
                guard !rect.isNull, !rect.isInfinite, rect.width > 0.01, rect.height > 0.01 else { continue }
                collected.append(Glyph(x: rect.minX, y: rect.midY, width: rect.width, text: character))
            }
            glyphs = collected
            lines = Self.makeLines(collected)
        }

        private static func makeLines(_ glyphs: [Glyph]) -> [Line] {
            let sorted = glyphs.sorted {
                if abs($0.y - $1.y) > 0.1 { return $0.y > $1.y }
                return $0.x < $1.x
            }
            var output: [Line] = []
            for glyph in sorted {
                if let idx = output.indices.reversed().prefix(6).first(where: { abs(output[$0].y - glyph.y) <= 2.6 }) {
                    output[idx].glyphs.append(glyph)
                } else {
                    output.append(Line(y: glyph.y, glyphs: [glyph]))
                }
            }
            return output.sorted { $0.y > $1.y }
        }
    }
}
