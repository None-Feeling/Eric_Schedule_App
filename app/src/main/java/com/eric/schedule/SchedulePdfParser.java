package com.eric.schedule;

import android.content.Context;
import android.net.Uri;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SchedulePdfParser {

    static final class Result {
        final List<MainActivity.Course> courses;
        final String title;

        Result(List<MainActivity.Course> courses, String title) {
            this.courses = courses;
            this.title = title;
        }
    }

    private static final Pattern PERIOD = Pattern.compile("\\((\\d+)\\s*-\\s*(\\d+)节\\)\\s*(\\d+)(?:\\s*-\\s*(\\d+))?周(?:\\((单|双)\\))?");
    private static final Pattern ROOM = Pattern.compile("[/／]场地[:：]([^/／]+)");
    private static final Pattern TEACHER = Pattern.compile("[/／]教师[:：]([^/／]+)");
    private static final Pattern CLASS_NAME = Pattern.compile("[/／]教学班[:：](.*?)(?:[/／]教学班组成|[/／]考核方式|[/／]选课备注|$)");

    private SchedulePdfParser() { }

    static Result parse(Context context, Uri uri) throws Exception {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("无法读取所选 PDF");

        try (InputStream input = in; PDDocument document = PDDocument.load(input)) {
            if (document.getNumberOfPages() == 0) throw new IOException("PDF 没有页面");

            List<PageData> pages = new ArrayList<>();
            StringBuilder plain = new StringBuilder();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PositionStripper stripper = new PositionStripper();
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                stripper.setSortByPosition(true);
                plain.append(stripper.getText(document)).append('\n');
                pages.add(new PageData(stripper.glyphs));
            }

            if (pages.get(0).glyphs.isEmpty()) {
                throw new IOException("PDF 没有可提取文字；截图或扫描版暂不支持自动识别");
            }

            double[] centers = findWeekdayCenters(pages.get(0));
            if (centers == null) centers = fallbackCenters(pages.get(0));
            double[] boundaries = makeBoundaries(centers, pages.get(0).pageWidth());

            @SuppressWarnings("unchecked")
            List<String>[] dayLines = new List[7];
            for (int d = 0; d < 7; d++) dayLines[d] = new ArrayList<>();

            for (PageData page : pages) {
                for (Line line : page.lines) {
                    for (int day = 0; day < 7; day++) {
                        String cell = line.textInRange(boundaries[day], boundaries[day + 1]);
                        cell = cleanLine(cell);
                        if (!cell.isEmpty()) dayLines[day].add(cell);
                    }
                }
            }

            List<MainActivity.Course> out = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int day = 0; day < 7; day++) parseDay(day + 1, dayLines[day], out, seen);

            if (out.isEmpty()) {
                throw new IOException("没有识别到课程。请使用西南大学教务系统直接导出的课表 PDF");
            }

            Collections.sort(out, (a, b) -> {
                if (a.day != b.day) return a.day - b.day;
                if (a.startPeriod != b.startPeriod) return a.startPeriod - b.startPeriod;
                return a.startWeek - b.startWeek;
            });

            String title = "导入课表";
            Matcher tm = Pattern.compile("(20\\d{2}-20\\d{2}学年第[12]学期)").matcher(plain.toString());
            if (tm.find()) title = tm.group(1);
            return new Result(out, title);
        }
    }

    private static void parseDay(int day, List<String> lines, List<MainActivity.Course> out, Set<String> seen) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int diamond = firstMarker(line);
            if (diamond < 0) continue;

            String currentTitle = line.substring(0, diamond).trim();
            if (i > 0 && shouldPrefixTitle(lines.get(i - 1), currentTitle)) currentTitle = lines.get(i - 1).trim() + currentTitle;
            currentTitle = normalizeTitle(currentTitle);
            if (currentTitle.isEmpty() || currentTitle.length() > 40) continue;

            StringBuilder record = new StringBuilder(line.substring(diamond + 1));
            int j = i + 1;
            while (j < lines.size()) {
                String next = lines.get(j);
                if (firstMarker(next) >= 0) break;
                record.append(next);
                j++;
            }
            String body = record.toString().replace(" ", "").replace("\u3000", "");
            Matcher p = PERIOD.matcher(body);
            if (!p.find()) continue;

            int startPeriod = safeInt(p.group(1), 1);
            int endPeriod = safeInt(p.group(2), startPeriod);
            int startWeek = safeInt(p.group(3), 1);
            int endWeek = p.group(4) == null ? startWeek : safeInt(p.group(4), startWeek);
            String parityText = p.group(5);
            int parity = "单".equals(parityText) ? 1 : ("双".equals(parityText) ? 2 : 0);
            if (startPeriod < 1 || endPeriod > 14 || startPeriod > endPeriod || startWeek < 1 || endWeek < startWeek || endWeek > 30) continue;

            String room = trimField(matchField(ROOM, body, "未安排"));
            String teacher = trimField(matchField(TEACHER, body, "未安排"));
            String className = trimField(matchField(CLASS_NAME, body, ""));
            String type = line.charAt(diamond) == '◇' ? "讲课" : "实验/实践";

            String key = String.format(Locale.ROOT, "%s|%d|%d|%d|%d|%d|%d|%s|%s",
                    currentTitle, day, startPeriod, endPeriod, startWeek, endWeek, parity, room, teacher);
            if (seen.add(key)) {
                out.add(new MainActivity.Course(currentTitle, type, day, startPeriod, endPeriod,
                        startWeek, endWeek, parity, room, teacher, className));
            }
        }
    }

    private static int firstMarker(String line) {
        int a = line.indexOf('◇');
        int b = line.indexOf('◆');
        if (a < 0) return b;
        if (b < 0) return a;
        return Math.min(a, b);
    }

    private static boolean shouldPrefixTitle(String previous, String current) {
        if (previous == null || previous.isEmpty() || current == null) return false;
        if (previous.length() > 24) return false;
        if (previous.contains("/") || previous.contains(":") || previous.contains("：") || previous.contains("周") || previous.contains("节")) return false;
        if (firstMarker(previous) >= 0) return false;
        return current.length() <= 12 && previous.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private static String normalizeTitle(String s) {
        if (s == null) return "";
        s = s.replace(" ", "").replace("\u3000", "").replace("\n", "").trim();
        s = s.replaceAll("^[0-9:：\\-—]+", "");
        s = s.replaceAll("[◇◆]+$", "");
        return s.trim();
    }

    private static String matchField(Pattern pattern, String body, String fallback) {
        Matcher m = pattern.matcher(body);
        return m.find() ? m.group(1) : fallback;
    }

    private static String trimField(String s) {
        if (s == null) return "";
        s = s.replace("\n", "").replace("\r", "").replace("\u3000", " ").trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1).trim();
        return s;
    }

    private static int safeInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private static String cleanLine(String s) {
        if (s == null) return "";
        return s.replace("\u0000", "").replaceAll("\\s+", "").trim();
    }

    private static double[] findWeekdayCenters(PageData page) {
        String[] names = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        double[] result = new double[7];
        for (int i = 0; i < names.length; i++) {
            result[i] = Double.NaN;
            for (Line line : page.lines) {
                double c = line.centerOfPhrase(names[i]);
                if (!Double.isNaN(c)) { result[i] = c; break; }
            }
            if (Double.isNaN(result[i])) return null;
        }
        for (int i = 1; i < result.length; i++) if (result[i] <= result[i - 1]) return null;
        return result;
    }

    private static double[] fallbackCenters(PageData page) {
        double width = page.pageWidth();
        double left = width * 0.145;
        double right = width * 0.995;
        double col = (right - left) / 7.0;
        double[] c = new double[7];
        for (int i = 0; i < 7; i++) c[i] = left + col * (i + 0.5);
        return c;
    }

    private static double[] makeBoundaries(double[] centers, double pageWidth) {
        double[] b = new double[8];
        for (int i = 1; i < 7; i++) b[i] = (centers[i - 1] + centers[i]) / 2.0;
        double firstGap = centers[1] - centers[0];
        double lastGap = centers[6] - centers[5];
        b[0] = Math.max(0, centers[0] - firstGap / 2.0);
        b[7] = Math.min(pageWidth, centers[6] + lastGap / 2.0);
        return b;
    }

    private static final class PositionStripper extends PDFTextStripper {
        final List<Glyph> glyphs = new ArrayList<>();

        PositionStripper() throws IOException { super(); }

        @Override
        protected void writeString(String text, List<TextPosition> positions) throws IOException {
            for (TextPosition p : positions) {
                String u = p.getUnicode();
                if (u == null || u.isEmpty()) continue;
                glyphs.add(new Glyph(p.getXDirAdj(), p.getYDirAdj(), p.getWidthDirAdj(), p.getHeightDir(), p.getPageWidth(), u));
            }
            super.writeString(text, positions);
        }
    }

    private static final class Glyph {
        final double x, y, w, h, pageWidth;
        final String text;
        Glyph(double x, double y, double w, double h, double pageWidth, String text) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.pageWidth = pageWidth; this.text = text;
        }
        double centerX() { return x + w / 2.0; }
    }

    private static final class Line {
        final double y;
        final List<Glyph> glyphs = new ArrayList<>();
        Line(double y) { this.y = y; }

        String textInRange(double left, double right) {
            List<Glyph> chosen = new ArrayList<>();
            for (Glyph g : glyphs) {
                double cx = g.centerX();
                if (cx >= left && cx < right) chosen.add(g);
            }
            chosen.sort(Comparator.comparingDouble(a -> a.x));
            StringBuilder sb = new StringBuilder();
            for (Glyph g : chosen) sb.append(g.text);
            return sb.toString();
        }

        double centerOfPhrase(String phrase) {
            StringBuilder all = new StringBuilder();
            List<Glyph> ordered = new ArrayList<>(glyphs);
            ordered.sort(Comparator.comparingDouble(a -> a.x));
            for (Glyph g : ordered) all.append(g.text);
            String s = all.toString().replaceAll("\\s+", "");
            int idx = s.indexOf(phrase);
            if (idx < 0) return Double.NaN;
            if (ordered.isEmpty()) return Double.NaN;
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            int logical = 0;
            for (Glyph g : ordered) {
                String t = g.text.replaceAll("\\s+", "");
                int next = logical + t.length();
                if (next > idx && logical < idx + phrase.length()) {
                    min = Math.min(min, g.x);
                    max = Math.max(max, g.x + g.w);
                }
                logical = next;
            }
            return min == Double.MAX_VALUE ? Double.NaN : (min + max) / 2.0;
        }
    }

    private static final class PageData {
        final List<Glyph> glyphs;
        final List<Line> lines = new ArrayList<>();
        PageData(List<Glyph> glyphs) {
            this.glyphs = new ArrayList<>(glyphs);
            buildLines();
        }
        double pageWidth() {
            return glyphs.isEmpty() ? 595.0 : glyphs.get(0).pageWidth;
        }
        private void buildLines() {
            List<Glyph> ordered = new ArrayList<>(glyphs);
            ordered.sort((a, b) -> {
                int cy = Double.compare(a.y, b.y);
                return cy != 0 ? cy : Double.compare(a.x, b.x);
            });
            for (Glyph g : ordered) {
                Line best = null;
                double bestDy = Double.MAX_VALUE;
                for (Line l : lines) {
                    double dy = Math.abs(l.y - g.y);
                    if (dy < 2.3 && dy < bestDy) { best = l; bestDy = dy; }
                }
                if (best == null) { best = new Line(g.y); lines.add(best); }
                best.glyphs.add(g);
            }
            lines.sort(Comparator.comparingDouble(a -> a.y));
        }
    }
}
