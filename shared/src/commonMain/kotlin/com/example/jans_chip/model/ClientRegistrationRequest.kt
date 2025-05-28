package com.example.jans_chip.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientRegistrationRequest(
    @SerialName("application_type") val applicationType: String,
    @SerialName("response_types") val responseTypes: List<String>,
    @SerialName("grant_types") val grantTypes: List<String>,
    @SerialName("redirect_uris") val redirectUris: List<String>,
    @SerialName("token_endpoint_auth_method") val tokenEndpointAuthMethod: String,
    val scope: String,
    @SerialName("client_name") val clientName: String
)