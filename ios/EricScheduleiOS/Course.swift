import Foundation

struct Course: Identifiable, Codable, Hashable {
    enum Parity: Int, Codable, Hashable {
        case all = 0
        case odd = 1
        case even = 2
    }

    var id: UUID = UUID()
    var name: String
    var type: String
    var day: Int
    var startPeriod: Int
    var endPeriod: Int
    var startWeek: Int
    var endWeek: Int
    var parity: Parity
    var room: String
    var teacher: String
    var className: String

    func isActive(in week: Int) -> Bool {
        guard week >= startWeek, week <= endWeek else { return false }
        switch parity {
        case .all: return true
        case .odd: return week % 2 == 1
        case .even: return week % 2 == 0
        }
    }

    var weekDescription: String {
        if startWeek == endWeek { return "第\(startWeek)周" }
        let suffix: String
        switch parity {
        case .all: suffix = ""
        case .odd: suffix = "（单周）"
        case .even: suffix = "（双周）"
        }
        return "第\(startWeek)–\(endWeek)周\(suffix)"
    }
}

extension Course {
    static let defaults: [Course] = [
        Course(name: "电机学", type: "实验", day: 2, startPeriod: 1, endPeriod: 3, startWeek: 2, endWeek: 16, parity: .even, room: "25-0813", teacher: "何强", className: "电机学-0001B"),
        Course(name: "现代控制系统", type: "讲课", day: 4, startPeriod: 1, endPeriod: 2, startWeek: 1, endWeek: 16, parity: .all, room: "08-0305", teacher: "谢文静", className: "现代控制系统-0001"),
        Course(name: "现代控制系统", type: "讲课", day: 5, startPeriod: 1, endPeriod: 2, startWeek: 1, endWeek: 8, parity: .all, room: "08-0502", teacher: "谢文静", className: "现代控制系统-0001"),
        Course(name: "传感器与检测技术", type: "讲课", day: 3, startPeriod: 3, endPeriod: 4, startWeek: 1, endWeek: 16, parity: .all, room: "08-0612", teacher: "范子川", className: "传感器与检测技术-0001"),
        Course(name: "传感器与检测技术", type: "讲课", day: 4, startPeriod: 3, endPeriod: 4, startWeek: 1, endWeek: 4, parity: .all, room: "08-0610", teacher: "范子川", className: "传感器与检测技术-0001"),
        Course(name: "电机学", type: "讲课", day: 1, startPeriod: 7, endPeriod: 9, startWeek: 8, endWeek: 11, parity: .all, room: "27-0401", teacher: "祁虔", className: "电机学-0001"),
        Course(name: "电机学", type: "讲课", day: 1, startPeriod: 7, endPeriod: 9, startWeek: 12, endWeek: 15, parity: .all, room: "27-0402", teacher: "祁虔、计外9", className: "电机学-0001"),
        Course(name: "大学生职业发展与就业指导B", type: "讲课", day: 2, startPeriod: 7, endPeriod: 9, startWeek: 9, endWeek: 10, parity: .all, room: "08-0312", teacher: "苗宗霞", className: "大学生职业发展与就业指导B-0063"),
        Course(name: "大学生职业发展与就业指导B", type: "讲课", day: 2, startPeriod: 7, endPeriod: 8, startWeek: 11, endWeek: 11, parity: .all, room: "08-0312", teacher: "苗宗霞", className: "大学生职业发展与就业指导B-0063"),
        Course(name: "形势与政策", type: "讲课", day: 2, startPeriod: 7, endPeriod: 9, startWeek: 14, endWeek: 14, parity: .all, room: "08-0611", teacher: "杨靖欣", className: "形势与政策-0114"),
        Course(name: "形势与政策", type: "讲课", day: 2, startPeriod: 7, endPeriod: 9, startWeek: 15, endWeek: 15, parity: .all, room: "08-0611", teacher: "唐瑞萱", className: "形势与政策-0114"),
        Course(name: "形势与政策", type: "讲课", day: 2, startPeriod: 7, endPeriod: 9, startWeek: 16, endWeek: 16, parity: .all, room: "08-0611", teacher: "李飞阳", className: "形势与政策-0114"),
        Course(name: "微机原理与接口技术", type: "讲课", day: 4, startPeriod: 7, endPeriod: 9, startWeek: 1, endWeek: 12, parity: .all, room: "28-0303", teacher: "赵亦欣", className: "微机原理与接口技术-0001"),
        Course(name: "微机原理与接口技术", type: "讲课", day: 7, startPeriod: 7, endPeriod: 9, startWeek: 13, endWeek: 15, parity: .odd, room: "28-0202", teacher: "赵亦欣", className: "微机原理与接口技术-0001"),
        Course(name: "电机学", type: "讲课", day: 2, startPeriod: 12, endPeriod: 14, startWeek: 12, endWeek: 15, parity: .all, room: "27-0406", teacher: "祁虔、计外9", className: "电机学-0001"),
        Course(name: "电机学", type: "讲课", day: 3, startPeriod: 12, endPeriod: 14, startWeek: 12, endWeek: 15, parity: .all, room: "27-0406", teacher: "祁虔、计外9", className: "电机学-0001"),
        Course(name: "微机原理与接口技术", type: "实验", day: 4, startPeriod: 12, endPeriod: 14, startWeek: 1, endWeek: 15, parity: .odd, room: "25-0803", teacher: "赵亦欣", className: "微机原理与接口技术-0001A"),
        Course(name: "传感器与检测技术", type: "实验", day: 4, startPeriod: 12, endPeriod: 14, startWeek: 2, endWeek: 16, parity: .even, room: "25-0812", teacher: "张建成", className: "传感器与检测技术-0001B"),
        Course(name: "微机原理与接口技术", type: "讲课", day: 5, startPeriod: 12, endPeriod: 14, startWeek: 14, endWeek: 16, parity: .even, room: "08-0306", teacher: "赵亦欣", className: "微机原理与接口技术-0001")
    ]
}
