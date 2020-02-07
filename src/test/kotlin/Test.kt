import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Test

import java.net.Socket
import java.io.File

import kotlin.concurrent.thread
import kotlinx.serialization.protobuf.ProtoBuf

import freechains.*
import java.io.DataInputStream
import java.io.DataOutputStream

@TestMethodOrder(Alphanumeric::class)
class Tests {
    @Test
    fun a_reset () {
        assert( File("tests/").deleteRecursively() )
    }

    @Test
    fun a1_hash () {
        val x = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
        val y = byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31)
        assert(x == y.toHexString())
        assert(x.hashToByteArray().contentEquals(y))
    }

    @Test
    fun b1_chain () {
        val chain1 = Chain("tests/local/", "/uerj", 0)
        //println("Chain /uerj/0: ${chain1.toHash()}")
        chain1.save()
        val chain2 = Chain_load(chain1.path, chain1.name, chain1.zeros)
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    fun b2_node () {
        val chain = Chain("tests/local/", "/uerj",0)
        val node = Node(0,0,"111", arrayOf(chain.toGenHH()))
        node.setNonceHashWithZeros(0)
        //println("Node /uerj/0/111: ${node.hash!!}")
        chain.saveNode(node)
        val node2 = chain.loadNodeFromHH(node.toNodeHH())
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun c1_publish () {
        val chain = Chain_create("tests/local/", "/ceu", 10)
        val n1 = chain.publish("aaa", 0)
        val n2 = chain.publish("bbb", 1)
        val n3 = chain.publish("ccc", 2)

        assert(chain.containsNode(chain.toGenHH()))
        //println(n1.toHeightHash())
        assert(chain.containsNode(n1.toNodeHH()))
        assert(chain.containsNode(n2.toNodeHH()))
        assert(chain.containsNode(n3.toNodeHH()))
        assert(!chain.containsNode(Node_HH(2, "........")))
    }

    @Test
    fun c2_getBacks () {
        val chain = Chain_load("tests/local/", "/ceu", 10.toByte())
        val ret = chain.getBacksWithHeightOf(chain.heads[0],2)
        //println(ret)
        assert(ret.toString() == "[000d621b455be6f7a441dc662b7506a0ecd85ab835853c2528ab5f212d61b5c7]")
    }

    @Test
    fun d1_protobuf () {
        val header = ProtoBuf.dump(Proto_Header.serializer(), Proto_Header('F'.toByte(), 'C'.toByte(), 0x1000))
        //println("SIZE_PROTOBUF_HEADER: ${bytes.size}")
        assert(header.size == 7)

        val v0 = Chain_NZ("/ceu", 10)
        val v1 = ProtoBuf.dump(Chain_NZ.serializer(), v0)
        val v2 = ProtoBuf.load(Chain_NZ.serializer(), v1)
        assert(v0 == v2)

        val n1 = Node(0,0,"111", arrayOf(Node_HH(0,"000"), Node_HH(1,"111")))
        n1.hash = "XXX"
        val bytes = ProtoBuf.dump(Node.serializer(), n1)
        val n2 = bytes.protobufToNode()
        assert(n1.toString() == n2.toString())
    }

    @Test
    fun d2_net () {
        // LOCAL
        val local = Host("tests/local/", 8330)
        local.save()
        val tmp = Host_load("tests/local/")
        assert(tmp == local)
        thread { daemon(local) }
        Thread.sleep(100)

        // REMOTE
        val remote = Host_create("tests/remote/")
        val chain = Chain_create(remote.path, "/ceu", 10)
        val node1 = chain.publish("remote1", 0)
        val node2 = chain.publish("remote2", 0)

        // LOCAL
        val client = Socket("127.0.0.1", local.port)
        val reader = DataInputStream(client.getInputStream())
        val writer = DataOutputStream(client.getOutputStream())

        // HEADER
        if (true) {
            val header = Proto_Header('F'.toByte(), 'C'.toByte(), 0x1000)
            val bytes = ProtoBuf.dump(Proto_Header.serializer(), header)
            assert(bytes.size <= Byte.MAX_VALUE)
            writer.writeByte(bytes.size)
            writer.write(bytes)
        }

        // CHAIN
        if (true) {
            val bytes = ProtoBuf.dump(Chain_NZ.serializer(), chain.toChainNZ())
            assert(bytes.size <= Short.MAX_VALUE)
            writer.writeShort(bytes.size)
            writer.write(bytes)
        }

        // HEIGHT_HASH
        if (true) {
            val hh = Proto_Node_HH(10, "000d621b455be6f7a441dc662b7506a0ecd85ab835853c2528ab5f212d61b5c7".hashToByteArray())
            val bytes = ProtoBuf.dump(Proto_Node_HH.serializer(), hh)
            //println("${bytes.size} : $bytes")
            assert(bytes.size <= Byte.MAX_VALUE)
            writer.writeByte(bytes.size)
            writer.write(bytes)
            val ret = reader.readByte()
            assert(ret == 0.toByte())   // 0 = don't need
        }

        // one more head
        writer.writeByte(1)

        // HEIGHT_HASH
        if (true) {
            if (true) {
                val bytes = ProtoBuf.dump(Proto_Node_HH.serializer(), node2.toProtoHH())
                //println("${bytes.size} : $bytes")
                assert(bytes.size <= Byte.MAX_VALUE)
                writer.writeByte(bytes.size)
                writer.write(bytes)
                val ret = reader.readByte()
                assert(ret == 1.toByte())   // 1 = need it
            }

            // NODE
            if (true) {
                val bytes = ProtoBuf.dump(Node.serializer(), node2)
                assert(bytes.size <= Int.MAX_VALUE)
                writer.writeInt(bytes.size)
                writer.write(bytes)
                //println(node2)
            }
        }

        // CHILD_NODE
        if (true) {
            val ret = reader.readByte()
            assert(ret == 1.toByte())   // 1 = need child
            val n = reader.readByte()
            val hh = reader.readNBytes(n.toInt()).toProtoNodeHH()
            //println("server wants child: ${hh.toNodeHH()}")
            assert(hh.toNodeHH().hash == "003b3323a89782261b42ac8aeb3d82e3b73ff216a9b0eecedd019278c79b8713")

            val bytes = ProtoBuf.dump(Node.serializer(), node1)
            assert(bytes.size <= Int.MAX_VALUE)
            writer.writeInt(bytes.size)
            writer.write(bytes)
            //println(node1)
        }

        // no more heads
        writer.writeByte(0)

        // TODO: testar chains e nodes que nao existam

        Thread.sleep(100)
        client.close()
        println("[client] terminated")
    }

    @Test
    fun d3_proto () {
        // REMOTE
        val remote = Host_load("tests/remote/")
        val remote_chain = Chain_create(remote.path, "/d3", 5)
        remote_chain.publish("aaa", 0)
        remote_chain.publish("bbb", 0)

        // LOCAL
        val local = Host_load("tests/local/")
        Chain_create(local.path, "/d3", 5)
        //thread { server(local) }
        //Thread.sleep(100)

        val socket = Socket("127.0.0.1", local.port)
        send_1000(socket, remote_chain)
        socket.close()
    }

    @Test
    fun e1_graph () {
        val chain = Chain("tests/local/", "/graph",0)

        val a1 = Node(0,0,"a1", arrayOf(chain.toGenHH()))
        val b1 = Node(0,0,"b1", arrayOf(chain.toGenHH()))
        a1.setNonceHashWithZeros(0)
        b1.setNonceHashWithZeros(0)
        chain.saveNode(a1)
        chain.saveNode(b1)
        chain.heads.clear()
        chain.heads.add(a1.toNodeHH())
        chain.heads.add(b1.toNodeHH())

        //val ab2 =
        chain.publish("ab2", 0)

        val b2 = Node(0,0,"b2", arrayOf(b1.toNodeHH()))
        b2.setNonceHashWithZeros(0)
        chain.saveNode(b2)
        chain.heads.add(b2.toNodeHH())

        val ret0 = chain.getBacksWithHeightOf(chain.heads[0],1)
        val ret1 = chain.getBacksWithHeightOf(chain.heads[1],1)
        //println(ret0)
        //println(ret1)
        assert(ret0.toString() == "[fdce30159fa932f438704eb7a646d5ed51938ea3bd8f928318f3e29a59403d54, ce274de26ef001a02cbe3f4d3adf360831fd1a27886cc55429fac0034daa4edc]")
        assert(ret1.toString() == "[ce274de26ef001a02cbe3f4d3adf360831fd1a27886cc55429fac0034daa4edc]")

        //val ab3 =
        chain.publish("ab3", 0)
        val ret3 = chain.getBacksWithHeightOf(chain.heads[0],1)
        val ret4 = chain.getBacksWithHeightOf(chain.heads[0],2)
        //println(ret3)
        //println(ret4)
        assert(ret3.toString() == "[fdce30159fa932f438704eb7a646d5ed51938ea3bd8f928318f3e29a59403d54, ce274de26ef001a02cbe3f4d3adf360831fd1a27886cc55429fac0034daa4edc, ce274de26ef001a02cbe3f4d3adf360831fd1a27886cc55429fac0034daa4edc]")
        assert(ret4.toString() == "[86dc6eb9696fb9f424be59c40daaef1c9e12883a3d2c498f81c9df22fdc96d59, bc4ce369e5a5b9ce427a07c6ea5beb124eb0d864348ff1d1c998c3a3557edd11]")

        chain.save()
        /*
               /-- (a1) --\
        (G) --<            >-- (ab2) --\__ (ab3)
               \-- (b1) --+--- (b2) ---/
         */
    }

    @Test
    fun f1_peers () {
        val h1 = Host_create("tests/h1/")
        val h1_chain = Chain_create(h1.path, "/d3", 5)
        h1_chain.publish("h1_1", 0)
        h1_chain.publish("h1_2", 0)

    }
}
