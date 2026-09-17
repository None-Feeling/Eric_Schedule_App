package com.eric.schedule;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private static final int DEFAULT_SEMESTER_WEEKS = 16;
    private static final int REQ_IMPORT_PDF = 1207;
    private static final String PREFS = "schedule_prefs";

    // 清新风配色：蓝绿为主，避免过粉。
    private static final int BG_TOP = Color.rgb(240, 250, 248);
    private static final int BG_BOTTOM = Color.rgb(233, 244, 252);
    private static final int PANEL = Color.argb(238, 255, 255, 255);
    private static final int PANEL_SOFT = Color.argb(210, 255, 255, 255);
    private static final int ACCENT = Color.rgb(32, 145, 134);
    private static final int ACCENT_DARK = Color.rgb(16, 107, 100);
    private static final int ACCENT_SOFT = Color.rgb(222, 245, 240);
    private static final int TEXT = Color.rgb(37, 58, 66);
    private static final int MUTED = Color.rgb(112, 135, 144);
    private static final int BORDER = Color.rgb(214, 231, 232);
    private static final int LINE = Color.rgb(227, 236, 239);

    private final String[] weekDayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private final String[] weekDayShort = {"一", "二", "三", "四", "五", "六", "日"};
    private final String[] periodStart = {"08:00", "08:55", "10:00", "10:55", "12:10", "13:05", "14:00", "14:55", "15:50", "16:55", "17:50", "19:20", "20:15", "21:10"};
    private final String[] periodEnd =   {"08:45", "09:40", "10:45", "11:40", "12:55", "13:50", "14:45", "15:40", "16:35", "17:40", "18:35", "20:05", "21:00", "21:55"};

    private final List<Course> courses = new ArrayList<>();
    private Calendar semesterStart;
    private int semesterWeeks = DEFAULT_SEMESTER_WEEKS;
    private int selectedWeek;
    private int selectedDay;
    private boolean agendaMode = true;

    private TextView pageTitle;
    private TextView pageSubtitle;
    private TextView dateTitle;
    private TextView daySummary;
    private LinearLayout weekChipRow;
    private LinearLayout contentList;
    private Button agendaTab;
    private Button listTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupWindow();
        PDFBoxResourceLoader.init(getApplicationContext());
        initSemester();
        if (!loadSavedSchedule()) initCourses();
        updateSemesterWeeks();
        initSelection();
        buildUi();
        refreshAll();
    }

    private void setupWindow() {
        Window w = getWindow();
        w.setStatusBarColor(BG_TOP);
        w.setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            w.getDecorView().setSystemUiVisibility(
                    w.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void initSemester() {
        semesterStart = Calendar.getInstance();
        semesterStart.set(2026, Calendar.AUGUST, 31, 0, 0, 0);
        semesterStart.set(Calendar.MILLISECOND, 0);
        long saved = getSharedPreferences(PREFS, MODE_PRIVATE).getLong("semester_start", -1L);
        if (saved > 0) semesterStart.setTimeInMillis(saved);
    }

    private void initSelection() {
        Calendar now = startOfDay(Calendar.getInstance());
        long diffMs = now.getTimeInMillis() - semesterStart.getTimeInMillis();
        int rawWeek = (int) Math.floor((double) diffMs / TimeUnit.DAYS.toMillis(7)) + 1;
        selectedWeek = clamp(rawWeek, 1, semesterWeeks);
        int dow = now.get(Calendar.DAY_OF_WEEK);
        selectedDay = dow == Calendar.SUNDAY ? 7 : dow - 1;
        if (rawWeek < 1 || rawWeek > semesterWeeks) selectedDay = 1;
    }

    private void initCourses() {
        add("电机学", "实验", 2, 1, 3, 2, 16, 2, "25-0813", "何强", "电机学-0001B");
        add("现代控制系统", "讲课", 4, 1, 2, 1, 16, 0, "08-0305", "谢文静", "现代控制系统-0001");
        add("现代控制系统", "讲课", 5, 1, 2, 1, 8, 0, "08-0502", "谢文静", "现代控制系统-0001");
        add("传感器与检测技术", "讲课", 3, 3, 4, 1, 16, 0, "08-0612", "范子川", "传感器与检测技术-0001");
        add("传感器与检测技术", "讲课", 4, 3, 4, 1, 4, 0, "08-0610", "范子川", "传感器与检测技术-0001");
        add("电机学", "讲课", 1, 7, 9, 8, 11, 0, "27-0401", "祁虔", "电机学-0001");
        add("电机学", "讲课", 1, 7, 9, 12, 15, 0, "27-0402", "祁虔、计外9", "电机学-0001");
        add("大学生职业发展与就业指导B", "讲课", 2, 7, 9, 9, 10, 0, "08-0312", "苗宗霞", "大学生职业发展与就业指导B-0063");
        add("大学生职业发展与就业指导B", "讲课", 2, 7, 8, 11, 11, 0, "08-0312", "苗宗霞", "大学生职业发展与就业指导B-0063");
        add("形势与政策", "讲课", 2, 7, 9, 14, 14, 0, "08-0611", "杨靖欣", "形势与政策-0114");
        add("形势与政策", "讲课", 2, 7, 9, 15, 15, 0, "08-0611", "唐瑞萱", "形势与政策-0114");
        add("形势与政策", "讲课", 2, 7, 9, 16, 16, 0, "08-0611", "李飞阳", "形势与政策-0114");
        add("微机原理与接口技术", "讲课", 4, 7, 9, 1, 12, 0, "28-0303", "赵亦欣", "微机原理与接口技术-0001");
        add("微机原理与接口技术", "讲课", 7, 7, 9, 13, 15, 1, "28-0202", "赵亦欣", "微机原理与接口技术-0001");
        add("电机学", "讲课", 2, 12, 14, 12, 15, 0, "27-0406", "祁虔、计外9", "电机学-0001");
        add("电机学", "讲课", 3, 12, 14, 12, 15, 0, "27-0406", "祁虔、计外9", "电机学-0001");
        add("微机原理与接口技术", "实验", 4, 12, 14, 1, 15, 1, "25-0803", "赵亦欣", "微机原理与接口技术-0001A");
        add("传感器与检测技术", "实验", 4, 12, 14, 2, 16, 2, "25-0812", "张建成", "传感器与检测技术-0001B");
        add("微机原理与接口技术", "讲课", 5, 12, 14, 14, 16, 2, "08-0306", "赵亦欣", "微机原理与接口技术-0001");
    }

    private void add(String name, String type, int day, int startP, int endP,
                     int startWeek, int endWeek, int parity,
                     String room, String teacher, String className) {
        courses.add(new Course(name, type, day, startP, endP, startWeek, endWeek, parity, room, teacher, className));
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(makeBackgroundGradient());

        LinearLayout topWrap = new LinearLayout(this);
        topWrap.setOrientation(LinearLayout.VERTICAL);
        topWrap.setPadding(dp(14), dp(12), dp(14), dp(8));
        root.addView(topWrap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(16), dp(16), dp(14));
        header.setBackground(makeRounded(PANEL_SOFT, Color.argb(90, 255, 255, 255), dp(24)));
        topWrap.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        headRow.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button prevDay = bubbleIcon("‹");
        prevDay.setOnClickListener(v -> moveSelectedDay(-1));
        headRow.addView(prevDay, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = dp(8);
        titleLp.rightMargin = dp(8);
        headRow.addView(titleBox, titleLp);

        dateTitle = tv("", 23, TEXT, true);
        dateTitle.setGravity(Gravity.CENTER);
        titleBox.addView(dateTitle);
        pageSubtitle = tv("", 12, MUTED, false);
        pageSubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams psLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        psLp.topMargin = dp(4);
        titleBox.addView(pageSubtitle, psLp);

        Button nextDay = bubbleIcon("›");
        nextDay.setOnClickListener(v -> moveSelectedDay(1));
        headRow.addView(nextDay, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout quickRow = new LinearLayout(this);
        quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams quickLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        quickLp.topMargin = dp(12);
        header.addView(quickRow, quickLp);

        Button today = actionButton("回到今天");
        today.setOnClickListener(v -> goToCurrentWeek());
        quickRow.addView(today, new LinearLayout.LayoutParams(0, dp(40), 1f));

        Button weekOverview = actionButton("本周总览");
        weekOverview.setOnClickListener(v -> showWeekOverview());
        LinearLayout.LayoutParams oLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        oLp.leftMargin = dp(8);
        quickRow.addView(weekOverview, oLp);

        Button importPdf = actionButton("导入PDF");
        importPdf.setOnClickListener(v -> pickSchedulePdf());
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        iLp.leftMargin = dp(8);
        quickRow.addView(importPdf, iLp);

        HorizontalScrollView weekScroll = new HorizontalScrollView(this);
        weekScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams wsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        wsLp.topMargin = dp(12);
        topWrap.addView(weekScroll, wsLp);

        weekChipRow = new LinearLayout(this);
        weekChipRow.setOrientation(LinearLayout.HORIZONTAL);
        weekScroll.addView(weekChipRow, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pageTitle = tv("", 18, TEXT, true);
        LinearLayout.LayoutParams ptLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ptLp.topMargin = dp(10);
        topWrap.addView(pageTitle, ptLp);

        daySummary = tv("", 13, MUTED, false);
        LinearLayout.LayoutParams dsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dsLp.topMargin = dp(4);
        topWrap.addView(daySummary, dsLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollLp.leftMargin = dp(14);
        scrollLp.rightMargin = dp(14);
        root.addView(scroll, scrollLp);

        contentList = new LinearLayout(this);
        contentList.setOrientation(LinearLayout.VERTICAL);
        contentList.setPadding(0, dp(8), 0, dp(16));
        scroll.addView(contentList, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(12), dp(8), dp(12), dp(12));
        bottom.setBackground(makeRounded(Color.argb(248, 255, 255, 255), Color.argb(80, 255, 255, 255), dp(22)));
        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bottomLp.leftMargin = dp(14);
        bottomLp.rightMargin = dp(14);
        bottomLp.bottomMargin = dp(10);
        root.addView(bottom, bottomLp);

        agendaTab = bottomTab("日程");
        agendaTab.setOnClickListener(v -> { agendaMode = true; refreshMode(); });
        bottom.addView(agendaTab, new LinearLayout.LayoutParams(0, dp(48), 1f));

        listTab = bottomTab("列表");
        listTab.setOnClickListener(v -> { agendaMode = false; refreshMode(); });
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        lLp.leftMargin = dp(10);
        bottom.addView(listTab, lLp);

        setContentView(root);
    }

    private void refreshAll() {
        refreshHeader();
        refreshWeekChips();
        refreshMode();
    }

    private void refreshHeader() {
        Calendar date = getDate(selectedWeek, selectedDay);
        dateTitle.setText(formatDate(date, "yyyy年M月d日") + "  " + weekDayNames[selectedDay - 1]);
        int currentWeek = getCurrentRawWeek();
        String sub = "第" + selectedWeek + "周";
        if (currentWeek >= 1 && currentWeek <= semesterWeeks && selectedWeek == currentWeek) sub += " · 当前周";
        pageSubtitle.setText(sub + " · 第一周从 " + formatDate(semesterStart, "M月d日") + " 开始");
    }

    private void refreshWeekChips() {
        weekChipRow.removeAllViews();
        for (int i = 1; i <= semesterWeeks; i++) {
            final int week = i;
            boolean active = (week == selectedWeek);
            TextView chip = tv("第" + i + "周", 13, active ? Color.WHITE : TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(10), dp(14), dp(10));
            chip.setBackground(makeRounded(active ? ACCENT : PANEL, active ? ACCENT : BORDER, dp(18)));
            chip.setOnClickListener(v -> {
                selectedWeek = week;
                refreshAll();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            weekChipRow.addView(chip, lp);
        }
    }

    private void refreshMode() {
        styleModeTabs();
        Calendar date = getDate(selectedWeek, selectedDay);
        String prefix = sameDay(date, startOfDay(Calendar.getInstance())) ? "今天" : formatDate(date, "M月d日 EEEE");
        if (agendaMode) {
            pageTitle.setText("课表日程");
            daySummary.setText(prefix + " · 周视图课表");
            refreshAgenda();
        } else {
            pageTitle.setText("课程列表");
            List<Course> dayCourses = getCourses(selectedWeek, selectedDay);
            daySummary.setText(prefix + " · " + (dayCourses.isEmpty() ? "没有课" : dayCourses.size() + " 个课程时段"));
            refreshList();
        }
    }

    private void styleModeTabs() {
        agendaTab.setTextColor(agendaMode ? Color.WHITE : MUTED);
        agendaTab.setBackground(makeRounded(agendaMode ? ACCENT : ACCENT_SOFT, agendaMode ? ACCENT : BORDER, dp(16)));
        listTab.setTextColor(!agendaMode ? Color.WHITE : MUTED);
        listTab.setBackground(makeRounded(!agendaMode ? ACCENT : ACCENT_SOFT, !agendaMode ? ACCENT : BORDER, dp(16)));
    }

    private void refreshAgenda() {
        contentList.removeAllViews();

        List<Course> selectedDayCourses = getCourses(selectedWeek, selectedDay);
        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(16), dp(14), dp(16), dp(14));
        note.setBackground(makeRounded(PANEL, Color.argb(80, 255, 255, 255), dp(22)));
        TextView title = tv("清新课表视图", 16, TEXT, true);
        note.addView(title);
        TextView hint = tv(selectedDayCourses.isEmpty() ? "当前选中日期没有课程，可以轻松安排自习或运动。" : "当前选中日期共 " + selectedDayCourses.size() + " 个课程时段，点击课程卡片可查看详情。", 12, MUTED, false);
        LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintLp.topMargin = dp(5);
        note.addView(hint, hintLp);
        contentList.addView(note, cardLp());

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        contentList.addView(hsv, cardLp());

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(dp(10), dp(10), dp(10), dp(10));
        wrapper.setBackground(makeRounded(PANEL, Color.argb(80, 255, 255, 255), dp(26)));
        hsv.addView(wrapper, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int timeWidth = dp(68);
        int dayWidth = dp(106);
        int slotHeight = dp(84);
        int headerHeight = dp(74);
        int totalHeight = slotHeight * 14;

        LinearLayout headRow = new LinearLayout(this);
        headRow.setOrientation(LinearLayout.HORIZONTAL);
        wrapper.addView(headRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout emptyTopLeft = new LinearLayout(this);
        emptyTopLeft.setBackground(makeRounded(Color.argb(90, 255, 255, 255), Color.TRANSPARENT, dp(18)));
        headRow.addView(emptyTopLeft, new LinearLayout.LayoutParams(timeWidth, headerHeight));

        Calendar monday = getWeekStart(selectedWeek);
        Calendar today = startOfDay(Calendar.getInstance());
        for (int d = 1; d <= 7; d++) {
            Calendar c = (Calendar) monday.clone();
            c.add(Calendar.DAY_OF_MONTH, d - 1);
            boolean selected = (d == selectedDay);
            boolean isToday = sameDay(c, today);

            LinearLayout dayCell = new LinearLayout(this);
            dayCell.setOrientation(LinearLayout.VERTICAL);
            dayCell.setGravity(Gravity.CENTER);
            dayCell.setPadding(dp(6), dp(8), dp(6), dp(8));
            dayCell.setBackground(makeRounded(selected ? Color.rgb(226, 246, 241) : Color.argb(90, 255, 255, 255), selected ? Color.rgb(164, 220, 208) : Color.TRANSPARENT, dp(18)));
            final int dd = d;
            dayCell.setOnClickListener(v -> {
                selectedDay = dd;
                refreshAll();
            });

            TextView t1 = tv(weekDayShort[d - 1], 18, selected ? ACCENT_DARK : TEXT, true);
            t1.setGravity(Gravity.CENTER);
            dayCell.addView(t1);
            TextView t2 = tv(formatDate(c, "M.d") + (isToday ? " · 今" : ""), 12, selected ? ACCENT_DARK : MUTED, false);
            t2.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams t2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            t2Lp.topMargin = dp(4);
            dayCell.addView(t2, t2Lp);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dayWidth, headerHeight);
            if (d > 1) lp.leftMargin = dp(6);
            headRow.addView(dayCell, lp);
        }

        LinearLayout bodyRow = new LinearLayout(this);
        bodyRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(8);
        wrapper.addView(bodyRow, bodyLp);

        LinearLayout timeCol = new LinearLayout(this);
        timeCol.setOrientation(LinearLayout.VERTICAL);
        timeCol.setBackground(makeRounded(Color.argb(80, 255, 255, 255), Color.TRANSPARENT, dp(18)));
        bodyRow.addView(timeCol, new LinearLayout.LayoutParams(timeWidth, totalHeight));

        for (int i = 0; i < 14; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            TextView idx = tv(String.valueOf(i + 1), 18, TEXT, true);
            idx.setGravity(Gravity.CENTER);
            cell.addView(idx);
            TextView tm = tv(periodStart[i] + "\n" + periodEnd[i], 10, MUTED, false);
            tm.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tmLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tmLp.topMargin = dp(4);
            cell.addView(tm, tmLp);
            timeCol.addView(cell, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, slotHeight));
        }

        FrameLayout grid = new FrameLayout(this);
        grid.setBackground(makeRounded(Color.argb(74, 255, 255, 255), Color.argb(140, 255, 255, 255), dp(22)));
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(dayWidth * 7 + dp(6) * 6, totalHeight);
        gridLp.leftMargin = dp(8);
        bodyRow.addView(grid, gridLp);

        for (int d = 1; d <= 7; d++) {
            View colBg = new View(this);
            colBg.setBackgroundColor(d == selectedDay ? Color.argb(75, 193, 233, 223) : Color.TRANSPARENT);
            FrameLayout.LayoutParams colLp = new FrameLayout.LayoutParams(dayWidth, totalHeight);
            colLp.leftMargin = (d - 1) * (dayWidth + dp(6));
            grid.addView(colBg, colLp);
        }

        for (int d = 1; d < 7; d++) {
            View vLine = new View(this);
            vLine.setBackgroundColor(LINE);
            FrameLayout.LayoutParams vLp = new FrameLayout.LayoutParams(dp(1), totalHeight);
            vLp.leftMargin = d * dayWidth + (d - 1) * dp(6) + dp(3);
            grid.addView(vLine, vLp);
        }
        for (int r = 1; r < 14; r++) {
            View hLine = new View(this);
            hLine.setBackgroundColor(LINE);
            FrameLayout.LayoutParams hLp = new FrameLayout.LayoutParams(dayWidth * 7 + dp(6) * 6, dp(1));
            hLp.topMargin = r * slotHeight;
            grid.addView(hLine, hLp);
        }

        List<Course> weekCourses = getCoursesForWeek(selectedWeek);
        for (Course c : weekCourses) {
            int[] p = palette(c.name);
            LinearLayout courseCard = new LinearLayout(this);
            courseCard.setOrientation(LinearLayout.VERTICAL);
            courseCard.setPadding(dp(8), dp(8), dp(8), dp(8));
            courseCard.setBackground(makeRounded(p[0], p[1], dp(16)));
            courseCard.setOnClickListener(v -> {
                selectedDay = c.day;
                refreshAll();
                showCourseDetail(c);
            });

            TextView name = tv(shortName(c.name, 8), 11, p[2], true);
            courseCard.addView(name);
            TextView room = tv("@" + c.room, 10, p[2], false);
            LinearLayout.LayoutParams roomLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            roomLp.topMargin = dp(3);
            courseCard.addView(room, roomLp);
            TextView teacher = tv(shortName(c.teacher, 8), 10, p[2], false);
            LinearLayout.LayoutParams teaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            teaLp.topMargin = dp(2);
            courseCard.addView(teacher, teaLp);
            TextView sec = tv("" + c.startPeriod + "-" + c.endPeriod + "节", 9, p[2], false);
            LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            secLp.topMargin = dp(4);
            courseCard.addView(sec, secLp);

            int left = (c.day - 1) * (dayWidth + dp(6)) + dp(4);
            int top = (c.startPeriod - 1) * slotHeight + dp(4);
            int height = (c.endPeriod - c.startPeriod + 1) * slotHeight - dp(8);
            FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(dayWidth - dp(8), height);
            cLp.leftMargin = left;
            cLp.topMargin = top;
            grid.addView(courseCard, cLp);
        }
    }

    private void refreshList() {
        contentList.removeAllViews();
        List<Course> dayCourses = getCourses(selectedWeek, selectedDay);
        if (dayCourses.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(52), dp(18), dp(52));
            empty.setBackground(makeRounded(PANEL, Color.argb(80, 255, 255, 255), dp(22)));
            TextView big = tv("今天没有课", 20, TEXT, true);
            big.setGravity(Gravity.CENTER);
            empty.addView(big);
            TextView hint = tv("可以安排自习、健身、社团或者休息。", 13, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hLp.topMargin = dp(6);
            empty.addView(hint, hLp);
            contentList.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return;
        }

        String status = getDayStatusText(dayCourses, sameDay(getDate(selectedWeek, selectedDay), startOfDay(Calendar.getInstance())));
        LinearLayout note = new LinearLayout(this);
        note.setOrientation(LinearLayout.VERTICAL);
        note.setPadding(dp(16), dp(14), dp(16), dp(14));
        note.setBackground(makeRounded(PANEL, Color.argb(80, 255, 255, 255), dp(22)));
        note.addView(tv(status, 14, TEXT, true));
        TextView t2 = tv("轻量列表视图，方便快速查看时间、教师和教室。", 12, MUTED, false);
        LinearLayout.LayoutParams t2Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        t2Lp.topMargin = dp(4);
        note.addView(t2, t2Lp);
        contentList.addView(note, cardLp());

        for (Course c : dayCourses) contentList.addView(createCourseCard(c), cardLp());
    }

    private View createCourseCard(Course c) {
        int[] palette = palette(c.name);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(makeRounded(palette[0], palette[1], dp(20)));
        card.setOnClickListener(v -> showCourseDetail(c));

        LinearLayout time = new LinearLayout(this);
        time.setOrientation(LinearLayout.VERTICAL);
        time.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(dp(74), ViewGroup.LayoutParams.WRAP_CONTENT);
        card.addView(time, timeLp);

        TextView t1 = tv(periodStart[c.startPeriod - 1], 16, palette[2], true);
        time.addView(t1);
        TextView line = tv("—", 12, palette[2], false);
        time.addView(line);
        TextView t2 = tv(periodEnd[c.endPeriod - 1], 14, palette[2], true);
        time.addView(t2);
        TextView pText = tv("第" + c.startPeriod + "–" + c.endPeriod + "节", 11, palette[2], false);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.topMargin = dp(4);
        time.addView(pText, pLp);

        View divider = new View(this);
        divider.setBackgroundColor(palette[1]);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(1), ViewGroup.LayoutParams.MATCH_PARENT);
        divLp.leftMargin = dp(4);
        divLp.rightMargin = dp(14);
        card.addView(divider, divLp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        card.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        info.addView(nameRow);

        TextView name = tv(c.name, 17, TEXT, true);
        name.setMaxLines(2);
        nameRow.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView badge = tv(c.type, 11, palette[2], true);
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackground(makeRounded(Color.argb(180, 255, 255, 255), palette[1], dp(18)));
        nameRow.addView(badge);

        TextView room = tv("教室  " + c.room, 13, TEXT, false);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = dp(8);
        info.addView(room, rLp);

        TextView teacher = tv("教师  " + c.teacher, 13, MUTED, false);
        LinearLayout.LayoutParams teaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        teaLp.topMargin = dp(3);
        info.addView(teacher, teaLp);

        TextView weeks = tv("周次  " + c.weekText(), 12, MUTED, false);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wLp.topMargin = dp(3);
        info.addView(weeks, wLp);

        return card;
    }

    private void showCourseDetail(Course c) {
        String message =
                "时间：" + periodStart[c.startPeriod - 1] + "–" + periodEnd[c.endPeriod - 1] + "（第" + c.startPeriod + "–" + c.endPeriod + "节）\n" +
                "教室：" + c.room + "\n" +
                "教师：" + c.teacher + "\n" +
                "周次：" + c.weekText() + "\n" +
                "类型：" + c.type + "\n" +
                "教学班：" + c.className;
        new AlertDialog.Builder(this)
                .setTitle(c.name)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showWeekOverview() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(18));
        scroll.addView(box);

        Calendar monday = getWeekStart(selectedWeek);
        for (int day = 1; day <= 7; day++) {
            Calendar date = (Calendar) monday.clone();
            date.add(Calendar.DAY_OF_MONTH, day - 1);
            TextView title = tv(weekDayNames[day - 1] + "  " + formatDate(date, "M月d日"), 16, TEXT, true);
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tLp.topMargin = day == 1 ? dp(8) : dp(18);
            box.addView(title, tLp);

            List<Course> list = getCourses(selectedWeek, day);
            if (list.isEmpty()) {
                TextView none = tv("无课", 13, MUTED, false);
                LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                nLp.topMargin = dp(5);
                box.addView(none, nLp);
            } else {
                for (Course c : list) {
                    String row = periodStart[c.startPeriod - 1] + "  " + c.name + " · " + c.room;
                    TextView item = tv(row, 13, TEXT, false);
                    item.setPadding(dp(10), dp(9), dp(10), dp(9));
                    int[] p = palette(c.name);
                    item.setBackground(makeRounded(p[0], p[1], dp(12)));
                    LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    iLp.topMargin = dp(6);
                    box.addView(item, iLp);
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("第 " + selectedWeek + " 周总览")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void pickSchedulePdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQ_IMPORT_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT_PDF || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        Toast.makeText(this, "正在识别课表…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                SchedulePdfParser.Result result = SchedulePdfParser.parse(this, uri);
                runOnUiThread(() -> onPdfParsed(result));
            } catch (Exception e) {
                runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("导入失败")
                        .setMessage("没有识别到可用课表。请使用学校教务系统导出的原始 PDF，不要使用截图转 PDF。\n\n" + e.getMessage())
                        .setPositiveButton("知道了", null)
                        .show());
            }
        }).start();
    }

    private void onPdfParsed(SchedulePdfParser.Result result) {
        if (result == null || result.courses.isEmpty()) {
            Toast.makeText(this, "未识别到课程", Toast.LENGTH_LONG).show();
            return;
        }
        Calendar d = (Calendar) semesterStart.clone();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar start = Calendar.getInstance();
            start.set(year, month, day, 0, 0, 0);
            start.set(Calendar.MILLISECOND, 0);
            int dow = start.get(Calendar.DAY_OF_WEEK);
            if (dow != Calendar.MONDAY) {
                new AlertDialog.Builder(this)
                        .setTitle("日期需要是周一")
                        .setMessage("第一周起始日期必须选择周一，请重新选择。")
                        .setPositiveButton("重新选择", (dialog, which) -> onPdfParsed(result))
                        .show();
                return;
            }
            courses.clear();
            courses.addAll(result.courses);
            semesterStart = start;
            updateSemesterWeeks();
            saveSchedule();
            initSelection();
            refreshAll();
            new AlertDialog.Builder(this)
                    .setTitle("导入成功")
                    .setMessage("已识别 " + courses.size() + " 个课程时段。\n第一周从 " + formatDate(semesterStart, "yyyy年M月d日") + " 开始。")
                    .setPositiveButton("完成", null)
                    .show();
        }, d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH));
        picker.setTitle("选择第一周的周一");
        picker.show();
    }

    private void updateSemesterWeeks() {
        int max = DEFAULT_SEMESTER_WEEKS;
        for (Course c : courses) max = Math.max(max, c.endWeek);
        semesterWeeks = max;
    }

    private void saveSchedule() {
        try {
            JSONArray arr = new JSONArray();
            for (Course c : courses) {
                JSONObject o = new JSONObject();
                o.put("name", c.name); o.put("type", c.type); o.put("day", c.day);
                o.put("startPeriod", c.startPeriod); o.put("endPeriod", c.endPeriod);
                o.put("startWeek", c.startWeek); o.put("endWeek", c.endWeek); o.put("parity", c.parity);
                o.put("room", c.room); o.put("teacher", c.teacher); o.put("className", c.className);
                arr.put(o);
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("courses", arr.toString())
                    .putLong("semester_start", semesterStart.getTimeInMillis())
                    .apply();
        } catch (Exception ignored) { }
    }

    private boolean loadSavedSchedule() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString("courses", "");
        if (raw == null || raw.isEmpty()) return false;
        try {
            JSONArray arr = new JSONArray(raw);
            if (arr.length() == 0) return false;
            courses.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                courses.add(new Course(
                        o.getString("name"), o.optString("type", "讲课"), o.getInt("day"),
                        o.getInt("startPeriod"), o.getInt("endPeriod"), o.getInt("startWeek"), o.getInt("endWeek"),
                        o.optInt("parity", 0), o.optString("room", "未安排"), o.optString("teacher", "未安排"),
                        o.optString("className", "")));
            }
            return true;
        } catch (Exception e) {
            courses.clear();
            return false;
        }
    }

    private List<Course> getCourses(int week, int day) {
        List<Course> out = new ArrayList<>();
        for (Course c : courses) if (c.day == day && c.activeInWeek(week)) out.add(c);
        Collections.sort(out, Comparator.comparingInt(a -> a.startPeriod));
        return out;
    }

    private List<Course> getCoursesForWeek(int week) {
        List<Course> out = new ArrayList<>();
        for (Course c : courses) if (c.activeInWeek(week)) out.add(c);
        Collections.sort(out, (a, b) -> {
            if (a.day != b.day) return a.day - b.day;
            return a.startPeriod - b.startPeriod;
        });
        return out;
    }

    private void goToCurrentWeek() {
        int rawWeek = getCurrentRawWeek();
        selectedWeek = clamp(rawWeek, 1, semesterWeeks);
        Calendar now = Calendar.getInstance();
        int dow = now.get(Calendar.DAY_OF_WEEK);
        selectedDay = dow == Calendar.SUNDAY ? 7 : dow - 1;
        if (rawWeek < 1 || rawWeek > semesterWeeks) selectedDay = 1;
        refreshAll();
    }

    private void moveSelectedDay(int offset) {
        Calendar c = getDate(selectedWeek, selectedDay);
        c.add(Calendar.DAY_OF_MONTH, offset);
        long diffMs = startOfDay(c).getTimeInMillis() - semesterStart.getTimeInMillis();
        int week = (int) Math.floor((double) diffMs / TimeUnit.DAYS.toMillis(7)) + 1;
        selectedWeek = clamp(week, 1, semesterWeeks);
        Calendar monday = getWeekStart(selectedWeek);
        long days = TimeUnit.MILLISECONDS.toDays(startOfDay(c).getTimeInMillis() - monday.getTimeInMillis());
        selectedDay = clamp((int) days + 1, 1, 7);
        refreshAll();
    }

    private int getCurrentRawWeek() {
        Calendar now = startOfDay(Calendar.getInstance());
        long diffMs = now.getTimeInMillis() - semesterStart.getTimeInMillis();
        return (int) Math.floor((double) diffMs / TimeUnit.DAYS.toMillis(7)) + 1;
    }

    private Calendar getWeekStart(int week) {
        Calendar c = (Calendar) semesterStart.clone();
        c.add(Calendar.DAY_OF_MONTH, (week - 1) * 7);
        return c;
    }

    private Calendar getDate(int week, int day) {
        Calendar c = getWeekStart(week);
        c.add(Calendar.DAY_OF_MONTH, day - 1);
        return c;
    }

    private Calendar startOfDay(Calendar input) {
        Calendar c = (Calendar) input.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private String formatDate(Calendar c, String pattern) {
        return new SimpleDateFormat(pattern, Locale.CHINA).format(new Date(c.getTimeInMillis()));
    }

    private String getDayStatusText(List<Course> dayCourses, boolean today) {
        if (dayCourses.isEmpty()) return "全天无课";
        if (!today) return "共 " + dayCourses.size() + " 个课程时段 · " + periodStart[dayCourses.get(0).startPeriod - 1] + " 开始";
        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        for (Course c : dayCourses) {
            int start = timeToMinutes(periodStart[c.startPeriod - 1]);
            int end = timeToMinutes(periodEnd[c.endPeriod - 1]);
            if (nowMinutes < start) return "下一节 " + periodStart[c.startPeriod - 1] + " · " + c.name;
            if (nowMinutes >= start && nowMinutes <= end) return "正在上课 · " + c.name + " · " + c.room;
        }
        return "今天的课已结束";
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        return lp;
    }

    private TextView tv(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setLineSpacing(0, 1.12f);
        return v;
    }

    private Button bubbleIcon(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(24);
        b.setTextColor(TEXT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, 0, 0, dp(2));
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setAllCaps(false);
        b.setBackground(makeRounded(Color.argb(140, 255, 255, 255), Color.argb(90, 255, 255, 255), dp(18)));
        return b;
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setPadding(dp(8), 0, dp(8), 0);
        b.setBackground(makeRounded(Color.argb(135, 255, 255, 255), Color.argb(80, 255, 255, 255), dp(14)));
        return b;
    }

    private Button bottomTab(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        b.setAllCaps(false);
        b.setMinWidth(0);
        b.setMinHeight(0);
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private GradientDrawable makeRounded(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke);
        return d;
    }

    private GradientDrawable makeBackgroundGradient() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{BG_TOP, BG_BOTTOM});
    }

    private int[] palette(String name) {
        if (name.contains("电机")) return new int[]{Color.rgb(236, 247, 255), Color.rgb(175, 214, 247), Color.rgb(46, 114, 181)};
        if (name.contains("现代控制")) return new int[]{Color.rgb(230, 247, 242), Color.rgb(168, 222, 211), Color.rgb(34, 126, 108)};
        if (name.contains("传感器")) return new int[]{Color.rgb(242, 250, 233), Color.rgb(196, 227, 165), Color.rgb(92, 139, 52)};
        if (name.contains("微机")) return new int[]{Color.rgb(241, 239, 255), Color.rgb(204, 195, 246), Color.rgb(98, 82, 184)};
        if (name.contains("职业") || name.contains("形势")) return new int[]{Color.rgb(255, 246, 233), Color.rgb(244, 214, 170), Color.rgb(168, 111, 43)};
        return new int[]{Color.rgb(241, 245, 249), Color.rgb(203, 213, 225), Color.rgb(71, 85, 105)};
    }

    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String shortName(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars) + "…";
    }

    static class Course {
        final String name;
        final String type;
        final int day;
        final int startPeriod;
        final int endPeriod;
        final int startWeek;
        final int endWeek;
        final int parity;
        final String room;
        final String teacher;
        final String className;

        Course(String name, String type, int day, int startPeriod, int endPeriod,
               int startWeek, int endWeek, int parity,
               String room, String teacher, String className) {
            this.name = name;
            this.type = type;
            this.day = day;
            this.startPeriod = startPeriod;
            this.endPeriod = endPeriod;
            this.startWeek = startWeek;
            this.endWeek = endWeek;
            this.parity = parity;
            this.room = room;
            this.teacher = teacher;
            this.className = className;
        }

        boolean activeInWeek(int week) {
            if (week < startWeek || week > endWeek) return false;
            if (parity == 1 && week % 2 == 0) return false;
            if (parity == 2 && week % 2 != 0) return false;
            return true;
        }

        String weekText() {
            if (startWeek == endWeek) return "第" + startWeek + "周";
            if (parity == 1) return "第" + startWeek + "–" + endWeek + "周（单周）";
            if (parity == 2) return "第" + startWeek + "–" + endWeek + "周（双周）";
            return "第" + startWeek + "–" + endWeek + "周";
        }
    }
}
