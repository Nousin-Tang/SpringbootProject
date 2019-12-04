package com.base.nousin.framework.common.util;

import com.base.nousin.framework.common.dto.ResultDto;

/**
 * 返回值工具类
 *
 * @author tangwc
 * @since 2019/11/29
 */
public class ResultUtil {
    /**
     * 成功快速返回
     *
     * @param t 返回对象
     * @return ResultDto
     */
    public static ResultDto success(Object t) {
        return new ResultDto("0", "操作成功", t);
    }

    /**
     * 失败快速返回
     * @param msg 信息
     * @return ResultDto
     */
    public static ResultDto fail(String msg) {
        return new ResultDto("9", msg, null);
    }

    /**
     * 系统异常
     * @return ResultDto
     */
    public static ResultDto error() {
        return new ResultDto("99",  "系统异常", null);
    }
}
