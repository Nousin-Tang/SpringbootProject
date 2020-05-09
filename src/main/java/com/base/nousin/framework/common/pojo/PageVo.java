package com.base.nousin.framework.common.pojo;

import com.base.nousin.framework.common.util.DozerUtil;
import com.github.pagehelper.Page;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * Page 对象
 *
 * @author tangwc
 * @since 2019/12/2
 */
@Getter
@Setter
public class PageVo<T> {
    private int pageNum;
    private int pageSize;
    private long total;//总记录数
    //结果集
    protected List<T> list;

    /**
     * 包装Page对象
     *
     * @param list page结果
     */
    public PageVo(List<T> list) {
        if (CollectionUtils.isEmpty(list))
            return;
        this.list = list;
        if (list instanceof com.github.pagehelper.Page) {
            Page page = (Page) list;
            this.total = ((Page) list).getTotal();
            this.pageNum = page.getPageNum();
            this.pageSize = page.getPageSize();
        } else {
            this.pageNum = 1;
            this.pageSize = list.size();
            this.total = list.size();
        }
    }

    public <V> PageVo<V> changeType(Class<V> clazz){
        PageVo<V> pageVo = new PageVo<V>(DozerUtil.mapList(this.list, clazz));
        pageVo.setPageNum(this.pageNum);
        pageVo.setPageSize(this.pageSize);
        pageVo.setTotal(this.total);
        return pageVo;
    }
}
