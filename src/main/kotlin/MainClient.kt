package freechains.client

import freechains.*
import org.docopt.Docopt
import java.net.Socket

val doc = """
freechains-client

Usage:
    freechains-client get [options] <chain>/<work>/<height>/<hash>
    freechains-client put <chain>/<work> (<file>|+<string>|-)
    freechains-client index [options] <chain>/<work> <n>
    freechains-client listen <chain>/<work>
    freechains-client remove <chain>/<work> <block-hash>

Options:
    --sign=<key-private>        signs publication (chain must have `key_public`)
    --passphrase=<passphrase>   deterministic creation from passphrase (minimum length?)
                                    (should be very long! never forget this!)
    --host=<addr:port>          address and port to connect [default: localhost:8330]
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/kotlin>.
"""

fun cmd_get (chain_work_height_hash: String, opt_host: String = "localhost:8330") : Int {
    val (host,port) = opt_host.split(":")
    val (name,zeros,height,hash) = chain_work_height_hash.split("/")
    val socket = Socket(host, port.toInt())
    val ret = socket.send_2000(Chain_NZ(name,zeros.toByte()), Node_HH(height.toLong(),hash))
    socket.close()
    return (if (ret == null) -1 else 0)
}

fun main (args: Array<String>) : Int {
    val opts = Docopt(doc).withVersion("freechains-client v0.2").parse(args.toMutableList())

    return when {
        opts["get"] as Boolean -> cmd_get(opts["<chain>/<work>/<height>/<hash>"] as String, opts["--host"] as String)
        else -> -1
    }
}
