package org.freechains.kotlin

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

    val ln = reader.readLineX()
    when (ln) {
        "FC STO" -> { server.close() ; println("Host is down: $local") }
        "FC GET" -> {
            val chain_ = reader.readLineX()
            val node_  = reader.readLineX()
            
            val chain = local.loadChain(chain_.pathToChainNW())
            val node  = chain.loadNodeFromHH(node_.pathToNodeHH())

            val json  = node.toJson()
            assert(json.length <= Int.MAX_VALUE)
            writer.writeLineX("1")
            writer.writeUTF(json)
        }
        "FC PUT" -> {
            val path = reader.readLineX()
            val n    = reader.readInt()
            val pay  = reader.readNBytes(n)

            val chain = local.loadChain(path.pathToChainNW())
            chain.publish(pay.toString(Charsets.UTF_8))
            writer.writeLineX("1")
        }
        //0x1000.toShort() -> recv_1000()
        else -> { println(ln) ; error("invalid header type") }
    }
}