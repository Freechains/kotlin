package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.time.Instant

@Serializable
data class Chain_NZ (
    val name  : String,
    val zeros : Byte
)

@Serializable
data class Chain (
    val path  : String,
    val name  : String,
    val zeros : Byte
) {
    val hash  : String = this.toHash()
    val heads : ArrayList<Node_HH> = arrayListOf(this.toGenHH())
}

// JSON

fun Chain.toJson () : String {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Chain.serializer(), this)
}

fun String.fromJsonToChain () : Chain {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Chain.serializer(), this)
}

// PROTO

fun Chain.toProtoHH () : Chain_NZ {
    return Chain_NZ(name, zeros)
}

// PUBLISH

fun Chain.publish (payload: String) : Node {
    return this.publish(payload, Instant.now().toEpochMilli())
}

fun Chain.publish (payload: String, time: Long) : Node {
    val node = Node(time, 0, payload, this.heads.toTypedArray())
    node.setNonceHashWithZeros(this.zeros)
    this.saveNode(node)
    this.heads.clear()
    this.heads.add(node.toNodeHH())
    this.save()
    return node
}

// GENESIS

fun Chain.toGenHH () : Node_HH {
    return Node_HH(0, this.toHash())
}

// PATH

fun Chain.toPath () : String {
    return this.name + "/" + this.zeros
}

// HASH

fun Chain.toHash () : String {
    return this.toByteArray().toHash()
}

private fun Chain.toByteArray () : ByteArray {
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

// FILE SYSTEM

fun Chain_create (path: String, name: String, zeros: Byte) : Chain {
    val chain = Chain(path,name,zeros)
    val file = File(path + "/chains/" + chain.toPath() + ".chain")
    if (!file.exists()) {
        chain.save()
    }
    return file.readText().fromJsonToChain()
}

fun Chain.save () {
    val dir = File(this.path + "/chains/" + this.toPath())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File(this.path + "/chains/" + this.toPath() + ".chain").writeText(this.toJson())
}

fun Chain_load (path: String, name: String, zeros: Byte) : Chain {
    val file = File(path + "/chains/" + name + "/" + zeros + ".chain")
    return file.readText().fromJsonToChain()
}

// NDOE

fun Chain.saveNode (node: Node) {
    val dir = File(this.path + "/chains/" + this.toPath())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File(this.path + "/chains/" + this.toPath() + "/" + node.hash + ".node").writeText(node.toJson())
}

fun Chain.loadNodeFromHash (hash: String): Node {
    return File(this.path + "/chains/" + this.toPath() + "/" + hash + ".node").readText().jsonToNode()
}

fun Chain.containsNode (hh: Node_HH) : Boolean {
    if (this.hash == hh.hash) {
        return true
    } else {
        val file = File(this.path + "/chains/" + this.toPath() + "/" + hh.hash + ".node")
        return file.exists()
    }
}

// TODO: REMOVE?

fun Chain.getBacksWithHeightOf (hh: Node_HH, height: Long) : ArrayList<String> {
    val ret: ArrayList<String> = ArrayList()
    this.getBacksWithHeightOf(ret, hh, height)
    return ret
}

fun Chain.getBacksWithHeightOf (ret: ArrayList<String>, hh: Node_HH, height: Long) {
    //println("$height vs $hh")
    assert(hh.height >= height) { "unexpected height"}
    if (hh.height == height) {
        ret.add(hh.hash)
        return
    }

    val node = this.loadNodeFromHash(hh.hash)
    for (back in node.backs) {
        this.getBacksWithHeightOf(ret, back, height)
    }
}