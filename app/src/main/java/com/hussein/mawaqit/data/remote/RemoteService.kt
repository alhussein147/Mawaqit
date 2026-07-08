package com.hussein.mawaqit.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object RemoteService {
   private val client = HttpClient(){
        install(ContentNegotiation){
            json(Json{
                ignoreUnknownKeys = true
            })
        }
    }

    fun getClient() = client
    fun close() = client.close()
}