package org.freechains.kotlin

import kotlinx.serialization.protobuf.ProtoBuf
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
    //System.err.println("host start: $host")

    while (true) {
        try {
            val remote = socket.accept()
            System.err.println("remote connect: $host <- ${remote.inetAddress.hostAddress}")
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

    val ln = reader.readLineX()
    when (ln) {
        "FC host stop" -> {
            writer.writeLineX("1")
            server.close()
            System.err.println("host stop: $local")
        }
        "FC chain create" -> {
            val path = reader.readLineX()
            val chain = local.createChain(path)
            writer.writeUTF(chain.hash)
        }
        "FC chain get" -> {
            val path = reader.readLineX().pathCheck()
            val node_ = reader.readLineX()

            val chain = local.loadChain(path)
            val node  = chain.loadNodeFromHH(node_.pathToNodeHH())
            val json  = node.toJson()

            assert(json.length <= Int.MAX_VALUE)
            writer.writeLineX("1")
            writer.writeUTF(json)
        }
        "FC chain put" -> {
            val path = reader.readLineX().pathCheck()
            val n = reader.readInt()
            val pay = reader.readNBytes(n)

            val chain = local.loadChain(path)
            val node = chain.publish(pay.toString(Charsets.UTF_8))

            writer.writeLineX(node.hash!!)
        }
        "FC chain send" -> {
            val path = reader.readLineX().pathCheck()
            val host_ = reader.readLineX()

            val chain = local.loadChain(path)
            val (host,port) = host_.hostSplit()

            val socket = Socket(host, port)
            socket.chain_send(chain)
            //writer.writeLineX(ret)
        }
        "FC chain receive" -> {
            val path = reader.readLineX().pathCheck()
            val chain = local.loadChain(path)
            remote.chain_recv(chain)
            //writer.writeLineX(ret)
        }
        else -> { error("$ln: invalid header type") }
    }
}

fun Socket.chain_send (chain: Chain) {
    val reader = DataInputStream(this.getInputStream()!!)
    val writer = DataOutputStream(this.getOutputStream()!!)

    writer.writeLineX("FC chain receive")
    writer.writeLineX(chain.toPath())

    val toSend : Stack<Node_HH> = Stack()
    val visited = mutableSetOf<Node_HH>()

    fun send_rec (hh: Node_HH) {
        //println("[send] $hh")
        if (visited.contains(hh)) {
            return
        } else {
            visited.add(hh)
        }

        // transmit HH
        val bytes = ProtoBuf.dump(Node_HH.serializer(), hh.toNodeHH())
        assert(bytes.size <= Byte.MAX_VALUE)
        writer.writeByte(bytes.size)
        writer.write(bytes)

        // receive response (needs or not)
        val ret = reader.readByte()
        if (ret == 0.toByte()) {
            return      // don't need it, nothing else to receive here
        }
        //println("[send] toSend: $hh")
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
        //println("[send] send: $hh")

        val node = chain.loadNodeFromHH(hh)
        val bytes = ProtoBuf.dump(Node.serializer(), node)
        assert(bytes.size <= Int.MAX_VALUE)
        writer.writeInt(bytes.size)
        writer.write(bytes)
    }
}

fun Socket.chain_recv (chain: Chain) {
    val reader = DataInputStream(this.getInputStream()!!)
    val writer = DataOutputStream(this.getOutputStream()!!)

    val toRecv : Stack<Node_HH> = Stack()

    // receive all heads
    while (true) {
        val n3 = reader.readByte()
        //println("[recv] bytes: $n3")
        if (n3 == 0.toByte()) {
            break      // no more nodes to receive
        }

        val hh = reader.readNBytes(n3.toInt()).toNodeHH()
        //println("[recv] toRecv? $hh")

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
        //println("[server] node: $node")
        node.recheck()
        chain.reheads(node)
        chain.saveNode(node)
        chain.save()
    }
}
