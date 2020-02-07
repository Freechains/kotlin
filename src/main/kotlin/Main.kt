package freechains

import org.docopt.Docopt

val doc = """
Freechains

Usage:
    freechains daemon start <config>
    freechains daemon stop
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

fun main (args: Array<String>) {
    val xxx = mutableListOf<String>("freechains","daemon","start","config")
    //val xxx = args.toMutableList()
    val freechains = xxx.removeAt(0)
    assert(freechains == "freechains")
    val opts = Docopt(doc).withVersion("Freechains v0.2").parse(xxx)
    println(opts)
}
