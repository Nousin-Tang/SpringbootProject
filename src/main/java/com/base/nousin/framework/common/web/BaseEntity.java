package com.base.nousin.framework.common.web;

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
    private String delFlag; // 删除标记【0-正常，1-删除】
    private Integer version; // 版本号
    private Date createDate; // 创建时间
    private String createBy; // 创建人
    private Date updateDate; // 更新时间
    private String updateBy; // 更新人
}
