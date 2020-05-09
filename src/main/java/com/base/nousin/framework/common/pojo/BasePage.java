package com.base.nousin.framework.common.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * 基础Page
 *
 * @author Nousin
 * @since 2020/3/30
 */
@Getter
@Setter
public class BasePage {
    private int pageNum = 1;
    private int pageSize = 10;
}
