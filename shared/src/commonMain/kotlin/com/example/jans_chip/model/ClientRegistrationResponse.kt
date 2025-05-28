package com.example.jans_chip.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientRegistrationResponse(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("client_name") val clientName: String? = null
)