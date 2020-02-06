package freechains

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.security.MessageDigest
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.math.max

@Serializable
data class Height_Hash (
    val height : Long,
    val hash   : String
)

@Serializable
data class Node (
    val time    : Long,             // TODO: ULong
    var nonce   : Long,             // TODO: ULong
    val payload : String,
    val backs   : Array<Height_Hash>
) {
    var height  : Long = this.backs.fold(0.toLong(), { cur,hh -> max(cur,hh.height) }) + 1
    var hash    : String? = null
}

fun Node.toJson (): String {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Node.serializer(), this)
}

fun ByteArray.protobufToNode (): Node {
    return ProtoBuf.load(Node.serializer(), this)
}

fun String.fromJsonToNode (): Node {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Node.serializer(), this)
}

fun Node.toHeightHash () : Height_Hash {
    return Height_Hash(this.height, this.hash!!)
}

fun Node.toProto () : Proto_1000_Height_Hash {
    return Proto_1000_Height_Hash(this.height, this.hash!!.hexToByteArray())
}

fun Node.calcHash (): String {
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

fun Node.setNonceHashWithZeros (zeros: Byte) {
    while (true) {
        val hash = this.calcHash()
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
    off += 8
    for (v in this.payload) {
        bytes.set(off, v.toByte())
        off += 1
    }
    for (hh in this.backs) {
        for (v in hh.hash) {
            bytes.set(off, v.toByte())
            off += 1
        }
    }
    return bytes
}

fun ByteArray.toHash (): String {
    return MessageDigest.getInstance("SHA-256").digest(this).toHexString()
}

fun String.fromHashToByteArray () : ByteArray {
    val ret = this.hexToByteArray()
    assert(ret.size == 32) { "invalid hash" }
    return ret
}

// https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd
fun ByteArray.toHexString () : String {
    return this.fold("", { str, it -> str + "%02x".format(it) })
}
fun String.hexToByteArray () : ByteArray {
    return ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
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