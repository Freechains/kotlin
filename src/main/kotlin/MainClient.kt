package org.freechains.kotlin.client

import org.freechains.kotlin.*
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

fun main (args: Array<String>) {
    val opts = Docopt(doc).withVersion("freechains-client v0.2").parse(args.toMutableList())

    val (host,port) = ((opts["--host"] as String?) ?: "localhost:8330").split(":")
    val socket = Socket(host, port.toInt())

    when {
        opts["get"] as Boolean -> {
            val ret = socket.send_get(
                (opts["<chain/work>"] as String).pathToChainNW(),
                (opts["<height/hash>"] as String).pathToNodeHH()
            )
            when (ret) {
                null -> { System.err.println("publication is not found") ; -1 }
                else -> { println(ret) }
            }
        }
        opts["put"] as Boolean -> {
            socket.send_put(
                (opts["<chain/work>"] as String).pathToChainNW(),
                when {
                    opts["text"] as Boolean -> (opts["<path_or_text>"] as String).toByteArray()
                    opts["file"] as Boolean -> File(opts["<path_or_text>"] as String).readBytes()
                    else -> error("TODO -")
                }
            )
        }
    }
    socket.close()
    //System.err.println("system return: $ret")
}
