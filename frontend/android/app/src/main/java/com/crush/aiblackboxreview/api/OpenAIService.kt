package com.crush.aiblackboxreview.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface `OpenAIService` {
    @POST("v1/chat/completions")
    suspend fun analyzeImage(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}