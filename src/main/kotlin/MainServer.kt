package freechains.server

import freechains.*
import org.docopt.Docopt
import java.net.Socket

val doc = """
freechains-server

Usage:
    freechains-server create <dir> [<port>]
    freechains-server start <dir>
    freechains-server stop
    freechains-server chain create <dir> <chain>/<work>
    freechains-server chain broadcast <dir> <chain>/<work>
    freechains-server chain subscribe <chain>/<work> (<address>:<port>)...

Options:
    --host=<addr:port>          address and port to connect [default: localhost:8330]
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/kotlin>.
"""

fun cmd_create (dir: String, port: Int) : Int {
    Host_create(dir,port)
    return 0
}

fun cmd_start (dir: String) : Int {
    val host = Host_load(dir)
    daemon(host)
    return 0
}

fun cmd_stop (opt_host: String?) : Int {
    val (host,port) = (opt_host ?: "localhost:8330").split(":")
    val socket = Socket(host,port.toInt())
    socket.send_0000()
    socket.close()
    return 0
}

fun main (args: Array<String>) : Int {
    val opts = Docopt(doc).withVersion("freechains-server v0.2").parse(args.toMutableList())

    return when {
        opts["create"] as Boolean -> cmd_create(opts["<dir>"] as String, 8330)
        opts["start"]  as Boolean -> cmd_start(opts["<dir>"] as String)
        opts["stop"]   as Boolean -> cmd_stop(opts["--host"] as String?)
        else -> -1
    }
}