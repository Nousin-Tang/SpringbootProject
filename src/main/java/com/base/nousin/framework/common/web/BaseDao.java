package com.base.nousin.framework.common.web;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Dao 基类
 *
 * @author tangwc
 * @since 2019/12/1
 */
@Repository
public interface BaseDao<T> {
    /**
     * 获取单条数据
     *
     * @param id id
     * @return 查询结果
     */
    T get(String id);

    /**
     * 获取单条数据
     *
     * @param entity 实体参数
     * @return 查询结果
     */
    T get(T entity);

    /**
     * 查询数据列表
     *
     * @param entity 实体参数
     * @return 查询结果
     */
    List<T> findList(T entity);

    /**
     * 插入数据
     *
     * @param entity 实体参数
     * @return 新增结果
     */
    int insert(T entity);

    /**
     * 更新数据
     *
     * @param entity 实体参数
     * @return 更新结果
     */
    int update(T entity);

    /**
     * 删除数据（一般为逻辑删除，更新del_flag字段为1）
     *
     * @param entity 实体参数
     * @return 删除结果
     */
    int delete(T entity);
}
