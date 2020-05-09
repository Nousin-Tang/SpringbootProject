package com.base.nousin.framework.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 结果返回类
 *
 * @author tangwc
 * @since 2019/11/27
 */
@Getter
@Setter
@AllArgsConstructor
public class ResultDto<T> {
    private String code;
    private String message;
    private T data;
}
