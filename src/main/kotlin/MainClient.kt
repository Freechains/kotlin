package freechains.client

import freechains.*
import org.docopt.Docopt
import java.io.File
import java.net.Socket

val doc = """
freechains-client

Usage:
    freechains-client get [options] <chain/work> <height/hash>
    freechains-client put <chain/work> (file | text | -) [<path_or_text>]
    freechains-client index [options] <chain/work> <n>
    freechains-client listen <chain/work>
    freechains-client remove <chain/work> <block-hash>

Options:
    --host=<addr:port>          address and port to connect [default: localhost:8330]
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/kotlin>.
"""

fun cmd_get (name_work: String, height_hash: String, opt_host: String?) : Int {
    val (host,port) = (opt_host ?: "localhost:8330").split(":")
    val socket = Socket(host, port.toInt())
    val ret = socket.send_2000(name_work.toChainNW(), height_hash.toNodeHH())
    socket.close()
    if (ret == null) {
        System.err.println("publication is not found")
        return -1
    } else {
        println(ret)
        return 0
    }
}

fun cmd_put (name_work: String, payload: ByteArray, opt_host: String?) : Int {
    val (host,port) = (opt_host ?: "localhost:8330").split(":")
    val socket = Socket(host, port.toInt())
    socket.send_3000(name_work.toChainNW(), payload)
    socket.close()
    return 0
}

fun main (args: Array<String>) : Int {
    val opts = Docopt(doc).withVersion("freechains-client v0.2").parse(args.toMutableList())
    println(opts)
    return when {
        opts["get"] as Boolean -> cmd_get(opts["<chain/work>"] as String, opts["<height/hash>"] as String, opts["--host"] as String?)
        opts["put"] as Boolean -> {
            val payload: ByteArray =
                when {
                    opts["text"] as Boolean -> (opts["<path_or_text>"] as String).toByteArray()
                    opts["file"] as Boolean -> File(opts["<path_or_text>"] as String).readBytes()
                    else -> error("TODO -")
                }
            cmd_put(opts["<chain/work>"] as String, payload, opts["--host"] as String?)
        }
        else -> -1
    }
}
