package com.example.jans_chip.service

import com.example.jans_chip.model.FlowResponse
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random


class AuthService(private val settings: Settings) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun authenticate(username: String, password: String): Result {
        val authorizationChallengeEndpoint = settings.getStringOrNull("authorization_challenge_endpoint") ?: return Result(false, "Missing authorization challenge endpoint")
        val tokenEndpoint = settings.getStringOrNull("token_endpoint") ?: return Result(false, "Missing token endpoint")
        val userInfoEndpoint = settings.getStringOrNull("userinfo_endpoint") ?: "${settings.getStringOrNull("issuer")}/userinfo"
        val clientId = settings.getStringOrNull("client_id") ?: return Result(false, "Missing client ID")
        val clientSecret = settings.getStringOrNull("client_secret") ?: return Result(false, "Missing client secret")
        val scope = settings.getStringOrNull("scope") ?: "openid profile"
        val issuer = settings.getStringOrNull("issuer") ?: "Missing Issuer"
        return try {
            // 1. Authorization challenge Request (agama native flow)
            val step1Response: HttpResponse = client.post(authorizationChallengeEndpoint) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                }
                setBody(
                    Parameters.build {
                        append("client_id", clientId)
                        append("use_auth_session", true.toString())
                        append("acr_values", "agama_challenge")
                        append("flow_name", "org.gluu.agama.pw.main")
                    }.formUrlEncode()
                )
            }

            val step1Json = step1Response.body<String>()
            val step1Data = json.decodeFromString(FlowResponse.serializer(), step1Json)

            if (step1Data.error != "flow_paused") {
                return Result(false, "Authorization challenge request failed at step 1: ${step1Data.error}, ${step1Data.engineError}")
            }
            //step 2
            val step2Response: HttpResponse = client.post(authorizationChallengeEndpoint) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                }
                setBody(
                    Parameters.build {
                        step1Data.authSession?.let { append("auth_session", it) }
                        append("use_auth_session", true.toString())
                        append("data", """{"username": "$username", "password": "$password"}""")
                    }.formUrlEncode()
                )
            }
            val step2Json = step2Response.body<String>()
            val step2Data = json.decodeFromString(FlowResponse.serializer(), step2Json)

            if (step2Data.error == "flow_paused" || step2Data.error != "flow_finished") {
                if (step2Data.flowPaused?.getValue("succeed")?.jsonPrimitive?.boolean == false) {
                    return Result(false, "Authorization challenge request failed at step 2: ${step2Data.error}, " +
                            "${step2Data.flowPaused?.getValue("errorMessage")?.jsonPrimitive?.toString()}")
                }
            }
            //step 3
            val step3Response: HttpResponse = client.post(authorizationChallengeEndpoint) {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                }
                setBody(
                    Parameters.build {
                        step2Data.authSession?.let { append("auth_session", it) }
                        append("use_auth_session", true.toString())
                    }.formUrlEncode()
                )
            }
            if (!step3Response.status.isSuccess()) {
                return Result(false, "Authorization challenge request failed in step 3: ${step3Response.status}")
            }
            val step3Json = step3Response.body<String>()
            val step3Data = json.decodeFromString(AuthorizationChallengeResponse.serializer(), step3Json)

            // 2. Token Request
            val tokenResponse: HttpResponse = client.post(tokenEndpoint) {
                headers {
                    append(
                        HttpHeaders.Authorization,
                        "Basic " + Base64.encode("$clientId:$clientSecret".toByteArray())
                    )
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                }
                setBody(
                    Parameters.build {
                        append("client_id", clientId)
                        append("code", step3Data.authorizationCode)
                        append("grant_type", "authorization_code")
                        append("redirect_uri", issuer)
                        append("scope", scope)
                    }.formUrlEncode()
                )
            }

            if (!tokenResponse.status.isSuccess()) {
                return Result(false, "Token request failed: ${tokenResponse.status}")
            }

            val tokenJson = tokenResponse.body<String>()
            val tokenData = json.decodeFromString(TokenResponse.serializer(), tokenJson)
            settings.putString("access_token", tokenData.accessToken)
            // 3. User Info Request
            val userInfoResponse: HttpResponse = client.post(userInfoEndpoint) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${tokenData.accessToken}")
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                }
                setBody(
                    Parameters.build {
                        append("access_token", "${tokenData.accessToken}")
                    }.formUrlEncode()
                )
            }

            if (!userInfoResponse.status.isSuccess()) {
                return Result(false, "Failed to fetch user info: ${userInfoResponse.status}")
            }

            val userInfo = userInfoResponse.body<String>()
            return Result(true, userInfo)

        } catch (e: Exception) {
            return Result(false, e.message)
        }
    }

    fun randomString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random(Random) }
            .joinToString("")
    }
}

@Serializable
data class AuthorizationChallengeResponse(
    @SerialName("authorization_code") val authorizationCode: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null
)

data class Result(val success: Boolean, val message: String? = null)
