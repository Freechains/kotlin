package freechains

import kotlinx.serialization.protobuf.ProtoBuf
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.*

fun Socket.send_0000 () {
    val writer = DataOutputStream(this.getOutputStream()!!)
    val header = Proto_Header('F'.toByte(), 'C'.toByte(), 0x0000)
    val bytes = ProtoBuf.dump(Proto_Header.serializer(), header)
    assert(bytes.size <= Byte.MAX_VALUE)
    writer.writeByte(bytes.size)
    writer.write(bytes)
}

fun Socket.send_2000 (chain: Chain_NW, node: Node_HH): String? {
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
    writer.writeShort(bytes2.size)
    writer.write(bytes2)

    val ret = reader.readBoolean()
    if (!ret) {
        return null
    } else {
        return reader.readUTF()
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
    val bytes2 = ProtoBuf.dump(Chain_NW.serializer(), chain.toChainNW())
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