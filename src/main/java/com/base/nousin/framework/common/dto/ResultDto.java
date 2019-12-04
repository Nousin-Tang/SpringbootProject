package com.base.nousin.framework.common.dto;

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
public class ResultDto {
    private String code;
    private String message;
    private Object data;
}
