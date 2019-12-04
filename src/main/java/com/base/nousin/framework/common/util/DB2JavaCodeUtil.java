package com.base.nousin.framework.common.util;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 根据数据库表自动生成Java代码类 (entity, dao, service, controller, dao.xml)
 * 依赖: lombok包,数据库驱动包
 *
 * @author tangwc
 * @since 2019/12/1
 */
@Slf4j
public class DB2JavaCodeUtil {

    private static final String DRIVER = "com.mysql.jdbc.Driver";
    private static final String URL = "jdbc:mysql://106.54.194.59:3306/nousin?useUnicode=true&characterEncoding=utf8&useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    private static final String author = "tangwc"; // 作者
    private static final boolean showColumnLength = false; // 备注显示字段长度

    public static void main(String[] args) {
        try {
            String pkg = "com.base.nousin.temp";// 生成文件存在方的包
            String path = getProjectPath() + "src/main/java/" + pkg.replace(".", "/") + "/";// 生成文件存放的路径
            System.out.println(path);
            // List<String> tableNameList = Collections.singletonList("t_product_info_ingco");
            for (Table table : getTables()) {
                // if (!tableNameList.contains(table.getTableName())) { return; }
                generateEntityFile(path, table, pkg);
                generateXmlFile(path, table, pkg);
                generateDaoFile(path, table, pkg);
                generateServiceFile(path, table, pkg);
                generateControllerFile(path, table, pkg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProjectPath() {
        return new File(DB2JavaCodeUtil.class.getResource("/").getPath()).getParentFile().getParentFile().getPath() + File.separator;
    }

    /**
     * 生成Entity层代码
     *
     * @param path  路径
     * @param table 表名
     * @param pkg   包名
     * @throws Exception
     */
    private static void generateEntityFile(String path, Table table, String pkg) throws IOException {
        String entityName = getTableName4J(table.getTableName());

        File file = new File(path + entityName + ".java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.base.nousin.framework.common.web.BaseEntity;\n");
        sb.append("import lombok.Getter;\n");
        sb.append("import lombok.Setter;\n");
        sb.append("import lombok.ToString;\n");
        List<String> baseEntityContainedType = Arrays.asList("delFlag", "version", "createDate", "createBy", "updateDate", "updateBy");
        sb.append(getImportJavaClass(table.getColumns().stream().filter(e -> !baseEntityContainedType.contains(e.getCamelColName())).map(Column::getType).collect(Collectors.toSet())));
        sb.append("\n\n");
        sb.append("/**\n");
        sb.append(" * TODO\n");
        sb.append(" *\n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
        sb.append(" */\n");
        sb.append("@Getter\n");
        sb.append("@Setter\n");
        sb.append("@ToString\n");
        sb.append("public class ").append(entityName).append(" extends BaseEntity {\n");
        for (Column column : table.getColumns()) {
            if (baseEntityContainedType.contains(column.getCamelColName()))
                continue;
            sb.append("\tprivate ").append(getJavaClass(column.getType())).append(" ").append(column.getCamelColName()).append("; // ")
                    .append(column.getComment());
            if (showColumnLength && (isString(column.getType()) || isDecimal(column.getType())))
                sb.append(" 字段长度[").append(column.getLength()).append("]");
            sb.append("\n");
        }
        sb.append("}\n");

        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + entityName + ".java");
//        log.info(sb.toString());
    }


    /**
     * 生成XML代码
     *
     * @param path  路径
     * @param table 表名
     * @param pkg   包名
     * @throws Exception
     */
    private static void generateXmlFile(String path, Table table, String pkg) throws IOException {
        String tableName = table.getTableName();
        String entityName = getTableName4J(table.getTableName());

        File file = new File(path + entityName + "Dao.xml");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        String resultType = pkg + "." + entityName;
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        sb.append("<mapper namespace=\"").append(resultType).append("Dao\">\n");
        sb.append("\t<sql id=\"baseColumn\">\n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            sb.append("\t\ta.").append(column.getColName().toLowerCase()).append(" as \"").append(column.getCamelColName()).append("\"");
            if (i == table.getColumns().size() - 1) {
                sb.append("\n");
            } else {
                sb.append(",\n");
            }
        }
        sb.append("\t</sql>\n");
        sb.append("\n");

        // get
        sb.append("\t<select id=\"get\" resultType=\"").append(resultType).append("\">\n");
        sb.append("\t\tSELECT \n");
        sb.append("\t\t\t<include refid=\"").append(resultType).append("Dao.baseColumn\"/>\n");
        sb.append("\t\tFROM ").append(tableName).append(" a \n");
        sb.append("\t\tWHERE a.del_flag=1 and a.id = #{id} \n");
        sb.append("\t</select>\n");
        sb.append("\n");

        // findList
        sb.append("\t<select id=\"findList\" resultType=\"").append(resultType).append("\">\n");
        sb.append("\t\tSELECT\n");
        sb.append("\t\t\t<include refid=\"").append(resultType).append("Dao.baseColumn\"/>\n");
        sb.append("\t\tFROM ").append(tableName).append(" a \n");
        sb.append("\t\tWHERE a.del_flag=1\n");
        sb.append("\t\tORDER BY a.id\n");
        sb.append("\t</select>\n");
        sb.append("\n");

        // findAllList
        // sb.append("\t<select id=\"findAllList\" resultType=\"").append(resultType).append("\">\n");
        // sb.append("\t\tSELECT\n");
        // sb.append("\t\t\t<include refid=\"").append(resultType).append("Dao.baseColumn\"/>\n");
        // sb.append("\t\tFROM ").append(tableName).append(" a \n");
        // sb.append("\t\tWHERE a.del_flag=1\n");
        // sb.append("\t\tORDER BY a.id\n");
        // sb.append("\t</select>\n");
        // sb.append("\n");

        // insert
        sb.append("\t<insert id=\"insert\">\n");
        sb.append("\t\tINSERT INTO ").append(tableName).append("(\n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            String columnName = table.getColumns().get(i).getColName().toLowerCase();
            if ("id".equalsIgnoreCase(columnName)) continue;
            sb.append("\t\t\t").append(columnName);
            if (i == table.getColumns().size() - 1)
                sb.append("\n");
            else
                sb.append(",\n");
        }
        sb.append("\t\t) VALUES (\n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            String columnName = column.getColName().toLowerCase();
            String camelColName = column.getCamelColName();
            if ("id".equalsIgnoreCase(columnName)) continue;

            if ("del_flag".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t1");
            } else if ("version".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t1");
            } else {
                sb.append("\t\t\t#{").append(camelColName).append("}");
            }
            if (i == table.getColumns().size() - 1)
                sb.append("\n");
            else
                sb.append(",\n");
        }
        sb.append("\t\t)\n");
        sb.append("\t</insert>\n");
        sb.append("\n");

        // update
        sb.append("\t<update id=\"update\">\n");
        sb.append("\t\tUPDATE ").append(tableName).append(" SET \n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            String columnName = column.getColName().toLowerCase();
            if ("id".equalsIgnoreCase(columnName) || "create_by".equalsIgnoreCase(columnName) || "create_date".equalsIgnoreCase(columnName))
                continue;

            if ("del_flag".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t").append(columnName).append(" = 0");
            } else if ("version".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t").append(columnName).append(" = ").append(columnName).append(" + 1");
            } else {
                sb.append("\t\t\t").append(columnName).append(" = #{").append(column.getCamelColName()).append("}");
            }
            if (i == table.getColumns().size() - 1)
                sb.append("\n");
            else
                sb.append(",\n");
        }
        sb.append("\t\tWHERE id = #{id}\n");
        sb.append("\t</update>\n");
        sb.append("\n");
        // delete
        sb.append("\t<update id=\"delete\">\n");
        sb.append("\t\tUPDATE ").append(tableName).append(" SET del_flag = 0, version = version + 1 WHERE id = #{id}\n");
        sb.append("\t</update>\n");
        sb.append("</mapper>\n");
        log.info("====文件[ {} ]已生成====", path + entityName + "Dao.xml");
        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
    }

    /**
     * 生成Dao层代码
     *
     * @param path  路径
     * @param table 表名
     * @param pkg   包名
     * @throws Exception
     */
    private static void generateDaoFile(String path, Table table, String pkg) throws Exception {
        String entityName = getTableName4J(table.getTableName());

        File file = new File(path + entityName + "Dao.java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import com.base.nousin.framework.common.web.BaseDao;\n");
        sb.append("import ").append(pkg).append(".").append(entityName).append(";\n");
        sb.append("import org.springframework.stereotype.Repository;\n");
        sb.append("\n\n");
        sb.append("/**\n");
        sb.append(" * TODO\n");
        sb.append(" *\n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
        sb.append(" */\n");
        sb.append("@Repository\n");
        sb.append("public interface ").append(entityName).append("Dao extends BaseDao<").append(entityName).append("> {\n");
        sb.append("\n");
        sb.append("}\n");
        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + entityName + "Dao.java");
    }

    /**
     * 生成Service层代码
     *
     * @param path  路径
     * @param table 表名
     * @param pkg   包名
     * @throws Exception
     */
    private static void generateServiceFile(String path, Table table, String pkg) throws Exception {
        String entityName = getTableName4J(table.getTableName());
        File file = new File(path + entityName + "Service.java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n");
        sb.append("\n");
        // sb.append("import com.github.pagehelper.PageHelper;\n");
        // sb.append("import com.github.pagehelper.PageInfo;\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import com.base.nousin.framework.common.web.BaseService;\n");
        sb.append("import ").append(pkg).append(".").append(entityName).append(";\n");
        sb.append("import ").append(pkg).append(".").append(entityName).append("Dao;\n");
        // sb.append("import org.springframework.transaction.annotation.Transactional;\n");
        sb.append("\n");
        // sb.append("import java.util.List;\n");
        sb.append("\n\n");
        sb.append("/**\n");
        sb.append(" * TODO\n");
        sb.append(" *\n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
        sb.append(" */\n");
        sb.append("@Service\n");
//        sb.append("@Transactional(readOnly = true)\n");
        sb.append("public class ").append(entityName).append("Service extends BaseService<").append(entityName).append("Dao, ").append(entityName).append("> {\n");
        sb.append("\n");
//        sb.append("\tpublic PageInfo<").append(entityName).append("> findPage(").append(entityName).append(" entity) {\n");
//        sb.append("\t\tPageHelper.startPage(entity.getPageNum(), entity.getPageSize());\n");
//        sb.append("\t\treturn new PageInfo<").append(entityName).append(">(dao.findList(entity));\n");
//        sb.append("\t}\n");
//        sb.append("\n");
//        sb.append("\tpublic List<").append(entityName).append("> findAllList(").append(entityName).append(" entity) {\n");
//        sb.append("\t\treturn dao.findAllList(entity);\n");
//        sb.append("\t}\n");
//        sb.append("\n");
//        sb.append("\t@Transactional\n");
//        sb.append("\tpublic void save(").append(entityName).append(" entity) {\n");
//        sb.append("\t\tsuper.save(entity);\n");
//        sb.append("\t}\n");
//        sb.append("\n");
//        sb.append("\t@Transactional\n");
//        sb.append("\tpublic void delete(").append(entityName).append(" entity) {\n");
//        sb.append("\t\tsuper.delete(entity);\n");
//        sb.append("\t}\n");
        sb.append("}");
        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + entityName + "Service.java");
    }

    /**
     * 生成Controller层代码
     *
     * @param path  路径
     * @param table 表名
     * @param pkg   包名
     * @throws Exception
     */
    private static void generateControllerFile(String path, Table table, String pkg) throws Exception {
        String entityName = getTableName4J(table.getTableName());

        File file = new File(path + entityName + "Controller.java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n");
        sb.append("\n");
        sb.append("import ").append(pkg).append(".").append(entityName).append("Service;\n");
        sb.append("import lombok.extern.slf4j.Slf4j;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.web.bind.annotation.RequestMapping;\n");
        sb.append("import org.springframework.web.bind.annotation.RestController;\n");
        sb.append("\n\n");
        sb.append("/**\n");
        sb.append(" * TODO\n");
        sb.append(" *\n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
        sb.append(" */\n");
        sb.append("@RestController\n");
        sb.append("@RequestMapping(value = \"\")\n");
        sb.append("@Slf4j\n");
        sb.append("public class ").append(entityName).append("Controller {\n");
        sb.append("\n");
        sb.append("\t@Autowired\n");
        sb.append("\tprivate ").append(entityName).append("Service service;\n");
        sb.append("\n");
        sb.append("\n");
        sb.append("}");

        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + entityName + "Controller.java");
//        log.info(sb.toString());
    }

    /**
     * 获取Java对应mysql字段类型的类名称
     *
     * @param typeStr Mysql 类型
     * @return Mysql 类型转 Java 类型
     */
    public static String getJavaClass(String typeStr) {
        if (isDate(typeStr)) {
            return "Date";
        }
        if (isDecimal(typeStr)) {
            return "BigDecimal";
        }
        if (isInteger(typeStr)) {
            return "Integer";
        }
        if (isBigInteger(typeStr)) {
            return "Long";
        }
        return "String";
    }

    /**
     * 获取java中对应mysql字段类型的映射
     *
     * @param mysqlValType mysql对应的字段属性
     * @return 获取属性类型
     */
    public static String getImportJavaClass(Set<String> mysqlValType) {
        Set<String> set = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        mysqlValType.forEach(typeStr -> {
            String str = "false";
            if (isDate(typeStr)) {
                str = "import java.util.Date;\n";
            } else if (isDecimal(typeStr)) {
                str = "import java.math.BigDecimal;\n";
            } else if (isInteger(typeStr)) {
                str = "import java.lang.Integer;\n";
            } else if (isBigInteger(typeStr)) {
                str = "import java.lang.Long;\n";
            } else if (isString(typeStr) || isLongString(typeStr)) {
                str = "import java.lang.String;\n";
            }
            if (set.add(str)) {
                sb.append(str);
            }
        });
        return sb.toString();
    }

    private static boolean isString(String type) {
        return "CHAR".equalsIgnoreCase(type) || "VARCHAR".equalsIgnoreCase(type) || "ENUM".equalsIgnoreCase(type) ||
                "SET".equalsIgnoreCase(type);
    }

    private static boolean isLongString(String typeStr) {
        return "BLOB".equalsIgnoreCase(typeStr) || "TEXT".equalsIgnoreCase(typeStr);
    }

    private static boolean isInteger(String type) {
        return "INT".equalsIgnoreCase(type) || "TINYINT".equalsIgnoreCase(type) || "SMALLINT".equalsIgnoreCase(type) ||
                "MEDIUMINT".equalsIgnoreCase(type) || "INTEGER".equalsIgnoreCase(type);
    }

    private static boolean isBigInteger(String type) {
        return "BIGINT".equalsIgnoreCase(type);
    }

    private static boolean isDecimal(String type) {
        return "DECIMAL".equalsIgnoreCase(type) || "FLOAT".equalsIgnoreCase(type) || "DOUBLE ".equalsIgnoreCase(type) ||
                "NUMERIC".equalsIgnoreCase(type);
    }

    private static boolean isDate(String type) {
        return "DATE".equalsIgnoreCase(type) || "TIME".equalsIgnoreCase(type) || "TIMESTAMP".equalsIgnoreCase(type) ||
                "DATETIME".equalsIgnoreCase(type);
    }

    /**
     * 下划线转驼峰
     *
     * @param str 要转换驼峰格式的字符串
     * @return 返回驼峰格式字符串
     */
    public static String camel(String str) {
        //利用正则删除下划线，把下划线后一位改成大写
        Pattern pattern = Pattern.compile("_(\\w)");
        Matcher matcher = pattern.matcher(str);
        StringBuffer sb = new StringBuffer(str);
        if (matcher.find()) {
            sb = new StringBuffer();
            //将当前匹配子串替换为指定字符串，并且将替换后的子串以及其之前到上次匹配子串之后的字符串段添加到一个StringBuffer对象里。
            //正则之前的字符和被替换的字符
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            //把之后的也添加到StringBuffer对象里
            matcher.appendTail(sb);
        } else {
            return sb.toString();
        }
        return camel(sb.toString());
    }

    /**
     * 创建不存在的文件，如果存在则返回false，不存在则创建并返回true
     *
     * @param file 文件
     * @return 返回是否创建文件结果
     * @throws IOException RuntimeException
     */
    private static boolean createFileIfAbsent(File file) throws IOException {
        if (!file.exists()) {
            boolean mkdirs = file.getParentFile().mkdirs();
            boolean newFile = file.createNewFile();
            return true;
        }
        return false;
    }

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库下的所有表
     */
    public static List<Table> getTables() {
        List<Table> tables = new ArrayList<>();
        ResultSet rs = null;
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            //获取数据库的元数据
            DatabaseMetaData db = conn.getMetaData();
            //从元数据中获取到所有的表名
            rs = db.getTables(null, null, null, new String[]{"TABLE"});
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                Table table = new Table();
                table.setTableName(tableName);
                List<Column> columnList = new ArrayList<>();
                ResultSet columns = db.getColumns(null, "%", tableName, "%");
                while (columns.next()) {
                    int decimal_digits = Integer.parseInt(Optional.ofNullable(columns.getString("DECIMAL_DIGITS")).orElse("0"));
                    columnList.add(new Column(
                            columns.getString("COLUMN_NAME"), // 字段名
                            columns.getString("TYPE_NAME"), // 类型
                            columns.getString("REMARKS"), // 备注
                            columns.getString("IS_NULLABLE").equalsIgnoreCase("yes"), // 是否可为空【true-可为空， false-不可为空】
                            columns.getString("COLUMN_SIZE") + (decimal_digits > 0 ? "," + decimal_digits : "") // 长度
                    ));
                }
                table.setColumns(columnList);
                tables.add(table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return tables;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Table {
        private String tableName; // 表名
        private List<Column> columns; // 列集合
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Column {
        private String colName; // 字段名称
        private String type; // 类型
        private String comment; // 属性名称
        public boolean nullable; // 是否可为空【true-可为空， false-不可为空】
        private String length; // 长度

        public String getCamelColName() {
            return camel(colName.toLowerCase());
        }
    }


    public static String getTableName4J(String tableName) {
        String tabName = camel(tableName.toLowerCase());
        return tabName.substring(0, 1).toUpperCase() + tabName.substring(1);
    }
}
