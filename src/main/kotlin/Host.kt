package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@Serializable
data class Host (
    val path : String,
    val port : Int
)

// JSON

fun Host.toJson () : String {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Host.serializer(), this)
}

fun String.fromJsonToHost () : Host {
    @UnstableDefault
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Host.serializer(), this)
}

// CHAIN

fun Host.createChain (name: String, work: Byte) : Chain {
    val chain = Chain(this.path,name,work)
    val file = File(this.path + "/chains/" + chain.toPath() + ".chain")
    assert(!file.exists()) { "chain already exists: $chain"}
    chain.save()
    return file.readText().fromJsonToChain()
}

fun Host.createChain (nw: Chain_NW) : Chain {
    return this.createChain(nw.name,nw.work)
}

fun Host.loadChain (name: String, work: Byte) : Chain {
    val file = File(this.path + "/chains/" + name + "/" + work + ".chain")
    return file.readText().fromJsonToChain()
}

fun Host.loadChain (chain: Chain_NW) : Chain {
    return this.loadChain(chain.name,chain.work)
}

// FILE SYSTEM

fun Host.save () {
    File(this.path + "/host").writeText(this.toJson())
}

fun Host_load (dir: String) : Host {
    return File(dir + "/host").readText().fromJsonToHost()
}

fun Host_create (dir: String, port: Int = 8330) : Host {
    val full = if (dir.substring(0,1) == "/") dir else System.getProperty("user.dir")+"/"+dir
    val fs = File(full)
    assert(!fs.exists()) { "directory already exists" }
    fs.mkdirs()
    val host = Host(full, port)
    host.save()
    return host
}