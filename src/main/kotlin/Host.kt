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

fun Host.save () {
    val dir = File(this.path)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    File(this.path + "/host").writeText(this.toJson())
}

fun Host_load (path: String) : Host {
    val file = File(path + "/host")
    if (!file.exists()) {
        Host(System.getProperty("user.dir"), 8330).save()
    }
    return file.readText().fromJsonToHost()
}

