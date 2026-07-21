package com.jumpbeat.common.error

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(exception.errorCode.status)
            .body(ApiErrorResponse.of(exception.errorCode, exception.details))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val details = exception.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "올바른 값을 입력해 주세요.")
        }
        val errorCode = ErrorCode.VALIDATION_FAILED

        return ResponseEntity
            .status(errorCode.status)
            .body(ApiErrorResponse.of(errorCode, details))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(): ResponseEntity<ApiErrorResponse> {
        val errorCode = ErrorCode.VALIDATION_FAILED

        return ResponseEntity
            .status(errorCode.status)
            .body(ApiErrorResponse.of(errorCode))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(): ResponseEntity<ApiErrorResponse> {
        val errorCode = ErrorCode.RESOURCE_NOT_FOUND

        return ResponseEntity
            .status(errorCode.status)
            .body(ApiErrorResponse.of(errorCode))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Unhandled exception", exception)
        val errorCode = ErrorCode.INTERNAL_SERVER_ERROR

        return ResponseEntity
            .status(errorCode.status)
            .body(ApiErrorResponse.of(errorCode))
    }
}
