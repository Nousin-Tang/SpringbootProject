package com.base.nousin.framework.common.util;

import com.base.nousin.framework.common.pojo.ResultDto;

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
    public static <T> ResultDto<T> success(T t) {
        return success("操作成功", t);
    }


    /**
     * 成功快速返回
     *
     * @param message 返回消息
     * @param t 返回对象
     * @return ResultDto
     */
    public static <T> ResultDto<T> success(String message, T t) {
        return new ResultDto<>("0", "操作成功", t);
    }

    /**
     * 失败快速返回
     *
     * @param msg 信息
     * @return ResultDto
     */
    public static <T> ResultDto<T> fail(String msg) {
        return new ResultDto<>("9", msg, null);
    }

    /**
     * 系统异常
     *
     * @return ResultDto
     */
    public static <T> ResultDto<T> error() {
        return new ResultDto<>("-1", "系统异常", null);
    }
}
