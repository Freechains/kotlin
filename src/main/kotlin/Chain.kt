package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@Serializable
data class Chain (
    val name  : String,
    val zeros : Int,
    val heads : Array<String>
)

fun Chain.toID (): String {
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
    val directory = File("data/" + this.toID())
    if (!directory.exists()) {
        directory.mkdirs()
    }
    File("data/" + this.toID() + ".chain").writeText(this.toJson())
}

fun String.fromIDLoadFromFS (): Chain {
    return File("data/" + this + ".chain").readText().fromJsonToChain()
}