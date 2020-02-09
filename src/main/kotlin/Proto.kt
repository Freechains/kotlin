package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
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

@Serializable
data class Proto_Get (
    val nz: Chain_NZ,
    val hh: Node_HH
)

// CONVERSIONS

fun Proto_Node_HH.toNodeHH () : Node_HH {
    return Node_HH(this.height, this.hash.toHexString())
}

fun ByteArray.toHeader () : Proto_Header {
    return ProtoBuf.load(Proto_Header.serializer(), this)
}

fun ByteArray.toChainNZ () : Chain_NZ {
    return ProtoBuf.load(Chain_NZ.serializer(), this)
}

fun ByteArray.toProtoNodeHH () : Proto_Node_HH {
    return ProtoBuf.load(Proto_Node_HH.serializer(), this)
}

fun ByteArray.toProtoGet () : Proto_Get {
    return ProtoBuf.load(Proto_Get.serializer(), this)
}

// SERVER

fun daemon (host : Host) {
    val socket = ServerSocket(host.port)
    println("Host is up: $host")

    while (true) {
        try {
            val remote = socket.accept()
            println("Client connected: ${remote.inetAddress.hostAddress}")
            thread { handle(socket, remote, host) }
        } catch (e: SocketException) {
            assert(e.message == "Socket closed")
            break
        }
    }
}

// SERVE

fun handle (server: ServerSocket, remote: Socket, local: Host) {
    val reader = DataInputStream(remote.getInputStream()!!)
    val writer = DataOutputStream(remote.getOutputStream()!!)

    // HEADER
    val n1 = reader.readByte()
    val header = reader.readNBytes(n1.toInt()).toHeader()
    assert(header.F.toChar() == 'F' && header.C.toChar() == 'C') { "invalid header signature" }
    //println("Type: 0x${header.type.toString(16)}")

    fun recv_1000 () {
        // receive chain
        val n2 = reader.readShort()
        val chain_ = reader.readNBytes(n2.toInt()).toChainNZ()
        val chain = local.loadChain(chain_)
        println("[recv] chain: $chain")

        val toRecv : Stack<Node_HH> = Stack()

        // receive all heads
        while (true) {
            val n3 = reader.readByte()
            println("[recv] bytes: $n3")
            if (n3 == 0.toByte()) {
                break      // no more nodes to receive
            }

            val phh = reader.readNBytes(n3.toInt()).toProtoNodeHH()
            val hh = phh.toNodeHH()
            println("[recv] toRecv? $hh")

            // do I need this head?
            if (chain.containsNode(hh)) {
                //println("[server] dont need")
                writer.writeByte(0)     // no
            } else {
                writer.writeByte(1)     // yes
                toRecv.push(hh)
            }
        }

        val tot = reader.readShort()
        assert(tot == toRecv.size.toShort()) { "unexpected number of nodes to receive" }

        while (toRecv.isNotEmpty()) {
            val hh = toRecv.pop()
            val n = reader.readInt()
            val node = reader.readNBytes(n).protobufToNode()
            assert(node.hash == hh.hash) { "unexpected hash of node received" }
            println("[server] node: $node")
            node.recheck()
            chain.reheads(node)
            chain.saveNode(node)
            chain.save()
        }
    }

    fun recv_2000 () {
        // receive chain
        val n = reader.readShort()
        val get = reader.readNBytes(n.toInt()).toProtoGet()
        val chain = local.loadChain(get.nz)
        val json = chain.loadNodeFromHH(get.hh).toJson()
        assert(json.length <= Int.MAX_VALUE)
        writer.writeByte(json.length)
        writer.writeChars(json)
    }

    when (header.type) {
        0x0000.toShort() -> { println("Host is down.") ; server.close() }
        0x1000.toShort() -> recv_1000()
        0x2000.toShort() -> recv_2000()
        else -> error("invalid header type")
    }
}

fun Socket.send_0000 () {
    val writer = DataOutputStream(this.getOutputStream()!!)
    val header = Proto_Header('F'.toByte(), 'C'.toByte(), 0x0000)
    val bytes = ProtoBuf.dump(Proto_Header.serializer(), header)
    assert(bytes.size <= Byte.MAX_VALUE)
    writer.writeByte(bytes.size)
    writer.write(bytes)
}

fun Socket.send_2000 (chain: Chain_NZ, node: Node_HH): String? {
    val reader = DataInputStream(this.getInputStream()!!)
    val writer = DataOutputStream(this.getOutputStream()!!)

    val header = Proto_Header('F'.toByte(), 'C'.toByte(), 0x2000)
    val bytes1 = ProtoBuf.dump(Proto_Header.serializer(), header)
    assert(bytes1.size <= Byte.MAX_VALUE)
    writer.writeByte(bytes1.size)
    writer.write(bytes1)

    val get = Proto_Get(chain, node)
    val bytes2 = ProtoBuf.dump(Proto_Get.serializer(), get)
    assert(bytes2.size <= Short.MAX_VALUE)
    writer.writeByte(bytes2.size)
    writer.write(bytes2)

    val ret = reader.readInt()
    if (ret == 0) {
        return null
    } else {
        return reader.readNBytes(ret).protobufToNode().toJson()
    }
}

fun Socket.send_1000 (chain: Chain) {
    val reader = DataInputStream(this.getInputStream()!!)
    val writer = DataOutputStream(this.getOutputStream()!!)

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
    println("[send] chain: $chain")

    val toSend : Stack<Node_HH> = Stack()
    val visited = mutableSetOf<Node_HH>()

    fun send_rec (hh: Node_HH) {
        println("[send] $hh")
        if (visited.contains(hh)) {
            return
        } else {
            visited.add(hh)
        }

        // transmit HH
        val bytes = ProtoBuf.dump(Proto_Node_HH.serializer(), hh.toProtoHH())
        assert(bytes.size <= Byte.MAX_VALUE)
        writer.writeByte(bytes.size)
        writer.write(bytes)

        // receive response (needs or not)
        val ret = reader.readByte()
        if (ret == 0.toByte()) {
            return      // don't need it, nothing else to receive here
        }
        println("[send] toSend: $hh")
        toSend.push(hh) // need it, add and proceed to backs

        // transmit backs
        val node = chain.loadNodeFromHH(hh)
        for (hh_back in node.backs) {
            if (hh_back != chain.toGenHH()) {
                send_rec(hh_back)
            }
        }
    }

    // send heads recursively
    for (hh in chain.heads) {
        send_rec(hh)
    }
    writer.writeByte(0)     // no more nodes to send

    // send number of nodes to be sent (just to confirm)
    assert(toSend.size <= Short.MAX_VALUE) { "too many nodes to send" }
    writer.writeShort(toSend.size)

    // send nodes in reverse order
    while (toSend.isNotEmpty()) {
        val hh = toSend.pop()
        println("[send] send: $hh")

        val node = chain.loadNodeFromHH(hh)
        val bytes = ProtoBuf.dump(Node.serializer(), node)
        assert(bytes.size <= Int.MAX_VALUE)
        writer.writeInt(bytes.size)
        writer.write(bytes)
    }
}