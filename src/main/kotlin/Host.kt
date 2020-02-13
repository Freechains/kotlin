package org.freechains.kotlin

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@Serializable
data class Host (
    val path : String,
    val port : Int,
    val timestamp : Boolean = true
)

// JSON

fun Host.toJson () : String {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.stringify(Host.serializer(), this)
}

fun String.fromJsonToHost () : Host {
    @UseExperimental(UnstableDefault::class)
    val json = Json(JsonConfiguration(prettyPrint=true))
    return json.parse(Host.serializer(), this)
}

// CHAIN

fun Host.createChain (path: String) : Chain {
    val (name,work) = path.pathToChainNW()
    val chain = Chain(this.path,name,work)
    val file = File(getRoot(),this.path + "/chains/" + chain.toPath() + ".chain")
    assert(!file.exists()) { "chain already exists: $chain"}
    chain.save()
    val genesis = Node(0,0, "", emptyArray())
    genesis.hash = chain.toGenHash()
    chain.saveNode(genesis)
    return file.readText().fromJsonToChain()
}

fun Host.loadChain (path: String) : Chain {
    val file = File(getRoot(),this.path + "/chains" + path + ".chain")
    return file.readText().fromJsonToChain()
}

// FILE SYSTEM

fun Host.save () {
    File(getRoot(),this.path + "/host").writeText(this.toJson()+"\n")
}

fun Host_load (dir: String) : Host {
    assert(dir.substring(0,1) == "/")
    return File(getRoot(),dir + "/host").readText().fromJsonToHost()
}

fun Host_exists (dir: String) : Boolean {
    assert(dir.substring(0,1) == "/")
    return File(getRoot(),dir).exists()
}

fun Host_create (dir: String, port: Int = 8330) : Host {
    assert(dir.substring(0,1) == "/")
    val fs = File(getRoot(),dir)
    assert(!fs.exists()) { "directory already exists" }
    fs.mkdirs()
    val host = Host(dir, port)
    host.save()
    return host
}

// SPLIT

fun String.hostSplit () : Pair<String,Int> {
    val lst = this.split(":")
    return when (lst.size) {
        0 -> Pair("localhost", 8330)
        1 -> Pair(lst[0], 8330)
        else -> Pair(lst[0], lst[1].toInt())
    }
}
