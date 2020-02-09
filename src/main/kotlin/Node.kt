package org.freechains.kotlin

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.security.MessageDigest
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.math.max

@Serializable
data class Node_HH (
    val height : Long,
    val hash   : String
)

@Serializable
data class Node (
    val time    : Long,             // TODO: ULong
    var nonce   : Long,             // TODO: ULong
    val payload : String,
    val backs   : Array<Node_HH>
) {
    val height  : Long = if (this.backs.isEmpty()) 0 else this.backs.fold(0.toLong(), { cur,hh -> max(cur,hh.height) }) + 1
    var hash    : String? = null
}

// JSON

fun Node.toJson (): String {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Node.serializer(), this)
}

fun String.jsonToNode (): Node {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Node.serializer(), this)
}

// PROTOBUF

fun ByteArray.protobufToNode (): Node {
    return ProtoBuf.load(Node.serializer(), this)
}

// PROTO

fun Node.toProtoHH () : Proto_Node_HH {
    return Proto_Node_HH(this.height, this.hash!!.hashToByteArray())
}

fun Node_HH.toProtoHH () : Proto_Node_HH {
    return Proto_Node_HH(this.height, this.hash.hashToByteArray())
}

// HH

fun Node.toNodeHH () : Node_HH {
    return Node_HH(this.height, this.hash!!)
}

fun String.toNodeHH () : Node_HH {
    val (height,hash) = this.split("/")
    return Node_HH(height.toLong(), hash)
}

// HASH

fun ByteArray.toHash (): String {
    return MessageDigest.getInstance("SHA-256").digest(this).toHexString()
}

fun Node.setNonceHashWithWork (work: Byte) {
    while (true) {
        val hash = this.calcHash()
        //println(hash)
        if (hash2work(hash) >= work) {
            this.hash = hash
            return
        }
        this.nonce++
    }
}

fun Node.recheck () {
    assert(this.hash!! == this.calcHash())
}

private fun Node.calcHash (): String {
    return this.toByteArray().toHash()
}

private fun hash2work (hash: String): Int {
    var work = 0
    for (i in hash.indices step 2) {
        val bits = hash.substring(i, i+2).toInt(16)
        for (j in 7 downTo 0) {
            //println("$work, $bits, $j, ${bits shr j}")
            if (((bits shr j) and 1) == 1) {
                return work
            }
            work += 1
        }
    }
    return work
}

private fun Node.toByteArray (): ByteArray {
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

private fun ByteArray.setLongAt (index: Int, value: Long) {
    this.set(index + 0, (value shr  0).toByte())
    this.set(index + 1, (value shr  8).toByte())
    this.set(index + 2, (value shr 16).toByte())
    this.set(index + 3, (value shr 24).toByte())
    this.set(index + 4, (value shr 32).toByte())
    this.set(index + 5, (value shr 40).toByte())
    this.set(index + 6, (value shr 48).toByte())
    this.set(index + 7, (value shr 56).toByte())
}

// HEX STRING <-> BYTE ARRAY

// https://gist.github.com/fabiomsr/845664a9c7e92bafb6fb0ca70d4e44fd

fun ByteArray.toHexString () : String {
    return this.fold("", { str, it -> str + "%02x".format(it) })
}

fun String.hashToByteArray () : ByteArray {
    val ret = this.hexToByteArray()
    assert(ret.size == 32) { "invalid hash" }
    return ret
}

private fun String.hexToByteArray () : ByteArray {
    return ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
