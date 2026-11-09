package com.dts.progress.adminreport;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** CSV for aggregate, source-owned progress data; never exports learner rows. */
public final class AdminProgressCsv {
    private AdminProgressCsv(){}
    public static String cell(Object raw){
        String value=raw==null?"":String.valueOf(raw).replace('\r',' ').replace('\n',' ');
        String leading=value.stripLeading();
        if(!leading.isEmpty() && "=+-@".indexOf(leading.charAt(0))>=0)value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    public static String line(Object... values){
        var cells=new String[values.length];
        for(int i=0;i<values.length;i++)cells[i]=cell(values[i]);
        return String.join(",",cells)+"\r\n";
    }
    public static byte[] encode(Map<String,Object> overview,Map<String,Object> chapters){
        var out=new StringBuilder();out.append(line("DTS — Báo cáo tiến độ học tập",overview.get("sourceService")));
        out.append(line("Từ",overview.get("from"),"Đến",overview.get("to")));
        out.append(line("Hạng GPLX",overview.get("licenseClass"),"Hoạt động",overview.get("module")));
        out.append(line("Trạng thái",overview.get("dataStatus"),"Cập nhật",overview.get("asOf")));
        out.append(line("Phân hệ","Chương","Học viên","Câu đã học/Tình huống luyện","Đạt 5/5","Cần ôn"));
        append(out,overview,"Tổng hợp");append(out,chapters,"Theo chương");
        return ("\ufeff"+out).getBytes(StandardCharsets.UTF_8);
    }
    @SuppressWarnings("unchecked")
    private static void append(StringBuilder out,Map<String,Object> source,String group){
        for(String name:List.of("theory","simulation")){
            Object values=source.get(name);
            if(values==null)continue;
            for(var item:(List<Map<String,Object>>)values){
                out.append(line(group+" — "+name,item.get("group_key"),item.get("learners"),
                    "theory".equals(name)?item.get("studied_questions"):item.get("practiced_scenarios"),
                    "simulation".equals(name)?item.get("perfect_scenarios"):null,item.get("need_review")));
            }
        }
    }
}
