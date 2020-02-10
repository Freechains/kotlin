package org.freechains.kotlin.server

import org.freechains.kotlin.*
import org.docopt.Docopt
import java.net.Socket

val doc = """
freechains-server

Usage:
    freechains-server <dir> create [<port>]
    freechains-server <dir> start
    freechains-server <dir> stop
    freechains-server <dir> chain create <chain/work>
    freechains-server <dir> chain broadcast <chain/work>
    freechains-server <dir> chain subscribe <chain/work> (<address>:<port>)...

Options:
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/kotlin>.
"""

fun main (args: Array<String>) {
    val opts = Docopt(doc).withVersion("freechains-server v0.2").parse(args.toMutableList())
    val dir = opts["<dir>"] as String

    when {
        // CHAIN
        opts["chain"] as Boolean -> {
            val host = Host_load(dir)
            when {
                opts["create"] as Boolean -> {
                    val nw = (opts["<chain/work>"] as String).pathToChainNW()
                    val chain = host.createChain(nw)
                    println("Creating chain: $chain")
                }
            }
        }

        // HOST

        opts["create"] as Boolean -> {
            val host = Host_create(dir,(opts["<port>"] as String?)?.toInt() ?: 8330)
            println("Creating host: $host")
        }
        opts["start"] as Boolean -> {
            val host = Host_load(dir)
            println("Starting host: $host")
            daemon(host)
        }
        opts["stop"] as Boolean -> {
            val host = Host_load(dir)
            println("Stopping host: $host")
            val socket = Socket("localhost",host.port)
            socket.send_close()
            socket.close()
        }
    }
    //System.err.println("system return: $ret")
}