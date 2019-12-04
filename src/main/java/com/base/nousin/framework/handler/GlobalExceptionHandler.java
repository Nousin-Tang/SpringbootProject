package com.base.nousin.framework.handler;

import com.base.nousin.framework.common.dto.ResultDto;
import com.base.nousin.framework.common.util.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理类
 *
 * @author tangwc
 * @since 2019/12/1
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public ResultDto defaultErrorHandler(HttpServletRequest req, HttpServletResponse res, Exception e) throws Exception {
        // 方便开发人员在控制台看到异常信息
        log.debug("系统异常",e);
        return ResultUtil.error();
    }
}