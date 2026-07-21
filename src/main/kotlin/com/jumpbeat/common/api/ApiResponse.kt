package com.jumpbeat.common.api

data class ApiResponse<T>(
    val data: T,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(data)
    }
}
