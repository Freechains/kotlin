package freechains

import java.io.DataInputStream
import java.io.DataOutputStream
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import kotlin.concurrent.thread

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
        val chain_ = reader.readNBytes(n2.toInt()).toChainNW()
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
        try {
            val n = reader.readShort()
            val get = reader.readNBytes(n.toInt()).toProtoGet()
            val chain = local.loadChain(get.nw)
            val json = chain.loadNodeFromHH(get.hh).toJson()
            assert(json.length <= Int.MAX_VALUE)
            writer.writeBoolean(true)
            writer.writeUTF(json)
        } catch (e: Exception) {
            writer.writeBoolean(false)
        }
    }

    when (header.type) {
        0x0000.toShort() -> { server.close() ; println("Host is down: $local") }
        0x1000.toShort() -> recv_1000()
        0x2000.toShort() -> recv_2000()
        else -> error("invalid header type")
    }
}