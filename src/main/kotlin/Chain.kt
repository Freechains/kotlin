package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.time.Instant
import java.time.Instant.now

typealias Name_Zeros = Pair<String,Byte>

@Serializable
data class Chain (
    val name  : String,
    val zeros : Byte
) {
    val hash  : String = this.toHash()
    var heads : Array<Height_Hash> = arrayOf(Height_Hash(0,this.hash))
}

fun Chain.publish (payload: String) {
    this.publish(payload, Instant.now().toEpochMilli())
}
fun Chain.publish (payload: String, time: Long) {
    val node = Node(time, 0, payload, this.heads)
    node.setNonceHashWithZeros(this.zeros)
    node.saveJsonToFS(this)
    this.heads = arrayOf(Height_Hash(node.height,node.hash!!))
    this.saveJsonToFS()
}

fun Chain.toHash (): String {
    return this.toByteArray().toHash()
}

fun Chain.toByteArray (): ByteArray {
    val bytes = ByteArray(this.name.length + 1)
    var off = 0
    for (v in this.name) {
        bytes.set(off, v.toByte())
        off += 1
    }
    bytes.set(off, this.zeros)
    off += 1
    return bytes
}

fun Chain.toPair (): Name_Zeros {
    return Name_Zeros(this.name, this.zeros)
}

fun Chain.toPath (): String {
    return this.name + "/" + this.zeros
}

fun Chain.toJson (): String {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Chain.serializer(), this)
}

fun String.fromJsonToChain (): Chain {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Chain.serializer(), this)
}

fun Chain.saveJsonToFS () {
    val directory = File("data/" + this.toPath())
    if (!directory.exists()) {
        directory.mkdirs()
    }
    File("data/" + this.toPath() + ".chain").writeText(this.toJson())
}

fun Name_Zeros.loadFromFS (): Chain {
    val chain = Chain(this.first,this.second)
    val file = File("data/" + chain.toPath() + ".chain")
    if (!file.exists()) {
        chain.saveJsonToFS()
    }
    return file.readText().fromJsonToChain()
}