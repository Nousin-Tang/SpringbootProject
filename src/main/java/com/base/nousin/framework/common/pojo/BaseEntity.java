package com.base.nousin.framework.common.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Entity 基类
 *
 * @author tangwc
 * @since 2019/12/1
 */
@Getter
@Setter
public class BaseEntity {
    protected Integer delFlag; // 删除标记【0-正常，1-删除】
    protected Integer versionNo; // 版本号
    protected Date createDate; // 创建时间
    protected String createBy; // 创建人
    protected Date updateDate; // 更新时间
    protected String updateBy; // 更新人

    public void preInsert(){
        createDate = new Date();
        updateDate = createDate;
        createBy="1";
        updateBy=createBy;
    }
    public void preUpdate(){
        createDate = new Date();
        updateDate = createDate;
        createBy="1";
        updateBy=createBy;
    }
}
