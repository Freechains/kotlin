package org.freechains.kotlin

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
    val file = File(this.path + "/chains/" + chain.toPath() + ".chain")
    assert(!file.exists()) { "chain already exists: $chain"}
    chain.save()
    val genesis = Node(0,0, "", emptyArray())
    genesis.hash = chain.toGenHH().hash
    chain.saveNode(genesis)
    return file.readText().fromJsonToChain()
}

fun Host.loadChain (path: String) : Chain {
    val file = File(this.path + "/chains" + path + ".chain")
    return file.readText().fromJsonToChain()
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

// SPLIT

fun String.hostSplit () : Pair<String,Int> {
    val lst = this.split(":")
    return when (lst.size) {
        0 -> Pair("localhost", 8330)
        1 -> Pair(lst[0], 8330)
        else -> Pair(lst[0], lst[1].toInt())
    }
}
