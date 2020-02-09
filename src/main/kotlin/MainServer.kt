package freechains.server

import freechains.*
import org.docopt.Docopt
import java.net.Socket

val doc = """
freechains-server

Usage:
    freechains-server <dir> create [<port>]
    freechains-server <dir> start
    freechains-server <dir> stop
    freechains-server <dir> chain create <chain>/<work>
    freechains-server <dir> chain broadcast <chain>/<work>
    freechains-server <dir> chain subscribe <chain>/<work> (<address>:<port>)...

Options:
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/kotlin>.
"""

fun cmd_create (dir: String, port: String?) : Int {
    val host = Host_create(dir,port?.toInt() ?: 8330)
    println("Creating host: $host")
    return 0
}

fun cmd_start (dir: String) : Int {
    val host = Host_load(dir)
    println("Starting host: $host")
    daemon(host)
    return 0
}

fun cmd_stop (dir: String) : Int {
    val host = Host_load(dir)
    val socket = Socket("localhost",host.port)
    println("Stopping host: $host")
    socket.send_0000()
    socket.close()
    return 0
}

fun main (args: Array<String>) : Int {
    val opts = Docopt(doc).withVersion("freechains-server v0.2").parse(args.toMutableList())
    return when {
        opts["create"] as Boolean -> cmd_create(opts["<dir>"] as String, opts["<port>"] as String?)
        opts["start"]  as Boolean -> cmd_start(opts["<dir>"] as String)
        opts["stop"]   as Boolean -> cmd_stop(opts["<dir>"] as String)
        else -> -1
    }
}