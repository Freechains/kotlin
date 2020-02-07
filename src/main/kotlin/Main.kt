package freechains

import org.docopt.Docopt
import java.net.Socket

val doc = """
Freechains

Usage:
    freechains host create <dir> <port>
    freechains host start <dir>
    freechains host stop <dir>
    freechains get <chain>[/<work>] [<block-hash> | <pub-hash>]
    freechains publish <chain>/<work> (<file>|+<string>|-)
    freechains remove <chain>/<work> <block-hash>
    freechains subscribe <chain>/<work> {<address>:<port>}
    freechains configure get {<field>}
    freechains configure set {<field> (=|+=|-=) <value>}
    freechains listen [<chain>[/<work>]]
    freechains crypto create (shared|public-private)
    freechains crypto encrypt (shared|seal|public-private) key [key] (<file>|+<string>|-)

Options:
    --sign=<key-private>        signs publication (chain must have `key_public`)
    --passphrase=<passphrase>   deterministic creation from passphrase (minimum length?)
                                    (should be very long! never forget this!)
    --address=<ip-address>      address to connect/bind (default: `localhost`)
    --port=<tcp-port>           port to connect/bind (default: `8330`)
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/freechains>.
"""

fun cmd_host_create (dir: String, port: Int) {
    Host_create(dir,port)
}

fun cmd_host_start (dir : String) {
    val host = Host_load(dir)
    daemon(host)
}

fun cmd_host_stop (dir: String) {
    val host = Host_load(dir)
    val socket = Socket("127.0.0.1", host.port)
    socket.send_0000()
    socket.close()
}

fun main (args: Array<String>) {
    //val xxx = mutableListOf<String>("freechains","daemon","start","config")
    val xxx = args.toMutableList()
    val freechains = xxx.removeAt(0)
    assert(freechains == "freechains")
    val opts = Docopt(doc).withVersion("Freechains v0.2").parse(xxx)

    when {
        opts["host"] as Boolean ->
            when {
                opts["create"] as Boolean -> cmd_host_create(opts["<dir>"] as String, 8330)
                opts["start"]  as Boolean -> cmd_host_start(opts["<dir>"] as String)
                opts["stop"]   as Boolean -> cmd_host_stop(opts["<dir>"] as String)
            }
    }
}
