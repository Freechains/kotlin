package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.time.Instant

@Serializable
data class Chain_NZ (
    val name : String,
    val work : Byte
)

@Serializable
data class Chain (
    val path : String,
    val name : String,
    val work : Byte
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

// CONVERSIONS

fun Chain.toChainNZ () : Chain_NZ {
    return Chain_NZ(name, work)
}

fun String.toChainNZ () : Chain_NZ {
    val (x, name,work) = this.split("/")
    assert(x == "/")
    return Chain_NZ(name,work.toByte())
}

// PUBLISH

fun Chain.publish (payload: String) : Node {
    return this.publish(payload, Instant.now().toEpochMilli())
}

fun Chain.publish (payload: String, time: Long) : Node {
    val node = Node(time, 0, payload, this.heads.toTypedArray())
    node.setNonceHashWithWork(this.work)
    this.saveNode(node)
    this.reheads(node)
    this.save()
    return node
}

fun Chain.reheads (node: Node) {
    this.heads.add(node.toNodeHH())
    for (hh in node.backs) {
        this.heads.remove(hh)
    }
}

// GENESIS

fun Chain.toGenHH () : Node_HH {
    return Node_HH(0, this.toHash())
}

// PATH

fun Chain.toPath () : String {
    return this.name + "/" + this.work
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
    bytes.set(off, this.work)
    off += 1
    return bytes
}

// FILE SYSTEM

fun Chain.save () {
    val dir = File(this.path + "/chains/" + this.toPath())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File(this.path + "/chains/" + this.toPath() + ".chain").writeText(this.toJson())
}

// NDOE

fun Chain.saveNode (node: Node) {
    val dir = File(this.path + "/chains/" + this.toPath())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File(this.path + "/chains/" + this.toPath() + "/" + node.hash + ".node").writeText(node.toJson())
}

fun Chain.loadNodeFromHH (hh: Node_HH): Node {
    return File(this.path + "/chains/" + this.toPath() + "/" + hh.hash + ".node").readText().jsonToNode()
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

    val node = this.loadNodeFromHH(hh)
    for (back in node.backs) {
        this.getBacksWithHeightOf(ret, back, height)
    }
}