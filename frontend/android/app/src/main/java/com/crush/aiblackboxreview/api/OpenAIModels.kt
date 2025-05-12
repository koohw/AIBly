package com.crush.aiblackboxreview.api

// 요청 모델
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int = 300
)

data class Message(
    val role: String,
    val content: List<Content>
)

data class Content(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

// 응답 모델
data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: ResponseMessage,
    val index: Int
)

data class ResponseMessage(
    val role: String,
    val content: String
)