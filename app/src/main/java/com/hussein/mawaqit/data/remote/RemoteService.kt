package com.hussein.mawaqit.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object RemoteService {

    private val defaultTrustManager: X509TrustManager? by lazy {
        try {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as java.security.KeyStore?)
            factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private val relaxedTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            try {
                defaultTrustManager?.checkClientTrusted(chain, authType)
            } catch (_: Exception) {
                // Fallback for client certificates
            }
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            try {
                defaultTrustManager?.checkServerTrusted(chain, authType)
            } catch (_: Exception) {
                // Fallback when server certification path validation fails (missing trust anchor or intermediate CA)
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return defaultTrustManager?.acceptedIssuers ?: emptyArray()
        }
    }

    val sslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(relaxedTrustManager), SecureRandom())
        }
    }

    private val client = HttpClient(Android) {
        engine {
            sslManager = { httpsURLConnection ->
                httpsURLConnection.sslSocketFactory = sslContext.socketFactory
                httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    fun getClient() = client
    fun close() = client.close()
}
