package org.example.flow

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform