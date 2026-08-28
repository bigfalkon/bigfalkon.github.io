package com.bigfalkon.karakterevreni.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.TimeZone

/**
 * Firestore REST istemcisi. Firebase SDK'sı (ve google-services.json) yerine
 * doğrudan REST kullanıyoruz: koleksiyonlar herkese açık okunabilir.
 */
object Firestore {

    private const val PROJECT = "karaktergalerisi-dc337"
    private const val API_KEY = "AIzaSyDrEU_QEuvf960ZPxo29n6x8N9K7yGIkCQ"
    private const val BASE =
        "https://firestore.googleapis.com/v1/projects/$PROJECT/databases/(default)/documents"

    data class Snapshot(
        val characters: List<Character>,
        val dismissed: List<Character>,
        val universes: List<AlternativeUniverse>
    ) {
        val isEmpty: Boolean get() = characters.isEmpty() && dismissed.isEmpty()
    }

    suspend fun load(context: Context): Snapshot = coroutineScope {
        val chars = async(Dispatchers.IO) { fetchCollection("characters") }
        val dism = async(Dispatchers.IO) { fetchCollection("dismissed") }
        val aus = async(Dispatchers.IO) { fetchCollection("alternativeUniverses") }
        val snapshot = Snapshot(
            characters = chars.await().map { parseCharacter(it, dismissed = false) },
            dismissed = dism.await().map { parseCharacter(it, dismissed = true) },
            universes = aus.await().map { parseUniverse(it) }
        )
        writeCache(context, snapshot)
        snapshot
    }

    // ─── Ağ ───────────────────────────────────────────────────────────────────
    private fun fetchCollection(name: String): List<JSONObject> {
        val docs = mutableListOf<JSONObject>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append("$BASE/$name?key=$API_KEY&pageSize=300")
                if (pageToken != null) append("&pageToken=$pageToken")
            }
            val body = get(url)
            val json = JSONObject(body)
            val arr = json.optJSONArray("documents") ?: JSONArray()
            for (i in 0 until arr.length()) docs.add(arr.getJSONObject(i))
            pageToken = json.optString("nextPageToken").takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return docs
    }

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode !in 200..299) {
                error("Firestore ${conn.responseCode}: ${conn.responseMessage}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    // ─── Firestore tipli değerlerinin çözümü ──────────────────────────────────
    private fun JSONObject.field(name: String): JSONObject? =
        optJSONObject("fields")?.optJSONObject(name)

    private fun str(v: JSONObject?): String? = v?.optString("stringValue")?.takeIf {
        it.isNotEmpty() && !v.has("nullValue")
    }

    private fun int(v: JSONObject?): Int? =
        v?.optString("integerValue")?.toIntOrNull() ?: v?.optDouble("doubleValue")?.toInt()

    private fun bool(v: JSONObject?): Boolean = v?.optBoolean("booleanValue") ?: false

    private fun docId(doc: JSONObject): String =
        doc.optString("name").substringAfterLast('/')

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun timestamp(v: JSONObject?): Long {
        val raw = v?.optString("timestampValue")?.takeIf { it.isNotEmpty() } ?: return 0L
        return try {
            timestampFormat.parse(raw.substringBefore('.').removeSuffix("Z"))?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun parseCharacter(doc: JSONObject, dismissed: Boolean): Character {
        val evolutions = doc.field("evolutions")
            ?.optJSONObject("arrayValue")?.optJSONArray("values")
            ?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    val map = arr.getJSONObject(i).optJSONObject("mapValue") ?: return@mapNotNull null
                    val f = map.optJSONObject("fields") ?: return@mapNotNull null
                    Evolution(
                        star = int(f.optJSONObject("star")) ?: return@mapNotNull null,
                        imageUrl = str(f.optJSONObject("imageUrl")),
                        imagePosition = str(f.optJSONObject("imagePosition"))
                    )
                }
            }.orEmpty().sortedBy { it.star }

        val partners = doc.field("fusionPartners")
            ?.optJSONObject("arrayValue")?.optJSONArray("values")
            ?.let { arr -> (0 until arr.length()).mapNotNull { str(arr.getJSONObject(it)) } }
            .orEmpty()

        val auFields = doc.field("auData")?.optJSONObject("mapValue")?.optJSONObject("fields")
        val auData = buildMap {
            auFields?.keys()?.forEach { auId ->
                val f = auFields.optJSONObject(auId)?.optJSONObject("mapValue")
                    ?.optJSONObject("fields") ?: return@forEach
                put(
                    auId,
                    AuEntry(
                        star3ImageUrl = str(f.optJSONObject("star3ImageUrl")),
                        star3ImagePosition = str(f.optJSONObject("star3ImagePosition")),
                        fusionImageUrl = str(f.optJSONObject("fusionImageUrl")),
                        fusionImagePosition = str(f.optJSONObject("fusionImagePosition")),
                        prompt = str(f.optJSONObject("prompt"))
                    )
                )
            }
        }

        return Character(
            id = docId(doc),
            name = str(doc.field("name")) ?: docId(doc),
            irk = str(doc.field("irk")),
            star = int(doc.field("star")) ?: 1,
            imageUrl = str(doc.field("imageUrl")),
            imagePosition = str(doc.field("imagePosition")),
            previewUrl = str(doc.field("previewUrl")),
            previewPosition = str(doc.field("previewPosition")),
            isFusion = bool(doc.field("isFusion")),
            isDismissed = dismissed,
            type = str(doc.field("type")),
            fusionPartners = partners,
            evolutions = evolutions,
            auData = auData,
            addedAt = timestamp(doc.field("eklenmeTarihi"))
        )
    }

    private fun parseUniverse(doc: JSONObject) = AlternativeUniverse(
        id = docId(doc),
        name = str(doc.field("name")) ?: docId(doc),
        color = str(doc.field("color")),
        icon = str(doc.field("icon")),
        locked = bool(doc.field("locked")),
        description = str(doc.field("description"))
    )

    // ─── Disk önbelleği (çevrimdışı açılış) ───────────────────────────────────
    private fun cacheFile(context: Context) = File(context.filesDir, "snapshot.json")

    private fun writeCache(context: Context, snapshot: Snapshot) {
        runCatching { cacheFile(context).writeText(encode(snapshot)) }
    }

    suspend fun readCache(context: Context): Snapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val file = cacheFile(context)
            if (!file.exists()) return@withContext null
            decode(JSONObject(file.readText()))
        }.getOrNull()
    }

    private fun encode(s: Snapshot): String {
        fun chars(list: List<Character>) = JSONArray().apply {
            list.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("irk", c.irk ?: JSONObject.NULL)
                    put("star", c.star); put("imageUrl", c.imageUrl ?: JSONObject.NULL)
                    put("imagePosition", c.imagePosition ?: JSONObject.NULL)
                    put("previewUrl", c.previewUrl ?: JSONObject.NULL)
                    put("previewPosition", c.previewPosition ?: JSONObject.NULL)
                    put("isFusion", c.isFusion); put("isDismissed", c.isDismissed)
                    put("type", c.type ?: JSONObject.NULL); put("addedAt", c.addedAt)
                    put("fusionPartners", JSONArray(c.fusionPartners))
                    put("evolutions", JSONArray().apply {
                        c.evolutions.forEach { e ->
                            put(JSONObject().apply {
                                put("star", e.star)
                                put("imageUrl", e.imageUrl ?: JSONObject.NULL)
                                put("imagePosition", e.imagePosition ?: JSONObject.NULL)
                            })
                        }
                    })
                    put("auData", JSONObject().apply {
                        c.auData.forEach { (id, a) ->
                            put(id, JSONObject().apply {
                                put("star3ImageUrl", a.star3ImageUrl ?: JSONObject.NULL)
                                put("star3ImagePosition", a.star3ImagePosition ?: JSONObject.NULL)
                                put("fusionImageUrl", a.fusionImageUrl ?: JSONObject.NULL)
                                put("fusionImagePosition", a.fusionImagePosition ?: JSONObject.NULL)
                                put("prompt", a.prompt ?: JSONObject.NULL)
                            })
                        }
                    })
                })
            }
        }
        return JSONObject().apply {
            put("characters", chars(s.characters))
            put("dismissed", chars(s.dismissed))
            put("universes", JSONArray().apply {
                s.universes.forEach { u ->
                    put(JSONObject().apply {
                        put("id", u.id); put("name", u.name)
                        put("color", u.color ?: JSONObject.NULL)
                        put("icon", u.icon ?: JSONObject.NULL)
                        put("locked", u.locked)
                        put("description", u.description ?: JSONObject.NULL)
                    })
                }
            })
        }.toString()
    }

    private fun decode(root: JSONObject): Snapshot {
        fun s(o: JSONObject, k: String): String? =
            if (o.isNull(k)) null else o.optString(k).takeIf { it.isNotEmpty() }

        fun chars(key: String): List<Character> {
            val arr = root.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val evoArr = o.optJSONArray("evolutions") ?: JSONArray()
                val auObj = o.optJSONObject("auData") ?: JSONObject()
                Character(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    irk = s(o, "irk"),
                    star = o.optInt("star", 1),
                    imageUrl = s(o, "imageUrl"),
                    imagePosition = s(o, "imagePosition"),
                    previewUrl = s(o, "previewUrl"),
                    previewPosition = s(o, "previewPosition"),
                    isFusion = o.optBoolean("isFusion"),
                    isDismissed = o.optBoolean("isDismissed"),
                    type = s(o, "type"),
                    fusionPartners = o.optJSONArray("fusionPartners")?.let { fp ->
                        (0 until fp.length()).map { fp.getString(it) }
                    }.orEmpty(),
                    evolutions = (0 until evoArr.length()).map { j ->
                        val e = evoArr.getJSONObject(j)
                        Evolution(e.optInt("star"), s(e, "imageUrl"), s(e, "imagePosition"))
                    },
                    auData = buildMap {
                        auObj.keys().forEach { id ->
                            val a = auObj.getJSONObject(id)
                            put(
                                id,
                                AuEntry(
                                    s(a, "star3ImageUrl"), s(a, "star3ImagePosition"),
                                    s(a, "fusionImageUrl"), s(a, "fusionImagePosition"),
                                    s(a, "prompt")
                                )
                            )
                        }
                    },
                    addedAt = o.optLong("addedAt")
                )
            }
        }

        val uArr = root.optJSONArray("universes") ?: JSONArray()
        return Snapshot(
            characters = chars("characters"),
            dismissed = chars("dismissed"),
            universes = (0 until uArr.length()).map { i ->
                val o = uArr.getJSONObject(i)
                AlternativeUniverse(
                    id = o.getString("id"),
                    name = o.optString("name"),
                    color = s(o, "color"),
                    icon = s(o, "icon"),
                    locked = o.optBoolean("locked"),
                    description = s(o, "description")
                )
            }
        )
    }

    // ─── Yazma işlemleri (admin paneli) ───────────────────────────────────────
    // Site admin panelinin Firebase SDK ile yaptığı set/update/delete işlemlerinin
    // REST karşılığı. idToken: Auth.signIn'den gelen Firebase kimlik jetonu.

    private fun authedRequest(method: String, url: String, idToken: String, body: String?): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            // HttpURLConnection PATCH bilmez; Google API'leri method override kabul eder.
            requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") setRequestProperty("X-HTTP-Method-Override", "PATCH")
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Content-Type", "application/json")
            if (body != null) doOutput = true
        }
        try {
            if (body != null) conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("Firestore ${conn.responseCode}: ${err.take(200)}")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun vNull() = JSONObject().put("nullValue", JSONObject.NULL)
    private fun vStr(s: String?) = if (s == null) vNull() else JSONObject().put("stringValue", s)
    private fun vInt(i: Int) = JSONObject().put("integerValue", i.toString())
    private fun vBool(b: Boolean) = JSONObject().put("booleanValue", b)
    private fun vTs(ms: Long) =
        JSONObject().put("timestampValue", timestampFormat.format(Date(ms)) + "Z")
    private fun vArr(values: List<JSONObject>) =
        JSONObject().put("arrayValue", JSONObject().put("values", JSONArray(values)))
    private fun vMap(fields: JSONObject) =
        JSONObject().put("mapValue", JSONObject().put("fields", fields))

    private fun auEntryFields(a: AuEntry): JSONObject = JSONObject().apply {
        a.star3ImageUrl?.let { put("star3ImageUrl", vStr(it)) }
        a.star3ImagePosition?.let { put("star3ImagePosition", vStr(it)) }
        a.fusionImageUrl?.let { put("fusionImageUrl", vStr(it)) }
        a.fusionImagePosition?.let { put("fusionImagePosition", vStr(it)) }
        a.prompt?.let { put("prompt", vStr(it)) }
    }

    /** Karakteri Firestore alanlarına çevirir; mask düzenlenen alanları sınırlar. */
    private fun characterPayload(c: Character, includeTimestamp: Boolean): Pair<JSONObject, List<String>> {
        val fields = JSONObject()
        val mask = mutableListOf<String>()
        fun set(name: String, v: JSONObject) { fields.put(name, v); mask.add(name) }

        set("name", vStr(c.name))
        set("irk", vStr(c.irk))
        set("star", vInt(c.star))
        set("isFusion", vBool(c.isFusion))
        set("imageUrl", vStr(c.imageUrl))
        set("imagePosition", vStr(c.imagePosition))
        if (c.isFusion) {
            set("previewUrl", vStr(c.previewUrl))
            set("previewPosition", vStr(c.previewPosition))
            set("fusionPartners", vArr(c.fusionPartners.map { vStr(it) }))
        } else {
            set("evolutions", vArr(c.evolutions.map { e ->
                vMap(JSONObject().apply {
                    put("star", vInt(e.star))
                    put("imageUrl", vStr(e.imageUrl))
                    put("imagePosition", vStr(e.imagePosition))
                })
            }))
        }
        set("auData", vMap(JSONObject().apply {
            c.auData.forEach { (id, a) -> put(id, vMap(auEntryFields(a))) }
        }))
        if (includeTimestamp) {
            set("eklenmeTarihi", vTs(if (c.addedAt > 0) c.addedAt else System.currentTimeMillis()))
        }
        return fields to mask
    }

    private fun patch(idToken: String, collection: String, id: String, fields: JSONObject, mask: List<String>?) {
        val maskQ = mask?.joinToString("") { "&updateMask.fieldPaths=$it" }.orEmpty()
        val url = "$BASE/$collection/${Uri.encode(id)}?key=$API_KEY$maskQ"
        authedRequest("PATCH", url, idToken, JSONObject().put("fields", fields).toString())
    }

    suspend fun saveCharacter(idToken: String, collection: String, c: Character, isNew: Boolean): Unit =
        withContext(Dispatchers.IO) {
            val (fields, mask) = characterPayload(c, includeTimestamp = isNew)
            // Yeni kayıtta belge tamamen yazılır; düzenlemede mask bilinmeyen alanları korur.
            patch(idToken, collection, c.id, fields, if (isNew) null else mask)
        }

    suspend fun saveUniverse(idToken: String, u: AlternativeUniverse): Unit =
        withContext(Dispatchers.IO) {
            val fields = JSONObject().apply {
                put("name", vStr(u.name))
                put("color", vStr(u.color))
                put("icon", vStr(u.icon))
                put("locked", vBool(u.locked))
                put("description", vStr(u.description ?: ""))
            }
            patch(idToken, "alternativeUniverses", u.id, fields, null)
        }

    suspend fun deleteDocument(idToken: String, collection: String, id: String): Unit =
        withContext(Dispatchers.IO) {
            authedRequest("DELETE", "$BASE/$collection/${Uri.encode(id)}?key=$API_KEY", idToken, null)
        }

    /**
     * Emekliye ayırma / geri getirme: sitedeki gibi belge olduğu gibi kopyalanır
     * (bilinmeyen alanlar dahil), hedefe yazılır, kaynaktan silinir.
     * addType null ise "type" alanı düşürülür (geri getirme).
     */
    suspend fun moveDocument(
        idToken: String,
        fromCollection: String,
        toCollection: String,
        id: String,
        addType: String?
    ): Unit = withContext(Dispatchers.IO) {
        val raw = JSONObject(get("$BASE/$fromCollection/${Uri.encode(id)}?key=$API_KEY"))
        val fields = raw.optJSONObject("fields") ?: JSONObject()
        if (addType != null) fields.put("type", vStr(addType)) else fields.remove("type")
        authedRequest(
            "PATCH",
            "$BASE/$toCollection/${Uri.encode(id)}?key=$API_KEY",
            idToken,
            JSONObject().put("fields", fields).toString()
        )
        authedRequest("DELETE", "$BASE/$fromCollection/${Uri.encode(id)}?key=$API_KEY", idToken, null)
    }
}
