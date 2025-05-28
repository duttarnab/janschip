package com.example.jans_chip.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenIdConfig(
    val issuer: String,
    @SerialName("authorization_endpoint") val authorizationEndpoint: String,
    @SerialName("token_endpoint") val tokenEndpoint: String,
    @SerialName("registration_endpoint") val registrationEndpoint: String,
    @SerialName("userinfo_endpoint") val userinfoEndpoint: String,
    @SerialName("authorization_challenge_endpoint") val authorizationChallengeEndpoint: String,
    @SerialName("revocation_endpoint") val revocationEndpoint: String
)