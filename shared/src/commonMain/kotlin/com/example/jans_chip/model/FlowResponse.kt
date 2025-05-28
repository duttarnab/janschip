package com.example.jans_chip.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FlowResponse(
    @SerialName("auth_session") val authSession: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("flow_finished") val flowFinished: JsonObject? = null,
    @SerialName("flow_paused") val flowPaused: JsonObject? = null,
    @SerialName("engine_error") val engineError: JsonObject? = null
)
