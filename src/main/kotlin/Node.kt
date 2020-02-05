package freechains

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.security.MessageDigest
import kotlinx.serialization.*

@Serializable
data class Node (
    val time    : Long,             // TODO: ULong
    var nonce   : Long,             // TODO: ULong
    val payload : String,
    val backs   : Array<String>
) {
    var hash: String? = null
}

fun Node.toJson (): String {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Node.serializer(), this)
}

fun String.fromHashToNode (): Node {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Node.serializer(), this)
}

fun Node.saveJsonToFS (chain: Chain) {
    val directory = File("data/" + chain.toID())
    if (!directory.exists()) {
        directory.mkdirs()
    }
    File("data/" + chain.toID() + "/" + this.hash + ".node").writeText(this.toJson())
}

fun String.fromHashLoadFromFS (chain: Chain): Node {
    return File("data/" + chain.toID() + "/" + this + ".node").readText().fromHashToNode()
}

fun Node.toHash (): String {
    return this.toByteArray().toHash()
}

fun hash2zeros (hash: String): Int {
    var zeros = 0
    for (i in hash.indices step 2) {
        val bits = hash.substring(i, i+2).toInt(16)
        for (j in 7 downTo 0) {
            //println("$zeros, $bits, $j, ${bits shr j}")
            if (((bits shr j) and 1) == 1) {
                return zeros
            }
            zeros += 1
        }
    }
    return zeros
}

fun Node.setNonceHashWithZeros (zeros: Int) {
    while (true) {
        val hash = this.toHash()
        //println(hash)
        if (hash2zeros(hash) >= zeros) {
            this.hash = hash
            return
        }
        this.nonce++
    }
}

fun Node.toByteArray (): ByteArray {
    val bytes = ByteArray(8 + 8 + 4 + this.payload.length + this.backs.size*64 + 64)
    var off = 0
    bytes.setLongAt(off, this.time)
    off += 8
    bytes.setLongAt(off, this.nonce)
    for (v in this.payload) {
        bytes.set(off, v.toByte())
        off += 1
    }
    for (back in this.backs) {
        for (v in back) {
            bytes.set(off, v.toByte())
            off += 1
        }
    }
    return bytes
}

fun ByteArray.toHash (): String {
    return MessageDigest.getInstance("SHA-256").digest(this).fold("", { str, it -> str + "%02x".format(it) })
}

fun ByteArray.setLongAt (index: Int, value: Long) {
    this.set(index + 0, (value shr  0).toByte())
    this.set(index + 1, (value shr  8).toByte())
    this.set(index + 2, (value shr 16).toByte())
    this.set(index + 3, (value shr 24).toByte())
    this.set(index + 4, (value shr 32).toByte())
    this.set(index + 5, (value shr 40).toByte())
    this.set(index + 6, (value shr 48).toByte())
    this.set(index + 7, (value shr 56).toByte())
}