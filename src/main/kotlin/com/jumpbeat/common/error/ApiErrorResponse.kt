package com.jumpbeat.common.error

data class ApiErrorResponse(
    val error: ErrorBody,
) {
    data class ErrorBody(
        val code: String,
        val message: String,
        val details: Any? = null,
    )

    companion object {
        fun of(errorCode: ErrorCode, details: Any? = null): ApiErrorResponse =
            ApiErrorResponse(
                error = ErrorBody(
                    code = errorCode.name,
                    message = errorCode.message,
                    details = details,
                ),
            )
    }
}
