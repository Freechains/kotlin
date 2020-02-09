package freechains

import org.docopt.Docopt
import java.net.Socket

/*
val tst = """
Use this cool tool to do cool stuff
Usage: cooltool.kts [options] <igenome> <fastq_files>...

Options:
 --gtf <gtfFile>     Custom gtf file instead of igenome bundled copy
 --pc-only           Use protein coding genes only for mapping and quantification
""${'"'}

val doArgs = Docopt(usage).parse(args.toList())
"""
*/

val doc = """
Freechains

Usage:
    freechains host create <dir> [<port>]
    freechains host start <dir>
    freechains host stop <dir>
    freechains host broadcast <dir> <chain>/<work>
    freechains host subscribe <chain>/<work> (<address>:<port>)...
    freechains get [options] <chain>/<work>/<height>/<hash>
    freechains put <chain>/<work> (<file>|+<string>|-)
    freechains index [options] <chain>/<work> <n>
    freechains listen <chain>/<work>
    freechains remove <chain>/<work> <block-hash>
    freechains configure get <field>...
    freechains configure set (<field> (=|+=|-=) <value>)...
    freechains crypto create (shared|public-private)
    freechains crypto encrypt (shared|seal|public-private) key [key] (<file>|+<string>|-)

Options:
    --sign=<key-private>        signs publication (chain must have `key_public`)
    --passphrase=<passphrase>   deterministic creation from passphrase (minimum length?)
                                    (should be very long! never forget this!)
    --host=<addr:port>          address and port to connect [default: localhost:8330]
    --help                      display this help
    --version                   display version information

More Information:

    http://www.freechains.org/

    Please report bugs at <http://github.com/Freechains/freechains>.
"""

fun cmd_host_create (dir: String, port: Int) : Int {
    Host_create(dir,port)
    return 0
}

fun cmd_host_start (dir: String) : Int {
    val host = Host_load(dir)
    daemon(host)
    return 0
}

fun cmd_host_stop (dir: String) : Int {
    val host = Host_load(dir)
    val socket = Socket("127.0.0.1", host.port)
    socket.send_0000()
    socket.close()
    return 0
}

fun cmd_get (chain_work_height_hash: String, opt_host: String = "localhost:8330") : Int {
    val (host,port) = opt_host.split(":")
    val (name,zeros,height,hash) = chain_work_height_hash.split("/")
    val socket = Socket(host, port.toInt())
    val ret = socket.send_2000(Chain_NZ(name,zeros.toByte()), Node_HH(height.toLong(),hash))
    socket.close()
    return (if (ret == null) -1 else 0)
}

fun main (args: Array<String>) : Int {
    //val xxx = mutableListOf<String>("freechains","daemon","start","config")
    val xxx = args.toMutableList()
    val freechains = xxx.removeAt(0)
    assert(freechains == "freechains")
    val opts = Docopt(doc).withVersion("Freechains v0.2").parse(xxx)

    return when {
        opts["host"] as Boolean ->
            when {
                opts["create"] as Boolean -> cmd_host_create(opts["<dir>"] as String, 8330)
                opts["start"]  as Boolean -> cmd_host_start(opts["<dir>"] as String)
                opts["stop"]   as Boolean -> cmd_host_stop(opts["<dir>"] as String)
                else -> -1
            }
        opts["get"] as Boolean -> cmd_get(opts["<chain>/<work>/<height>/<hash>"] as String, opts["--host"] as String)
        else -> -1
    }
}
