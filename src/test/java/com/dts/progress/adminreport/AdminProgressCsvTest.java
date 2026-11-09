package com.dts.progress.adminreport;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminProgressCsvTest {
    @Test void csvDoesNotExecuteSpreadsheetFormula(){
        assertEquals("\"'=malicious\"",AdminProgressCsv.cell("=malicious"));
        assertEquals("\"ab\"\"cd\"",AdminProgressCsv.cell("ab\"cd"));
    }
    @Test void emptyProjectionUsesRealEmptyRows(){
        Map<String,Object> a=Map.of("sourceService","dts-progress","from","start","to","end","module","THEORY","dataStatus","READY","asOf","now","theory",List.of());
        String csv=new String(AdminProgressCsv.encode(a,a),StandardCharsets.UTF_8);
        assertTrue(csv.contains("Cần ôn"));
        assertFalse(csv.contains("user_id"));
        assertTrue(csv.startsWith("\ufeff"));
    }
}
