package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataInputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

@Serializable
data class Proto_Header (
    val F    : Byte,
    var C    : Byte,
    val type : Short
)

@Serializable
data class Proto_1000_Chain (
    val name  : String,
    val zeros : Byte
)

@Serializable
data class Proto_1000_Height_Hash (
    val height : Long,
    val hash   : Array<Byte>
)

fun ByteArray.toHeader (): Proto_Header {
    return ProtoBuf.load(Proto_Header.serializer(), this)
}

fun ByteArray.to_1000_Chain (): Proto_1000_Chain {
    return ProtoBuf.load(Proto_1000_Chain.serializer(), this)
}

fun ByteArray.to_1000_Height_Hash (): Proto_1000_Height_Hash {
    return ProtoBuf.load(Proto_1000_Height_Hash.serializer(), this)
}

fun server (host : Host) {
    val server = ServerSocket(host.port)
    println("Server is running on port ${server.localPort}")

    while (true) {
        val client = server.accept()
        println("Client connected: ${client.inetAddress.hostAddress}")
        thread { Handler(client).handle() }
    }

}

class Handler (client: Socket) {
    val reader = DataInputStream(client.getInputStream()!!)
    //val writer = client.getOutputStream()!!

    fun handle () {
        val n = reader.readByte()
        val header = reader.readNBytes(n.toInt()).toHeader()
        assert(header.F.toChar() == 'F' && header.C.toChar() == 'C') { "invalid header signature" }

        println("Type: 0x${header.type.toString(16)}")

        when (header.type) {
            0x1000.toShort() -> handle_1000()
            else -> error("invalid header type")
        }
    }

    fun handle_1000 () {
        val n = reader.readShort()
        val chain = reader.readNBytes(n.toInt()).to_1000_Chain()
        println(chain)

        while (true) {
            val n = reader.readByte()
            println(n)
            val hh = reader.readNBytes(n.toInt()).to_1000_Height_Hash()
            println(hh)
        }
    }
}