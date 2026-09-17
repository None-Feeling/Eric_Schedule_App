package com.eric.schedule;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class MainActivity extends Activity {
    static final int REQ_PDF=7, BLUE=Color.rgb(37,99,235), BG=Color.rgb(248,250,252), TEXT=Color.rgb(15,23,42), MUTED=Color.rgb(100,116,139), BORDER=Color.rgb(226,232,240);
    final String[] dayNames={"周一","周二","周三","周四","周五","周六","周日"};
    final String[] starts={"08:00","08:55","10:00","10:55","12:10","13:05","14:00","14:55","15:50","16:55","17:50","19:20","20:15","21:10"};
    final String[] ends={"08:45","09:40","10:45","11:40","12:55","13:50","14:45","15:40","16:35","17:40","18:35","20:05","21:00","21:55"};
    ArrayList<Course> courses=new ArrayList<>(); Calendar firstMonday=Calendar.getInstance(); int week=1,day=1; boolean agenda=true;
    TextView weekText,dateText,summary,title,subtitle; LinearLayout dayBar,list; Button agendaBtn,listBtn;

    @Override public void onCreate(Bundle b){super.onCreate(b); PDFBoxResourceLoader.init(getApplicationContext()); firstMonday.set(2026,7,31,0,0,0); firstMonday.set(Calendar.MILLISECOND,0); long savedStart=getPreferences(0).getLong("firstMonday",0); if(savedStart>0) firstMonday.setTimeInMillis(savedStart); loadCourses(); selectToday(); build(); refresh();}

    void selectToday(){Calendar n=midnight(Calendar.getInstance()); long d=(n.getTimeInMillis()-firstMonday.getTimeInMillis())/86400000L; week=Math.max(1,Math.min(16,(int)Math.floor(d/7.0)+1)); int w=n.get(Calendar.DAY_OF_WEEK); day=w==1?7:w-1;}

    void loadCourses(){String saved=getPreferences(0).getString("courses",""); if(!saved.isEmpty()){for(String ln:saved.split("\\n")){Course c=Course.parse(ln); if(c!=null)courses.add(c);} if(!courses.isEmpty())return;} seed();}
    void seed(){
        add("电机学","实验",2,1,3,2,16,2,"25-0813","何强"); add("现代控制系统","讲课",4,1,2,1,16,0,"08-0305","谢文静"); add("现代控制系统","讲课",5,1,2,1,8,0,"08-0502","谢文静");
        add("传感器与检测技术","讲课",3,3,4,1,16,0,"08-0612","范子川"); add("传感器与检测技术","讲课",4,3,4,1,4,0,"08-0610","范子川"); add("电机学","讲课",1,7,9,8,11,0,"27-0401","祁虔"); add("电机学","讲课",1,7,9,12,15,0,"27-0402","祁虔、计外9");
        add("大学生职业发展与就业指导B","讲课",2,7,9,9,10,0,"08-0312","苗宗霞"); add("大学生职业发展与就业指导B","讲课",2,7,8,11,11,0,"08-0312","苗宗霞");
        add("形势与政策","讲课",2,7,9,14,14,0,"08-0611","杨靖欣"); add("形势与政策","讲课",2,7,9,15,15,0,"08-0611","唐瑞萱"); add("形势与政策","讲课",2,7,9,16,16,0,"08-0611","李飞阳");
        add("微机原理与接口技术","讲课",4,7,9,1,12,0,"28-0303","赵亦欣"); add("微机原理与接口技术","讲课",7,7,9,13,15,1,"28-0202","赵亦欣");
        add("电机学","讲课",2,12,14,12,15,0,"27-0406","祁虔、计外9"); add("电机学","讲课",3,12,14,12,15,0,"27-0406","祁虔、计外9"); add("微机原理与接口技术","实验",4,12,14,1,15,1,"25-0803","赵亦欣"); add("传感器与检测技术","实验",4,12,14,2,16,2,"25-0812","张建成"); add("微机原理与接口技术","讲课",5,12,14,14,16,2,"08-0306","赵亦欣");
    }
    void add(String n,String t,int d,int s,int e,int sw,int ew,int p,String r,String teacher){courses.add(new Course(n,t,d,s,e,sw,ew,p,r,teacher));}

    void build(){
        getWindow().setStatusBarColor(BG); LinearLayout root=col(); root.setBackgroundColor(BG); root.setPadding(dp(14),dp(10),dp(14),0);
        LinearLayout head=col(); head.setPadding(dp(16),dp(16),dp(16),dp(14)); head.setBackground(round(Color.WHITE,BORDER,18)); root.addView(head,lp(-1,-2));
        title=txt("日程",26,TEXT,true); subtitle=txt("按时间查看每天的课程安排",13,MUTED,false); head.addView(title); head.addView(subtitle);
        LinearLayout nav=row(); nav.setGravity(Gravity.CENTER_VERTICAL); Button prev=btn("‹"),next=btn("›"); nav.addView(prev,lp(dp(44),dp(44))); LinearLayout center=col(); center.setGravity(Gravity.CENTER); weekText=txt("",18,TEXT,true); dateText=txt("",12,MUTED,false); center.addView(weekText);center.addView(dateText);nav.addView(center,new LinearLayout.LayoutParams(0,-2,1));nav.addView(next,lp(dp(44),dp(44))); head.addView(nav);
        prev.setOnClickListener(v->{if(week>1){week--;refresh();}});next.setOnClickListener(v->{if(week<16){week++;refresh();}});
        LinearLayout actions=row(); Button today=btn("回到本周"), imp=btn("导入课表 PDF"), start=btn("设置第一周"); actions.addView(today,new LinearLayout.LayoutParams(0,dp(44),1)); actions.addView(imp,new LinearLayout.LayoutParams(0,dp(44),1)); head.addView(actions); head.addView(start,lp(-1,dp(42))); today.setOnClickListener(v->{selectToday();refresh();}); imp.setOnClickListener(v->choosePdf()); start.setOnClickListener(v->setFirstWeek());
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);dayBar=row();hsv.addView(dayBar);root.addView(hsv,lp(-1,dp(72))); summary=txt("",18,TEXT,true);root.addView(summary);
        ScrollView sc=new ScrollView(this); list=col();list.setPadding(0,dp(6),0,dp(16));sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=row();bottom.setPadding(0,dp(8),0,dp(10));agendaBtn=btn("日程");listBtn=btn("列表");bottom.addView(agendaBtn,new LinearLayout.LayoutParams(0,dp(48),1));bottom.addView(listBtn,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(bottom); agendaBtn.setOnClickListener(v->{agenda=true;refreshBody();});listBtn.setOnClickListener(v->{agenda=false;refreshBody();}); setContentView(root);
    }

    void refresh(){Calendar s=(Calendar)firstMonday.clone();s.add(Calendar.DAY_OF_MONTH,(week-1)*7);Calendar e=(Calendar)s.clone();e.add(Calendar.DAY_OF_MONTH,6);weekText.setText("第 "+week+" 周");dateText.setText(fmt(s,"M月d日")+" — "+fmt(e,"M月d日"));dayBar.removeAllViews();for(int i=1;i<=7;i++){final int d=i;Calendar x=(Calendar)s.clone();x.add(Calendar.DAY_OF_MONTH,i-1);Button b=btn(dayNames[i-1]+"\n"+fmt(x,"M/d"));b.setTextColor(d==day?Color.WHITE:TEXT);b.setBackground(round(d==day?BLUE:Color.WHITE,d==day?BLUE:BORDER,14));b.setOnClickListener(v->{day=d;refresh();});LinearLayout.LayoutParams p=lp(dp(82),dp(64));p.setMargins(0,dp(4),dp(7),dp(4));dayBar.addView(b,p);}refreshBody();}
    void refreshBody(){agendaBtn.setBackground(round(agenda?BLUE:Color.WHITE,agenda?BLUE:BORDER,14));agendaBtn.setTextColor(agenda?Color.WHITE:MUTED);listBtn.setBackground(round(!agenda?BLUE:Color.WHITE,!agenda?BLUE:BORDER,14));listBtn.setTextColor(!agenda?Color.WHITE:MUTED);title.setText(agenda?"日程":"课程列表");subtitle.setText(agenda?"按时间查看每天的课程安排":"查看课程、教室、教师和周次");list.removeAllViews();ArrayList<Course> cs=getDay();summary.setText(dayNames[day-1]+" · "+(cs.isEmpty()?"没有课":cs.size()+" 个课程时段"));if(cs.isEmpty()){TextView e=txt("今天没有课程\n可以安排自习、运动或休息",18,MUTED,true);e.setGravity(Gravity.CENTER);e.setPadding(10,dp(60),10,dp(60));list.addView(e,lp(-1,-2));return;}for(Course c:cs)list.addView(agenda?agendaCard(c):listCard(c),cardLp());}
    ArrayList<Course> getDay(){ArrayList<Course> r=new ArrayList<>();for(Course c:courses)if(c.day==day&&c.active(week))r.add(c);Collections.sort(r,(a,b)->a.sp-b.sp);return r;}

    View agendaCard(Course c){LinearLayout box=row();TextView time=txt(starts[c.sp-1]+"\n"+ends[c.ep-1],14,BLUE,true);time.setGravity(Gravity.CENTER);box.addView(time,lp(dp(72),-1));LinearLayout info=col();info.setPadding(dp(14),dp(12),dp(14),dp(12));info.setBackground(round(Color.WHITE,BORDER,16));TextView n=txt(c.name,17,TEXT,true);info.addView(n);info.addView(txt(c.room+" · "+c.teacher,13,MUTED,false));info.addView(txt("第"+c.sp+"–"+c.ep+"节 · "+c.weekLabel(),12,MUTED,false));box.addView(info,new LinearLayout.LayoutParams(0,-2,1));box.setOnClickListener(v->detail(c));return box;}
    View listCard(Course c){LinearLayout info=col();info.setPadding(dp(16),dp(14),dp(16),dp(14));info.setBackground(round(Color.WHITE,BORDER,16));info.addView(txt(c.name,18,TEXT,true));info.addView(txt(starts[c.sp-1]+"–"+ends[c.ep-1]+" · 第"+c.sp+"–"+c.ep+"节",13,BLUE,true));info.addView(txt("教室  "+c.room,13,MUTED,false));info.addView(txt("教师  "+c.teacher,13,MUTED,false));info.addView(txt("周次  "+c.weekLabel(),13,MUTED,false));info.setOnClickListener(v->detail(c));return info;}
    void detail(Course c){new AlertDialog.Builder(this).setTitle(c.name).setMessage(starts[c.sp-1]+"–"+ends[c.ep-1]+"\n"+c.room+"\n"+c.teacher+"\n"+c.weekLabel()).setPositiveButton("知道了",null).show();}

    void choosePdf(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_PDF);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==REQ_PDF&&c==RESULT_OK&&data!=null&&data.getData()!=null)importPdf(data.getData());}
    void importPdf(Uri uri){final ProgressDialog pd=ProgressDialog.show(this,"正在导入","正在解析课表 PDF…",true,false);new Thread(()->{try{InputStream in=getContentResolver().openInputStream(uri);PDDocument doc=PDDocument.load(in);PDFTextStripper s=new PDFTextStripper();s.setSortByPosition(true);String text=s.getText(doc);doc.close();ArrayList<Course> parsed=parseSchoolPdf(text);runOnUiThread(()->{pd.dismiss();if(parsed.isEmpty()){new AlertDialog.Builder(this).setTitle("未识别到课程").setMessage("这个 PDF 的排版与当前支持的西南大学教务课表格式不一致。原课表不会被覆盖。").setPositiveButton("知道了",null).show();}else{courses.clear();courses.addAll(parsed);saveCourses();week=1;day=1;refresh();new AlertDialog.Builder(this).setTitle("导入成功").setMessage("已识别 "+parsed.size()+" 个课程时段。请确认第一周开始日期。\n当前："+fmt(firstMonday,"yyyy-MM-dd")).setPositiveButton("设置第一周",(d,w)->setFirstWeek()).setNegativeButton("稍后",null).show();}});}catch(Exception e){runOnUiThread(()->{pd.dismiss();Toast.makeText(this,"PDF 导入失败："+e.getMessage(),Toast.LENGTH_LONG).show();});}}).start();}

    ArrayList<Course> parseSchoolPdf(String raw){ArrayList<Course> out=new ArrayList<>();String t=raw.replace('\r',' ').replace('\n',' ');Pattern p=Pattern.compile("([^/]{2,40}?)[◇◆]?\\s*\\((\\d{1,2})-(\\d{1,2})节\\)\\s*(\\d{1,2})(?:-(\\d{1,2}))?周(?:\\((单|双)\\))?\\s*/校区:[^/]*?/场地:([^/]+?)/教师:([^/]+?)/教学班:");Matcher m=p.matcher(t);int fallbackDay=1;while(m.find()){String name=m.group(1).replaceAll(".*(?:学分[:：][0-9.]+|\\d{1,2}:\\d{2})\\s*","").trim();int sp=n(m.group(2)),ep=n(m.group(3)),sw=n(m.group(4)),ew=m.group(5)==null?sw:n(m.group(5));int parity="单".equals(m.group(6))?1:"双".equals(m.group(6))?2:0;String room=m.group(7).trim(),teacher=m.group(8).trim();int d=guessDay(raw,m.start(),fallbackDay);fallbackDay=d;String type=name.contains("实验")?"实验":"讲课";if(sp>=1&&ep<=14&&sw>=1&&ew<=30&&name.length()>1)out.add(new Course(name,type,d,sp,ep,sw,ew,parity,room,teacher));}return out;}
    int guessDay(String raw,int pos,int def){int a=Math.max(0,pos-800);String pre=raw.substring(a,pos);int best=-1,d=def;for(int i=0;i<7;i++){int x=pre.lastIndexOf(dayNames[i]);if(x>best){best=x;d=i+1;}}return d;}

    void setFirstWeek(){final EditText e=new EditText(this);e.setText(fmt(firstMonday,"yyyy-MM-dd"));e.setHint("yyyy-MM-dd");new AlertDialog.Builder(this).setTitle("第一周周一日期").setMessage("例如：2026-08-31").setView(e).setPositiveButton("保存",(d,w)->{try{Date x=new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).parse(e.getText().toString().trim());firstMonday.setTime(x);firstMonday=midnight(firstMonday);getPreferences(0).edit().putLong("firstMonday",firstMonday.getTimeInMillis()).apply();week=1;refresh();}catch(Exception ex){Toast.makeText(this,"日期格式应为 yyyy-MM-dd",Toast.LENGTH_LONG).show();}}).setNegativeButton("取消",null).show();}
    void saveCourses(){StringBuilder b=new StringBuilder();for(Course c:courses)b.append(c.pack()).append('\n');getPreferences(0).edit().putString("courses",b.toString()).apply();}

    int n(String s){try{return Integer.parseInt(s);}catch(Exception e){return 0;}}
    String fmt(Calendar c,String f){return new SimpleDateFormat(f,Locale.CHINA).format(c.getTime());} Calendar midnight(Calendar c){Calendar x=(Calendar)c.clone();x.set(Calendar.HOUR_OF_DAY,0);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);return x;}
    LinearLayout col(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x;} LinearLayout row(){LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.HORIZONTAL);return x;}
    TextView txt(String s,int z,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(color);if(bold)t.setTypeface(null,1);t.setPadding(dp(2),dp(4),dp(2),dp(4));return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(TEXT);b.setBackground(round(Color.WHITE,BORDER,12));return b;}
    GradientDrawable round(int fill,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;} int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);} LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);} LinearLayout.LayoutParams cardLp(){LinearLayout.LayoutParams p=lp(-1,-2);p.setMargins(0,dp(6),0,dp(6));return p;}

    static class Course{String name,type,room,teacher;int day,sp,ep,sw,ew,parity;Course(String n,String t,int d,int s,int e,int a,int b,int p,String r,String te){name=n;type=t;day=d;sp=s;ep=e;sw=a;ew=b;parity=p;room=r;teacher=te;}boolean active(int w){return w>=sw&&w<=ew&&(parity==0||(parity==1&&w%2==1)||(parity==2&&w%2==0));}String weekLabel(){return sw+(sw==ew?"":"–"+ew)+"周"+(parity==1?"（单）":parity==2?"（双）":"");}String pack(){return esc(name)+"\t"+esc(type)+"\t"+day+"\t"+sp+"\t"+ep+"\t"+sw+"\t"+ew+"\t"+parity+"\t"+esc(room)+"\t"+esc(teacher);}static String esc(String s){return s.replace("\\","\\\\").replace("\t"," ").replace("\n"," ");}static Course parse(String s){String[] a=s.split("\\t",-1);if(a.length<10)return null;try{return new Course(a[0],a[1],Integer.parseInt(a[2]),Integer.parseInt(a[3]),Integer.parseInt(a[4]),Integer.parseInt(a[5]),Integer.parseInt(a[6]),Integer.parseInt(a[7]),a[8],a[9]);}catch(Exception e){return null;}}}
}
