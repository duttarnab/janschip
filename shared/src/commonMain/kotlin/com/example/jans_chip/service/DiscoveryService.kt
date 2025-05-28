package com.example.jans_chip.service

import com.example.jans_chip.model.ClientRegistrationRequest
import com.example.jans_chip.model.ClientRegistrationResponse
import com.example.jans_chip.model.OpenIdConfig
import com.example.jans_chip.model.Result
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.logging.Logger
import kotlinx.serialization.json.Json

class DiscoveryService(private val settings: Settings) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun isConfigStored(): Boolean {
        return settings.hasKey("issuer") && settings.hasKey("client_id")
    }

    fun clearConfig() {
        settings.remove("issuer")
        settings.remove("authorization_endpoint")
        settings.remove("token_endpoint")
        settings.remove("registration_endpoint")
        settings.remove("client_id")
        settings.remove("client_secret")
        settings.remove("client_name")
        settings.remove("scope")
        settings.remove("authorization_challenge_endpoint")
        settings.remove("userinfo_endpoint")
        settings.remove("scope")
        settings.remove("revocation_endpoint")
    }

    suspend fun fetchConfigAndRegisterClient(discoveryUrl: String): Result {
        return try {
            val configResponse: HttpResponse = client.get("$discoveryUrl/.well-known/openid-configuration")
            if (!configResponse.status.isSuccess()) {
                return com.example.jans_chip.model.Result(false, "Failed to fetch discovery config")
            }

            val configBody = configResponse.body<String>()
            val config = json.decodeFromString(OpenIdConfig.serializer(), configBody)

            // Save OpenID config
            settings.putString("issuer", config.issuer)
            settings.putString("authorization_endpoint", config.authorizationEndpoint)
            settings.putString("token_endpoint", config.tokenEndpoint)
            settings.putString("registration_endpoint", config.registrationEndpoint)
            settings.putString("authorization_challenge_endpoint", config.authorizationChallengeEndpoint)
            settings.putString("userinfo_endpoint", config.userinfoEndpoint)
            settings.putString("revocation_endpoint", config.revocationEndpoint)

            // Register client
            val registrationBody = json.encodeToString(ClientRegistrationRequest.serializer(), ClientRegistrationRequest(
                applicationType = "native",
                responseTypes = listOf("code"),
                grantTypes = listOf("authorization_code", "client_credentials"),
                tokenEndpointAuthMethod = "client_secret_basic",
                scope = "openid authorization_challenge profile",
                clientName = "MyKMPApp",
                redirectUris = listOf(discoveryUrl)
            ))

            val registrationResponse: HttpResponse = client.post(config.registrationEndpoint) {
                contentType(ContentType.Application.Json)
                setBody(registrationBody)
            }

            if (!registrationResponse.status.isSuccess()) {
                return com.example.jans_chip.model.Result(false, "Client registration failed: ${registrationResponse.status}")
            }

            val registrationBodyStr = registrationResponse.body<String>()
            val registration = json.decodeFromString(ClientRegistrationResponse.serializer(), registrationBodyStr)

            settings.putString("client_name", registration.clientName ?: "MyKMPApp")
            settings.putString("client_id", registration.clientId)
            settings.putString("client_secret", registration.clientSecret ?: "")
            settings.putString("scope", "openid authorization_challenge profile")

            com.example.jans_chip.model.Result(true)
        } catch (e: Exception) {
            com.example.jans_chip.model.Result(false, e.message)
        }
    }
}