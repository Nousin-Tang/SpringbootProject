package com.base.nousin.framework.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Page 对象
 *
 * @author tangwc
 * @since 2019/12/2
 */
@Getter
@Setter
@AllArgsConstructor
public class PageDto {
    private int pageNum;
    private int pageSize;
}
