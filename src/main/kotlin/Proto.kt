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
        thread { handle(host,client) }
    }

}

// HANDLE

fun handle (host: Host, client: Socket) {
    val reader = DataInputStream(client.getInputStream()!!)
    val writer = DataOutputStream(client.getOutputStream()!!)

    // HEADER
    val n1 = reader.readByte()
    val header = reader.readNBytes(n1.toInt()).toHeader()
    assert(header.F.toChar() == 'F' && header.C.toChar() == 'C') { "invalid header signature" }
    //println("Type: 0x${header.type.toString(16)}")

    fun handle_1000 () {
        val n2 = reader.readShort()
        val chain_ = reader.readNBytes(n2.toInt()).toChainNZ()
        val chain = Chain_load(host.path, chain_.name,chain_.zeros)
        //println(chain)

        // receive all heads
        while (true) {
            val n3 = reader.readByte()
            val hh3 = reader.readNBytes(n3.toInt()).toProtoNodeHH()
            //println(hh3.toNodeHH())

            // do I need this head?
            if (chain.containsNode(hh3.toNodeHH())) {
                writer.writeByte(0)     // no, send me next or stop
            } else {
                writer.writeByte(1)     // yes, send me it complete

                // receive this and all recursive backs
                fun receive () : Node {
                    val n4 = reader.readInt()
                    val node = reader.readNBytes(n4).protobufToNode()
                    //println(node)
                    //node.nonce += 1
                    node.recheck()
                    chain.saveNode(node)

                    // check backs from received node
                    for (hh4 in node.backs) {
                        if (!chain.containsNode(hh4)) {
                            //println("request ${hh4.hash} from ${node.hash}")
                            // send request for this back which I don't have
                            writer.writeByte(1)         // request a child
                            val bytes = ProtoBuf.dump(Proto_Node_HH.serializer(), hh4.toProtoHH())
                            assert(bytes.size <= Byte.MAX_VALUE)
                            writer.writeByte(bytes.size)
                            writer.write(bytes)

                            receive()
                        }
                    }
                    return node
                }
                val head = receive()
                chain.heads.add(head.toNodeHH())
                chain.save()

                // TODO: caution: save node in FS but backs are not still received, connection might break

                writer.writeByte(0)     // I have it all!
            }

            // has more heads?
            val more = reader.readByte()
            if (more.toInt() == 0) {
                println("server disconnected")
                break       // no: disconnect
            }
        }
    }

    when (header.type) {
        0x1000.toShort() -> handle_1000()
        else -> error("invalid header type")
    }
}