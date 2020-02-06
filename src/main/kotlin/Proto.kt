package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

@Serializable
data class Proto_Header (
    val F    : Byte,
    var C    : Byte,
    val type : Short
)

@Serializable
data class Proto_Node_HH (
    val height : Long,
    val hash   : ByteArray
)

// CONVERSIONS

fun Proto_Node_HH.toNodeHH () : Node_HH {
    return Node_HH(this.height, this.hash.toHexString())
}

fun ByteArray.toHeader (): Proto_Header {
    return ProtoBuf.load(Proto_Header.serializer(), this)
}

fun ByteArray.toChainNZ (): Chain_NZ {
    return ProtoBuf.load(Chain_NZ.serializer(), this)
}

fun ByteArray.toProtoNodeHH (): Proto_Node_HH {
    return ProtoBuf.load(Proto_Node_HH.serializer(), this)
}

// SERVER

fun server (host : Host) {
    val server = ServerSocket(host.port)
    println("Server is running on port ${server.localPort}")

    while (true) {
        val client = server.accept()
        println("Client connected: ${client.inetAddress.hostAddress}")
        thread { Handler(host,client).handle() }
    }

}

// HANDLER

class Handler (host: Host, client: Socket) {
    val host = host
    val reader = DataInputStream(client.getInputStream()!!)
    val writer = DataOutputStream(client.getOutputStream()!!)

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
        val chain_ = reader.readNBytes(n.toInt()).toChainNZ()
        val chain = Chain_load(host.path, chain_.name,chain_.zeros)
        println(chain)

        while (true) {
            val n = reader.readByte()
            val hh = reader.readNBytes(n.toInt()).toProtoNodeHH()
            println(hh.toNodeHH())
            if (chain.containsNode(hh.toNodeHH())) {
                writer.writeByte(1)
            } else {
                writer.writeByte(0)
                val n = reader.readInt()
                //val node = reader.readNBytes(n.toInt()).protobufToNode()
                println(n)
            }
        }
    }
}