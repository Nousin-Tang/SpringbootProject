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
    private static final String URL = "jdbc:mysql://localhost:3306/nousin?useUnicode=true&characterEncoding=utf8&useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    private static final String author = "tangwc"; // 作者
    private static final boolean showColumnLength = false; // 备注显示字段长度

    private static final String subEntityPath = "entity";
    private static final String subMapperPath = "mapper";
    private static final String subDaoPath = "dao";
    private static final String subServicePath = "service";
    private static final String subControllerPath = "controller";
    private static final String subDtoPath = "dto";

    private static final String[] strAttr = {"delFlag", "versionNo", "createDate", "createBy", "updateDate", "updateBy"};

    private static final List<String> genericEntityAttributes;

    static {
        genericEntityAttributes = Arrays.asList(strAttr);
        List<String> genericEntityAttributesWithId = new ArrayList<>(genericEntityAttributes);
        genericEntityAttributesWithId.add("id");
    }

    public static void main(String[] args) {
        try {
            String pkg = "com.base.nousin.temp";// 生成文件存在方的包
            String path = getProjectPath() + "src/main/java/" + pkg.replace(".", "/") + "/";// 生成文件存放的路径
            // /Users/unnous/project/git-clone/SpringbootProject/src/main/java/com/base/nousin/temp/
            System.out.println(path);
            // List<String> tableNameList = Collections.singletonList("t_product_info_ingco");
            for (Table table : getTables()) {
                // if (!tableNameList.contains(table.getTableName())) { return; }
                generateEntityFile(path, table, pkg);
                generateDtoFile(path, table, pkg);
                generateXmlFile(path, table, pkg);
                generateDaoFile(path, table, pkg);
                generateServiceFile(path, table, pkg);
                generateControllerFile(path, table, pkg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 项目路径
     *
     * @return /Users/unnous/project/git-clone/SpringbootProject
     */
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

        File file = new File(path + subEntityPath + File.separator + entityName + ".java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".").append(subEntityPath).append(";\n\n");
        if (genericTable(table)) sb.append("import com.base.nousin.framework.common.pojo.BaseEntity;\n");
        sb.append("import lombok.Getter;\n");
        sb.append("import lombok.Setter;\n");
        sb.append("import lombok.ToString;\n");
        sb.append(getImportJavaClass(table.getColumns().stream()
                .filter(e -> !genericEntityAttributes.contains(e.getCamelColName()))
                .map(Column::getType).collect(Collectors.toSet())));
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
        sb.append("public class ").append(entityName);
        if (genericTable(table))
            sb.append(" extends BaseEntity {\n");
        else
            sb.append(" {\n");
        for (Column column : table.getColumns()) {
            if (genericEntityAttributes.contains(column.getCamelColName()))
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
        log.info("====文件[ {} ]已生成====", path + subEntityPath + File.separator + entityName + ".java");
    }

    /**
     * 是否是整张规则的表
     *
     * @param table 表
     * @return true / false
     */
    private static boolean genericTable(Table table) {
        List<Column> columns = table.getColumns();
        for (Column column : columns) {
            if (genericEntityAttributes.contains(column.getCamelColName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成Dto
     *
     * @param path  路径
     * @param table 表
     * @param pkg   包名
     * @throws Exception
     */
    public static void generateDtoFile(String path, Table table, String pkg) throws Exception {
        String entityName = getTableName4J(table.getTableName());
        String[] dtoSuffixList = {"PageDto", "InsertDto", "UpdateDto", "Dto"};
        for (String dtoSuffix : dtoSuffixList) {
            File file = new File(path + subDtoPath + File.separator + entityName + dtoSuffix + ".java");
            // 判断文件是否存在，存在则返回
            if (createFileIfAbsent(file)) {

                StringBuilder sb = new StringBuilder();
                sb.append("package ").append(pkg).append(".").append(subDtoPath).append(";\n\n");
                sb.append("import lombok.Getter;\n");
                sb.append("import lombok.Setter;\n");
                sb.append(getImportJavaClass(table.getColumns().stream()
                        .filter(e -> !genericEntityAttributes.contains(e.getCamelColName()))
                        .map(Column::getType).collect(Collectors.toSet())));
                sb.append("\n\n");
                sb.append("/**\n");
                sb.append(" * TODO\n");
                sb.append(" *\n");
                sb.append(" * @author ").append(author).append("\n");
                sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
                sb.append(" */\n");
                sb.append("@Getter\n");
                sb.append("@Setter\n");
                sb.append("public class ").append(entityName).append(dtoSuffix).append(" {\n");
                for (Column column : table.getColumns()) {
                    if (genericEntityAttributes.contains(column.getCamelColName()))
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
                log.info("====文件[ {} ]已生成====", path + subEntityPath + File.separator + entityName + dtoSuffix + ".java");
            }
        }

        String pageRequestDto = "PageRequestDto";
        File file1 = new File(path + subDtoPath + File.separator + entityName + pageRequestDto + ".java");
        // 判断文件是否存在，存在则返回
        if (createFileIfAbsent(file1)) {

            StringBuilder sb1 = new StringBuilder();
            sb1.append("package ").append(pkg).append(".").append(subDtoPath).append(";\n\n");
            sb1.append("import com.base.nousin.framework.common.pojo.BasePage;\n");
            sb1.append("import lombok.Getter;\n");
            sb1.append("import lombok.Setter;\n");
            sb1.append(getImportJavaClass(table.getColumns().stream()
                    .filter(e -> !genericEntityAttributes.contains(e.getCamelColName()))
                    .map(Column::getType).collect(Collectors.toSet())));
            sb1.append("\n\n");
            sb1.append("/**\n");
            sb1.append(" * TODO\n");
            sb1.append(" *\n");
            sb1.append(" * @author ").append(author).append("\n");
            sb1.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
            sb1.append(" */\n");
            sb1.append("@Getter\n");
            sb1.append("@Setter\n");
            sb1.append("public class ").append(entityName).append(pageRequestDto).append(" extends BasePage {\n");
            for (Column column : table.getColumns()) {
                if (genericEntityAttributes.contains(column.getCamelColName()))
                    continue;
                sb1.append("\tprivate ").append(getJavaClass(column.getType())).append(" ").append(column.getCamelColName()).append("; // ")
                        .append(column.getComment());
                if (showColumnLength && (isString(column.getType()) || isDecimal(column.getType())))
                    sb1.append(" 字段长度[").append(column.getLength()).append("]");
                sb1.append("\n");
            }
            sb1.append("}\n");

            // 写入文件
            FileOutputStream out1 = new FileOutputStream(file1);
            out1.write(sb1.toString().getBytes(StandardCharsets.UTF_8));
            out1.close();
            log.info("====文件[ {} ]已生成====", path + subEntityPath + File.separator + entityName + pageRequestDto + ".java");
        }

        String deleteDto = "DeleteDto";
        File file2 = new File(path + subDtoPath + File.separator + entityName + deleteDto + ".java");
        // 判断文件是否存在，存在则返回
        if (createFileIfAbsent(file2)) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("package ").append(pkg).append(".").append(subDtoPath).append(";\n\n");
            sb2.append("import com.base.nousin.framework.common.pojo.BaseDelete;\n");
            sb2.append("import lombok.Getter;\n");
            sb2.append("import lombok.Setter;\n");
            sb2.append(getImportJavaClass(table.getColumns().stream()
                    .filter(e -> !genericEntityAttributes.contains(e.getCamelColName()))
                    .map(Column::getType).collect(Collectors.toSet())));
            sb2.append("\n\n");
            sb2.append("/**\n");
            sb2.append(" * TODO\n");
            sb2.append(" *\n");
            sb2.append(" * @author ").append(author).append("\n");
            sb2.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
            sb2.append(" */\n");
            sb2.append("@Getter\n");
            sb2.append("@Setter\n");
            sb2.append("public class ").append(entityName).append(deleteDto).append(" extends BaseDelete {\n");
            for (Column column : table.getColumns()) {
                if (genericEntityAttributes.contains(column.getCamelColName()))
                    continue;
                sb2.append("\tprivate ").append(getJavaClass(column.getType())).append(" ").append(column.getCamelColName()).append("; // ")
                        .append(column.getComment());
                if (showColumnLength && (isString(column.getType()) || isDecimal(column.getType())))
                    sb2.append(" 字段长度[").append(column.getLength()).append("]");
                sb2.append("\n");
            }
            sb2.append("}\n");

            // 写入文件
            FileOutputStream out2 = new FileOutputStream(file2);
            out2.write(sb2.toString().getBytes(StandardCharsets.UTF_8));
            out2.close();
            log.info("====文件[ {} ]已生成====", path + subEntityPath + File.separator + entityName + deleteDto + ".java");
        }

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


        File file = new File(path + subMapperPath + File.separator + entityName + "Dao.xml");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        String nameSpace = pkg + "." + subDaoPath + "." + entityName;
        String resultType = pkg + "." + subEntityPath + "." + entityName;
        String pageDto = pkg + "." + subDtoPath + "." + entityName + "PageDto";
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sb.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        sb.append("<mapper namespace=\"").append(nameSpace).append("Dao\">\n");
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
        sb.append("\t\tselect \n");
        sb.append("\t\t\t<include refid=\"baseColumn\"/>\n");
        sb.append("\t\tfrom ").append(tableName).append(" a \n");
        sb.append("\t\twhere a.del_flag=0 and a.id = #{id} \n");
        sb.append("\t</select>\n");
        sb.append("\n");

        // page
        sb.append("\t<select id=\"page\" resultType=\"").append(pageDto).append("\">\n");
        sb.append("\t\tselect\n");
        sb.append("\t\t\t<include refid=\"baseColumn\"/>\n");
        sb.append("\t\tfrom ").append(tableName).append(" a \n");
        sb.append("\t\twhere a.del_flag=0\n");
        sb.append("\t\torder by a.id\n");
        sb.append("\t</select>\n");
        sb.append("\n");

        // list
        sb.append("\t<select id=\"list\" resultType=\"").append(resultType).append("\">\n");
        sb.append("\t\tselect\n");
        sb.append("\t\t\t<include refid=\"baseColumn\"/>\n");
        sb.append("\t\tfrom ").append(tableName).append(" a \n");
        sb.append("\t\twhere a.del_flag=0\n");
        sb.append("\t\torder by a.id\n");
        sb.append("\t</select>\n");
        sb.append("\n");

        // insert
        sb.append("\t<insert id=\"insert\">\n");
        sb.append("\t\tinsert into ").append(tableName).append("(\n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            String columnName = table.getColumns().get(i).getColName().toLowerCase();
            if ("id".equalsIgnoreCase(columnName)) continue;
            sb.append("\t\t\t").append(columnName);
            if (i == table.getColumns().size() - 1)
                sb.append("\n");
            else
                sb.append(",\n");
        }
        sb.append("\t\t) values (\n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            String columnName = column.getColName().toLowerCase();
            String camelColName = column.getCamelColName();
            if ("id".equalsIgnoreCase(columnName)) continue;

            if ("del_flag".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t0");
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
        sb.append("\t\tupdate ").append(tableName).append(" SET \n");
        for (int i = 0; i < table.getColumns().size(); i++) {
            Column column = table.getColumns().get(i);
            String columnName = column.getColName().toLowerCase();
            if ("id".equalsIgnoreCase(columnName) || "create_by".equalsIgnoreCase(columnName) || "create_date".equalsIgnoreCase(columnName))
                continue;

            if ("del_flag".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t").append(columnName).append(" = 0");
            } else if ("version_no".equalsIgnoreCase(columnName)) {
                sb.append("\t\t\t").append(columnName).append(" = ").append(columnName).append(" + 1");
            } else {
                sb.append("\t\t\t").append(columnName).append(" = #{").append(column.getCamelColName()).append("}");
            }
            if (i == table.getColumns().size() - 1)
                sb.append("\n");
            else
                sb.append(",\n");
        }
        sb.append("\t\twhere id = #{id} and version_no = #{versionNo}\n");
        sb.append("\t</update>\n");
        sb.append("\n");
        // delete
        sb.append("\t<update id=\"delete\">\n");
        sb.append("\t\tupdate ").append(tableName).append(" SET del_flag = 1, version_no = version_no + 1 WHERE id = #{id} and version_no = #{versionNo}\n");
        sb.append("\t</update>\n");
        sb.append("</mapper>\n");
        log.info("====文件[ {} ]已生成====", path + subMapperPath + File.separator + entityName + "Dao.xml");
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

        File file = new File(path + subDaoPath + File.separator + entityName + "Dao.java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".").append(subDaoPath).append(";\n\n");
        sb.append("import ").append(pkg).append(".").append(subEntityPath).append(".").append(entityName).append(";\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("PageRequestDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("PageDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("DeleteDto;\n");
        sb.append("import org.springframework.stereotype.Repository;\n");
        sb.append("import java.util.List;\n");
        sb.append("\n\n");
        sb.append("/**\n");
        sb.append(" * TODO\n");
        sb.append(" *\n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
        sb.append(" */\n");
        sb.append("@Repository\n");
        sb.append("public interface ").append(entityName).append("Dao {\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 获取单条数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param id id\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\t").append(entityName).append(" get(Integer id);\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 查询数据列表\n");
        sb.append("\t *\n");
        sb.append("\t * @param requestPageDto 实体参数\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\tList<").append(entityName).append("PageDto> page(").append(entityName).append("PageRequestDto requestPageDto);\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 查询数据列表\n");
        sb.append("\t *\n");
        sb.append("\t * @param entity 实体参数\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\tList<").append(entityName).append("> list(").append(entityName).append(" entity);\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 插入数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param entity 实体参数\n");
        sb.append("\t * @return 新增结果\n");
        sb.append("\t */\n");
        sb.append("\tint insert(").append(entityName).append(" entity);\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 更新数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param entity 实体参数\n");
        sb.append("\t * @return 更新结果\n");
        sb.append("\t */\n");
        sb.append("\tint update(").append(entityName).append(" entity);\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 删除数据（一般为逻辑删除，更新del_flag字段为1）\n");
        sb.append("\t *\n");
        sb.append("\t * @param entity 实体参数\n");
        sb.append("\t * @return 删除结果\n");
        sb.append("\t */\n");
        sb.append("\tint delete(").append(entityName).append("DeleteDto entity);");
        sb.append("\t\n");
        sb.append("}\n");
        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + subDaoPath + File.separator + entityName + "Dao.java");
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
        String entityDaoName = entityName.substring(0, 1).toLowerCase() + entityName.substring(1) + "Dao";
        File file = new File(path + subServicePath + File.separator + entityName + "Service.java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".").append(subServicePath).append(";\n");
        sb.append("\n");
        sb.append("import com.github.pagehelper.PageHelper;\n");
        sb.append("import com.base.nousin.framework.common.pojo.PageVo;\n");
        sb.append("import com.base.nousin.framework.common.util.DozerUtil;\n");
        sb.append("import ").append(pkg).append(".").append(subEntityPath).append(".").append(entityName).append(";\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("PageRequestDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("PageDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("Dto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("InsertDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("UpdateDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("DeleteDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDaoPath).append(".").append(entityName).append("Dao;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        sb.append("import org.springframework.stereotype.Service;\n");
        sb.append("import org.springframework.transaction.annotation.Transactional;\n");
        sb.append("\n");
        sb.append("import java.util.List;\n");
        sb.append("\n\n");
        sb.append("/**\n");
        sb.append(" * TODO\n");
        sb.append(" *\n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" * @since ").append(LocalDate.now().toString().replace("-", "/")).append("\n");
        sb.append(" */\n");
        sb.append("@Service\n");
        sb.append("public class ").append(entityName).append("Service  {\n");
        sb.append("\n");
        sb.append("\t@Autowired\n");
        sb.append("\tprivate ").append(entityName).append("Dao ").append(entityDaoName).append(";\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 获取单条数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param id id\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\tpublic ").append(entityName).append("Dto get(Integer id) {\n");
        sb.append("\t\treturn DozerUtil.map(").append(entityDaoName).append(".get(id), ").append(entityName).append("Dto.class);\n");
        sb.append("\t}\n\n");
        sb.append("\t/**\n");
        sb.append("\t * 查询数据列表\n");
        sb.append("\t *\n");
        sb.append("\t * @param pageRequestDto 实体参数\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\tpublic PageVo<").append(entityName).append("PageDto> findPage(").append(entityName).append("PageRequestDto pageRequestDto) {\n");
        sb.append("\t\tPageHelper.startPage(pageRequestDto.getPageNum(), pageRequestDto.getPageSize());\n");
        sb.append("\t\treturn new PageVo<>(").append(entityDaoName).append(".page(pageRequestDto));\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 查询数据列表\n");
        sb.append("\t *\n");
        sb.append("\t * @param pageRequestDto 实体参数\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\tpublic List<").append(entityName).append("> list(").append(entityName).append(" entity) {\n");
        sb.append("\t\treturn ").append(entityDaoName).append(".list(entity);\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 插入数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param dto 参数\n");
        sb.append("\t * @return 新增结果\n");
        sb.append("\t */\n");
        sb.append("\t@Transactional\n");
        sb.append("\tpublic int insert(").append(entityName).append("InsertDto dto) {\n");
        sb.append("\t\t").append(entityName).append(" entity = DozerUtil.map(dto, ").append(entityName).append(".class);\n");
        sb.append("\t\tentity.preInsert();\n");
        sb.append("\t\treturn ").append(entityDaoName).append(".insert(entity);\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 更新数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param dto 参数\n");
        sb.append("\t * @return 更新结果\n");
        sb.append("\t */\n");
        sb.append("\t@Transactional\n");
        sb.append("\tpublic int update(").append(entityName).append("UpdateDto dto) {\n");
        sb.append("\t\t").append(entityName).append(" entity = DozerUtil.map(dto, ").append(entityName).append(".class);\n");
        sb.append("\t\tentity.preUpdate();\n");
        sb.append("\t\treturn ").append(entityDaoName).append(".update(entity);\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 删除数据（一般为逻辑删除，更新del_flag字段为1）\n");
        sb.append("\t *\n");
        sb.append("\t * @param entity 实体参数\n");
        sb.append("\t * @return 删除结果\n");
        sb.append("\t */\n");
        sb.append("\t@Transactional\n");
        sb.append("\tpublic int delete(").append(entityName).append("DeleteDto entity) {\n");
        sb.append("\t\treturn ").append(entityDaoName).append(".delete(entity);\n");
        sb.append("\t}\n");
        sb.append("}");
        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + subServicePath + File.separator + entityName + "Service.java");
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
        String entityServiceName = entityName.substring(0, 1).toLowerCase() + entityName.substring(1) + "Service";
        File file = new File(path + subControllerPath + File.separator + entityName + "Controller.java");
        // 判断文件是否存在，存在则返回
        if (!createFileIfAbsent(file)) return;

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".").append(subControllerPath).append(";\n");
        sb.append("\n");
        sb.append("import ").append(pkg).append(".").append(subServicePath).append(".").append(entityName).append("Service;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("PageRequestDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("PageDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("Dto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("InsertDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("UpdateDto;\n");
        sb.append("import ").append(pkg).append(".").append(subDtoPath).append(".").append(entityName).append("DeleteDto;\n");
        sb.append("import com.base.nousin.framework.common.pojo.PageVo;\n");
        sb.append("import com.base.nousin.framework.common.pojo.ResultDto;\n");
        sb.append("import com.base.nousin.framework.common.util.ResultUtil;\n");
        sb.append("import lombok.extern.slf4j.Slf4j;\n");
        sb.append("import org.springframework.web.bind.annotation.RestController;\n");
        sb.append("import org.springframework.web.bind.annotation.RequestMapping;\n");
        sb.append("import org.springframework.web.bind.annotation.GetMapping;\n");
        sb.append("import org.springframework.web.bind.annotation.PostMapping;\n");
        sb.append("import org.springframework.web.bind.annotation.PutMapping;\n");
        sb.append("import org.springframework.web.bind.annotation.DeleteMapping;\n");
        sb.append("import org.springframework.web.bind.annotation.PathVariable;\n");
        sb.append("import org.springframework.web.bind.annotation.RequestParam;\n");
        sb.append("import org.springframework.beans.factory.annotation.Autowired;\n");
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
        sb.append("\t@Autowired\n");
        sb.append("\tprivate ").append(entityName).append("Service service;\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 获取明细数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param id id\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\t@GetMapping(\"{id}\")\n");
        sb.append("\tpublic ResultDto<").append(entityName).append("Dto> get(@PathVariable(\"id\") Integer id) {\n");
        sb.append("\t\treturn ResultUtil.success(service.get(id));\n");
        sb.append("\t}\n");
        sb.append("\t\n");
        sb.append("\t/**\n");
        sb.append("\t * 查询数据列表\n");
        sb.append("\t *\n");
        sb.append("\t * @param pageRequestDto 实体参数\n");
        sb.append("\t * @return 查询结果\n");
        sb.append("\t */\n");
        sb.append("\t@GetMapping\n");
        sb.append("\tpublic ResultDto<PageVo<").append(entityName).append("PageDto>> page(").append(entityName).append("PageRequestDto pageRequestDto) {\n");
        sb.append("\t\treturn ResultUtil.success(service.findPage(pageRequestDto));\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 插入数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param dto 参数\n");
        sb.append("\t * @return 新增结果\n");
        sb.append("\t */\n");
        sb.append("\t@PostMapping\n");
        sb.append("\tpublic ResultDto<Integer> insert(").append(entityName).append("InsertDto dto) {\n");
        sb.append("\t\treturn ResultUtil.success(service.insert(dto));\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 更新数据\n");
        sb.append("\t *\n");
        sb.append("\t * @param dto 参数\n");
        sb.append("\t * @return 更新结果\n");
        sb.append("\t */\n");
        sb.append("\t@PutMapping\n");
        sb.append("\tpublic ResultDto<Integer> update(").append(entityName).append("UpdateDto dto) {\n");
        sb.append("\t\treturn ResultUtil.success(service.update(dto));\n");
        sb.append("\t}\n");
        sb.append("\n");
        sb.append("\t/**\n");
        sb.append("\t * 删除数据（一般为逻辑删除，更新del_flag字段为1）\n");
        sb.append("\t *\n");
        sb.append("\t * @param id ID\n");
        sb.append("\t * @param versionNo 版本号\n");
        sb.append("\t * @return 删除结果\n");
        sb.append("\t */\n");
        sb.append("\t@DeleteMapping(\"{id}\")\n");
        sb.append("\tpublic ResultDto<Integer> delete(@PathVariable(\"id\") Integer id, @RequestParam Integer versionNo) {\n");
        sb.append("\t\t").append(entityName).append("DeleteDto deleteDto = new ").append(entityName).append("DeleteDto();\n");
        sb.append("\t\tdeleteDto.setId(id);\n");
        sb.append("\t\tdeleteDto.setVersionNo(versionNo);\n");
        sb.append("\t\treturn ResultUtil.success(service.delete(deleteDto));\n");
        sb.append("\t}\n");
        sb.append("}");

        // 写入文件
        FileOutputStream out = new FileOutputStream(file);
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.close();
        log.info("====文件[ {} ]已生成====", path + subControllerPath + File.separator + entityName + "Controller.java");
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
