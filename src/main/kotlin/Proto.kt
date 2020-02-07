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
        thread { serve(client, host) }
    }

}

// SERVE

fun serve (remote: Socket, local: Host) {
    val reader = DataInputStream(remote.getInputStream()!!)
    val writer = DataOutputStream(remote.getOutputStream()!!)

    // HEADER
    val n1 = reader.readByte()
    val header = reader.readNBytes(n1.toInt()).toHeader()
    assert(header.F.toChar() == 'F' && header.C.toChar() == 'C') { "invalid header signature" }
    //println("Type: 0x${header.type.toString(16)}")

    fun serve_1000 () {
        val n2 = reader.readShort()
        val chain_ = reader.readNBytes(n2.toInt()).toChainNZ()
        val chain = Chain_load(local.path, chain_.name,chain_.zeros)
        println("[server] chain: $chain")

        // receive all heads
        while (true) {
            val n3 = reader.readByte()
            val hh3 = reader.readNBytes(n3.toInt()).toProtoNodeHH()
            println("[server] head: ${hh3.toNodeHH()}")

            // do I need this head?
            if (chain.containsNode(hh3.toNodeHH())) {
                println("[server] dont need")
                writer.writeByte(0)     // no, send me next or stop
            } else {
                writer.writeByte(1)     // yes, send me it complete

                // receive this and all recursive backs
                fun receive () : Node {
                    val n4 = reader.readInt()
                    val node = reader.readNBytes(n4).protobufToNode()
                    println("[server] node: $node")
                    node.recheck()
                    chain.saveNode(node)

                    // check backs from received node
                    for (hh4 in node.backs) {
                        if (chain.containsNode(hh4)) {
                            chain.heads.remove(hh4)     // if child is a head, remove it!
                        } else {
                            println("[server] ask: ${hh4.hash} from ${node.hash}")
                            // send request for this back which I don't have
                            writer.writeByte(1)         // request a child
                            val bytes = ProtoBuf.dump(Proto_Node_HH.serializer(), hh4.toProtoHH())
                            assert(bytes.size <= Byte.MAX_VALUE)
                            writer.writeByte(bytes.size)
                            writer.write(bytes)

                            receive()
                        }
                    }
                    writer.writeByte(0)   // no more childs
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
                println("[server] disconnected")
                break       // no: disconnect
            }
        }
    }

    when (header.type) {
        0x1000.toShort() -> serve_1000()
        else -> error("invalid header type")
    }
}

fun client_1000 (remote: Socket, chain: Chain) {
    val reader = DataInputStream(remote.getInputStream()!!)
    val writer = DataOutputStream(remote.getOutputStream()!!)

    // send header
    val header = Proto_Header('F'.toByte(), 'C'.toByte(), 0x1000)
    val bytes1 = ProtoBuf.dump(Proto_Header.serializer(), header)
    assert(bytes1.size <= Byte.MAX_VALUE)
    writer.writeByte(bytes1.size)
    writer.write(bytes1)

    // send chain
    val bytes2 = ProtoBuf.dump(Chain_NZ.serializer(), chain.toChainNZ())
    assert(bytes2.size <= Short.MAX_VALUE)
    writer.writeShort(bytes2.size)
    writer.write(bytes2)

    // HEADS
    for (hh2 in chain.heads) {
        // send head HH
        val bytes3 = ProtoBuf.dump(Proto_Node_HH.serializer(), hh2.toProtoHH())
        assert(bytes3.size <= Byte.MAX_VALUE)
        writer.writeByte(bytes3.size)
        writer.write(bytes3)

        val ret3 = reader.readByte()
        if (ret3 == 1.toByte()) {    // remote needs it
            fun send (hh: Node_HH) {
                // send complete head node
                val node4 = chain.loadNodeFromHH(hh)
                val bytes4 = ProtoBuf.dump(Node.serializer(), node4)
                assert(bytes4.size <= Int.MAX_VALUE)
                writer.writeInt(bytes4.size)
                writer.write(bytes4)

                // receive backs remote needs
                val ret4 = reader.readByte()
                if (ret4 == 1.toByte()) {    // remote needs back
                    val n5 = reader.readByte()
                    val hh5 = reader.readNBytes(n5.toInt()).toProtoNodeHH()
                    println("[client]: server wants ${hh5.toNodeHH().hash}")
                    send(hh5.toNodeHH())
                }
            }
            send(hh2)
        }

        // one more head
        writer.writeByte(1)
    }

    // no more heads
    writer.writeByte(0)
}