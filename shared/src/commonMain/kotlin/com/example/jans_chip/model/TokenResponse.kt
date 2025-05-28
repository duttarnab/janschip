package com.example.jans_chip.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("id_token") val idToken: String,
    @SerialName("token_type") val tokenType: String? = null
)