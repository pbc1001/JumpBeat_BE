package com.jumpbeat.common.error

class BusinessException(
    val errorCode: ErrorCode,
    val details: Any? = null,
) : RuntimeException(errorCode.message)
