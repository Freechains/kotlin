import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Test

import java.net.Socket
import java.io.File

import kotlin.concurrent.thread
import kotlinx.serialization.protobuf.ProtoBuf

import org.freechains.kotlin.*
import org.freechains.kotlin.client.main as clientMain
import org.freechains.kotlin.server.main as serverMain

/*
 *  TODO:
 *  - command-line daemon / client
 *  - chain locks
 *  - peer/chain configurations in host
 *  - generate executable
 */

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
        val host1 = Host_create("tests/local/")
        val chain1 = Chain("tests/local/", "/uerj", 0)
        //println("Chain /uerj/0: ${chain1.toHash()}")
        chain1.save()
        val chain2 = host1.loadChain(chain1.toChainNW())
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    fun b2_node () {
        val chain = Chain("tests/local/", "/uerj",0)
        val node = Node(0,0,"111", arrayOf(chain.toGenHH()))
        node.setNonceHashWithWork(0)
        //println("Node /uerj/0/111: ${node.hash!!}")
        chain.saveNode(node)
        val node2 = chain.loadNodeFromHH(node.toNodeHH())
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun c1_publish () {
        val host = Host_load("tests/local/")
        val chain = host.createChain("/ceu", 10)
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
        val host = Host_load("tests/local/")
        val chain = host.loadChain("/ceu", 10.toByte())
        val ret = chain.getBacksWithHeightOf(chain.heads[0],2)
        //println(ret)
        assert(ret.toString() == "[000d621b455be6f7a441dc662b7506a0ecd85ab835853c2528ab5f212d61b5c7]")
    }

    @Test
    fun d1_protobuf () {
        val header = ProtoBuf.dump(Proto_Header.serializer(), Proto_Header('F'.toByte(), 'C'.toByte(), 0x1000))
        //println("SIZE_PROTOBUF_HEADER: ${bytes.size}")
        assert(header.size == 7)

        val v0 = Chain_NW("/ceu", 10)
        val v1 = ProtoBuf.dump(Chain_NW.serializer(), v0)
        val v2 = ProtoBuf.load(Chain_NW.serializer(), v1)
        assert(v0 == v2)

        val n1 = Node(0,0,"111", arrayOf(Node_HH(0,"000"), Node_HH(1,"111")))
        n1.hash = "XXX"
        val bytes = ProtoBuf.dump(Node.serializer(), n1)
        val n2 = bytes.protobufToNode()
        assert(n1.toString() == n2.toString())
    }

    @Test
    fun d3_proto () {
        // REMOTE
        val remote = Host_create("tests/remote/")
        val remote_chain = remote.createChain("/d3", 5)
        remote_chain.publish("aaa", 0)
        remote_chain.publish("bbb", 0)

        // LOCAL
        val local = Host_load("tests/local/")
        local.createChain("/d3", 5)
        thread { daemon(local) }
        Thread.sleep(100)

        val s1 = Socket("127.0.0.1", local.port)
        s1.send_1000(remote_chain)
        Thread.sleep(100)
        s1.close()

        val s2 = Socket("127.0.0.1", local.port)
        s2.send_0000()
        Thread.sleep(100)
        s2.close()
    }

    @Test
    fun e1_graph () {
        val chain = Chain("tests/local/", "/graph",0)

        val a1 = Node(0,0,"a1", arrayOf(chain.toGenHH()))
        val b1 = Node(0,0,"b1", arrayOf(chain.toGenHH()))
        a1.setNonceHashWithWork(0)
        b1.setNonceHashWithWork(0)
        chain.saveNode(a1)
        chain.saveNode(b1)
        chain.reheads(a1)
        chain.reheads(b1)

        //val ab2 =
        chain.publish("ab2", 0)

        val b2 = Node(0,0,"b2", arrayOf(b1.toNodeHH()))
        b2.setNonceHashWithWork(0)
        chain.saveNode(b2)
        chain.reheads(b2)

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
        //a_reset()

        val h1 = Host_create("tests/h1/", 8330)
        val h1_chain = h1.createChain("/xxx", 0)
        h1_chain.publish("h1_1", 0)
        h1_chain.publish("h1_2", 0)

        val h2 = Host_create("tests/h2/", 8331)
        val h2_chain = h2.createChain("/xxx", 0)
        h2_chain.publish("h2_1", 0)
        h2_chain.publish("h2_2", 0)

        Thread.sleep(100)
        thread { daemon(h1) }
        thread { daemon(h2) }
        Thread.sleep(100)

        val socket = Socket("127.0.0.1", h1.port)
        socket.send_1000(h2_chain)
        socket.close()
    }

    @Test
    fun m1_args () {
        a_reset()
        serverMain(arrayOf("tests/local/","create"))
        serverMain(arrayOf("tests/local/","chain","create","/xxx/0/"))
        serverMain(arrayOf("tests/8331/","create","8331"))
        thread {
            Thread.sleep(100)
            clientMain(arrayOf("put","/xxx/0","text","aaa"))
            clientMain(arrayOf("put","/xxx/0","file","tests/local/host"))
            clientMain(arrayOf("get","--host=localhost:8330","/xxx/0", "0/826ffb4505831e6355edc141f49b1ccf5b489b9f03760f0f2fed4eeed419c6fe"))
            clientMain(arrayOf("get","/xxx/0/", "0/826ffb4505831e6355edc141f49b1ccf5b489b9f03760f0f2fed4eeed419c6fe/"))
            serverMain(arrayOf("tests/local/","stop"))
        }
        serverMain(arrayOf("tests/local/","start"))
    }
}
