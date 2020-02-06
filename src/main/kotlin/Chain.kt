package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.time.Instant

typealias Name_Zeros = Pair<String,Byte>

@Serializable
data class Chain (
    val name  : String,
    val zeros : Byte
) {
    val hash  : String = this.toHash()
    var heads : Array<Height_Hash> = arrayOf(this.toHeightHash())
}

fun Chain.publish (payload: String) {
    this.publish(payload, Instant.now().toEpochMilli())
}
fun Chain.publish (payload: String, time: Long) : Node {
    val node = Node(time, 0, payload, this.heads)
    node.setNonceHashWithZeros(this.zeros)
    this.saveNode(node)
    this.heads = arrayOf(node.toHeightHash())
    this.save()
    return node
}

fun Chain.toHeightHash () : Height_Hash {
    return Height_Hash(0, this.toHash())
}

fun Chain.toHash () : String {
    return this.toByteArray().toHash()
}

fun Chain.toByteArray () : ByteArray {
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

fun Chain.toPair () : Name_Zeros {
    return Name_Zeros(this.name, this.zeros)
}

fun Chain.toPath () : String {
    return this.name + "/" + this.zeros
}

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

fun Chain.save () {
    val dir = File("data/" + this.toPath())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File("data/" + this.toPath() + ".chain").writeText(this.toJson())
}

fun Name_Zeros.load () : Chain {
    val chain = Chain(this.first,this.second)
    val file = File("data/" + chain.toPath() + ".chain")
    if (!file.exists()) {
        chain.save()
    }
    return file.readText().fromJsonToChain()
}

fun Chain.saveNode (node: Node) {
    val dir = File("data/" + this.toPath())
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File("data/" + this.toPath() + "/" + node.hash + ".node").writeText(node.toJson())
}

fun Chain.loadNodeFromHash (hash: String): Node {
    return File("data/" + this.toPath() + "/" + hash + ".node").readText().fromJsonToNode()
}


fun Chain.contains (hh: Height_Hash) : Boolean {
    if (this.hash == hh.hash) {
        return true
    } else {
        val file = File("data/" + this.toPath() + "/" + hh.hash + ".node")
        return file.exists()
    }
}

fun Chain.getBacksWithHeightOf (hh: Height_Hash, height: Long) : ArrayList<String> {
    val ret: ArrayList<String> = ArrayList()
    this.getBacksWithHeightOf(ret, hh, height)
    return ret
}

fun Chain.getBacksWithHeightOf (ret: ArrayList<String>, hh: Height_Hash, height: Long) {
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