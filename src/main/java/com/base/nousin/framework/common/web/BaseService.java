package com.base.nousin.framework.common.web;

import com.base.nousin.framework.common.dto.PageDto;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service 基类
 *
 * @author tangwc
 * @since 2019/12/2
 */
public abstract class BaseService<D extends BaseDao<T>, T extends BaseEntity> {
    /**
     * 持久层对象
     */
    @Autowired
    protected D dao;

    /**
     * 获取单条数据
     *
     * @param id
     * @return
     */
    public T get(String id) {
        return dao.get(id);
    }

    /**
     * 获取单条数据
     *
     * @param entity
     * @return
     */
    public T get(T entity) {
        return dao.get(entity);
    }

    /**
     * 查询列表数据
     *
     * @param entity
     * @return
     */
    public List<T> findList(T entity) {
        return dao.findList(entity);
    }

    /**
     * 分页查询列表数据
     *
     * @param entity
     * @return
     */
    public PageInfo<T> findPage(T entity, PageDto page) {
        PageHelper.startPage(page.getPageNum(), page.getPageSize());
        return new PageInfo<T>(dao.findList(entity));
    }

    /**
     * 修改
     * @param entity
     * @return
     */
    @Transactional
    public int insert(T entity) {
        return dao.insert(entity);
    }

    /**
     * 更新数据
     * @param entity 参数
     * @return 更新记录的行数
     */
    @Transactional
    public int update(T entity) {
        return dao.update(entity);
    }

    /**
     * 删除数据
     * @param entity 参数
     * @return 被删除记录的行数
     */
    @Transactional
    public int delete(T entity) {
        return dao.delete(entity);
    }

}
