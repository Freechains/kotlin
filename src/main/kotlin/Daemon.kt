package org.freechains.kotlin

import java.io.DataInputStream
import java.io.DataOutputStream
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
            val pay = reader.readUTF()

            val chain = local.loadChain(path)
            val node = chain.publish(pay)

            writer.writeLineX(node.toPath())
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
        writer.writeLineX(hh.toPath())

        // receive response (needs or not)
        val ret = reader.readLineX()
        if (ret == "0") {
            return      // don't need it, nothing else to receive here
        }
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
    writer.writeLineX("")  // no more nodes to send

    // send number of nodes to be sent (just to confirm)
    assert(toSend.size <= Short.MAX_VALUE) { "too many nodes to send" }
    writer.writeLineX(toSend.size.toString())

    // send nodes in reverse order
    while (toSend.isNotEmpty()) {
        val hh = toSend.pop()
        //println("[send] send: $hh")

        val node = chain.loadNodeFromHH(hh)
        val json = node.toJson()
        writer.writeUTF(json)
    }
}

fun Socket.chain_recv (chain: Chain) {
    val reader = DataInputStream(this.getInputStream()!!)
    val writer = DataOutputStream(this.getOutputStream()!!)

    val toRecv : Stack<Node_HH> = Stack()

    // receive all heads
    while (true) {
        val hh_ = reader.readLineX()
        //println("[recv] bytes: $n3")
        if (hh_ == "") {
            break      // no more nodes to receive
        }
        val hh = hh_.pathToNodeHH()

        // do I need this head?
        if (chain.containsNode(hh)) {
            //println("[server] dont need")
            writer.writeLineX("0")     // no
        } else {
            writer.writeLineX("1")     // yes
            toRecv.push(hh)
        }
    }

    val tot = reader.readLineX().toInt()
    assert(tot == toRecv.size) { "unexpected number of nodes to receive" }

    while (toRecv.isNotEmpty()) {
        val hh = toRecv.pop()
        val node = reader.readUTF().jsonToNode()
        assert(node.toNodeHH() == hh) { "unexpected hash of node received" }
        //println("[server] node: $node")
        node.recheck()
        chain.reheads(node)
        chain.saveNode(node)
        chain.save()
    }
}
