package com.base.nousin.framework.handler;

import com.base.nousin.framework.common.dto.ResultDto;
import com.base.nousin.framework.common.util.ResultUtil;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 拦截 @Validated 校验失败的信息
 *
 * @author tangwc
 * @since 2019/12/1
 */
@ControllerAdvice
public class ValidationExceptionHandler {
    /**
     * 处理Get请求中 使用@Validated 验证路径中请求实体校验失败后抛出的异常，详情继续往下看代码
     *
     * @param e 异常参数
     * @return ResultDto
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ResultDto BindExceptionHandler(BindException e) {
        return ResultUtil.fail(e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(";")));
    }

    /**
     * 处理请求参数格式错误 @RequestParam 上validate失败后抛出的异常是javax.validation.ConstraintViolationException
     *
     * @param e 异常参数
     * @return ResultDto
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResultDto constraintViolationExceptionHandler(ConstraintViolationException e) {
        return ResultUtil.fail(e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.joining("；")));
    }

    /**
     * 处理请求参数格式错误 @RequestBody 上validate失败后抛出的异常是MethodArgumentNotValidException异常。
     *
     * @param e 异常参数
     * @return ResultDto
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResultDto methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        return ResultUtil.fail(e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("；")));
    }


    /**
     * 访问接口参数不全
     *
     * @param e 异常参数
     * @return ResultDto
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResultDto missingServletRequestParameterException(MissingServletRequestParameterException e) {
        return ResultUtil.fail("请求参数" + e.getParameterName() + "不存在");
    }

    /**
     * 请求方法不支持
     *
     * @param e 异常参数
     * @return ResultDto
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ResultDto httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String method = Optional.ofNullable(e.getSupportedHttpMethods()).orElseGet(HashSet::new).stream()
                .map(HttpMethod::name).collect(Collectors.joining("、"));
        return ResultUtil.fail(e.getMethod() + "请求方式不正确，仅支持" + method);
    }
}
