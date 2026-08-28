package com.bigfalkon.karakterevreni.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase e-posta/parola girişi (Identity Toolkit REST). Dönen idToken,
 * gizli evren kilidi ve admin panelindeki Firestore yazmaları için kullanılır.
 */
object Auth {

    private const val API_KEY = "AIzaSyDrEU_QEuvf960ZPxo29n6x8N9K7yGIkCQ"
    private const val ENDPOINT =
        "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$API_KEY"

    suspend fun signIn(email: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("returnSecureToken", true)
                }.toString()

                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("Content-Type", "application/json")
                }
                try {
                    conn.outputStream.use { it.write(payload.toByteArray()) }
                    if (conn.responseCode !in 200..299) {
                        val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        val message = runCatching {
                            JSONObject(err).getJSONObject("error").optString("message")
                        }.getOrNull()
                        error(
                            when (message) {
                                "EMAIL_NOT_FOUND", "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" ->
                                    "E-posta veya parola hatalı."
                                "USER_DISABLED" -> "Bu hesap devre dışı."
                                "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Çok fazla deneme. Biraz sonra dene."
                                else -> "Giriş yapılamadı."
                            }
                        )
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    JSONObject(body).optString("idToken")
                } finally {
                    conn.disconnect()
                }
            }
        }
}
