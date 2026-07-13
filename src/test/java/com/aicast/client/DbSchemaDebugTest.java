package com.aicast.client;

import org.junit.jupiter.api.Test;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

public class DbSchemaDebugTest {

    @Test
    void printAllTableSchemas() throws Exception {
        String url = "jdbc:mariadb://211.248.161.43:43306/ai_smartcast";
        String user = "ai_aventusm";
        String password = "ai_aventusm!!";
        
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password)) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 모든 테이블 조회
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    System.out.println("=========================================");
                    System.out.println("TABLE: " + tableName);
                    System.out.println("=========================================");
                    
                    // 각 테이블의 컬럼 정보 조회
                    try (ResultSet columns = metaData.getColumns(null, null, tableName, "%")) {
                        while (columns.next()) {
                            String columnName = columns.getString("COLUMN_NAME");
                            String typeName = columns.getString("TYPE_NAME");
                            int columnSize = columns.getInt("COLUMN_SIZE");
                            System.out.println("  " + columnName + " (" + typeName + ", size=" + columnSize + ")");
                        }
                    }
                    
                    // 각 테이블의 Primary Key 정보 조회
                    try (ResultSet pks = metaData.getPrimaryKeys(null, null, tableName)) {
                        while (pks.next()) {
                            System.out.println("  [PK] " + pks.getString("COLUMN_NAME") + " (seq=" + pks.getShort("KEY_SEQ") + ")");
                        }
                    }
                    System.out.println();
                }
            }
        }
    }
}
