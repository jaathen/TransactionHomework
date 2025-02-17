package com.zhangboyu.transaction.exception;

import com.zhangboyu.transaction.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zhangboyu.transaction.enums.ErrorEnum.ARGUMENT_EXCEPTION;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ErrorResponse> handleTransactionException(TransactionException ex) {
        return ResponseEntity
                .ok(new ErrorResponse(
                        ex.getCode(),
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        Map<String, String> details = fieldErrors.stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));

        return ResponseEntity.badRequest().body(
                new ErrorResponse(
                        ARGUMENT_EXCEPTION.getCode(),
                        ARGUMENT_EXCEPTION.getMessage(),
                        details
                )
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(Exception ex) {
        log.error("系统异常: ", ex);
        return ResponseEntity.internalServerError().body(
                new ErrorResponse(
                        5000,
                        "系统繁忙，请稍后再试",
                        null
                )
        );
    }
}
