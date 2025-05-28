package com.example.jans_chip.service

import com.russhwolf.settings.Settings
import io.ktor.client.*
import kotlin.io.encoding.Base64
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.Serializable
import kotlin.io.encoding.ExperimentalEncodingApi


class LogoutService(private val settings: Settings) {
    private val client = HttpClient()

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun logout(): LogoutResult {
        val revocationEndpoint = settings.getStringOrNull("revocation_endpoint")
            ?: return LogoutResult(success = false, message = "No revocation endpoint configured")

        val clientId = settings.getStringOrNull("client_id")
            ?: return LogoutResult(success = false, message = "Missing client ID")

        val clientSecret = settings.getStringOrNull("client_secret")
            ?: return LogoutResult(success = false, message = "Missing client secret")

        val accessToken = settings.getStringOrNull("access_token")
            ?: return LogoutResult(success = false, message = "No access token to revoke")

        return try {
            val response: HttpResponse = client.post(revocationEndpoint) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                    append(
                        HttpHeaders.Authorization,
                        "Basic " + Base64.encode("$clientId:$clientSecret".toByteArray())
                    )
                }
                setBody(
                    Parameters.build {
                        append("token", accessToken)
                        append("token_type_hint", "access_token")
                    }.formUrlEncode()
                )
            }

            if (response.status.isSuccess()) {
                settings.remove("access_token")
                LogoutResult(success = true, message = "Access token revoked")
            } else {
                LogoutResult(success = false, message = "Revocation failed: ${response.status}")
            }
        } catch (e: Exception) {
            LogoutResult(success = false, message = "Exception: ${e.message}")
        }
    }
}

@Serializable
data class LogoutResult(val success: Boolean, val message: String)