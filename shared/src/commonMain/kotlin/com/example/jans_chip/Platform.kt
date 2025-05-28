package com.example.jans_chip

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform