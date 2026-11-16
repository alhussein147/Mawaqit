package com.hussein.mawaqit.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object RemoteService {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                json = Json {
                    ignoreUnknownKeys = true
                },
                contentType = ContentType.Any,
            )
        }
    }

    fun getClient() = client
    fun close() = client.close()
}
