import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Test

import java.net.Socket
import java.io.File

import kotlin.concurrent.thread
import kotlinx.serialization.protobuf.ProtoBuf

import freechains.*

@TestMethodOrder(Alphanumeric::class)
class Tests {
    @Test
    fun a_reset () {
        assert( File("data/").deleteRecursively() )
    }

    @Test
    fun b1_chain () {
        val chain1 = Chain("/uerj", 0)
        //println("Chain /uerj/0: ${chain1.toHash()}")
        chain1.save()
        val chain2 = chain1.toPair().load()
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    fun b2_node () {
        val chain = Chain("/uerj",0)
        val node = Node(0,0,"111", arrayOf(chain.toHeightHash()))
        node.setNonceHashWithZeros(0)
        //println("Node /uerj/0/111: ${node.hash!!}")
        chain.saveNode(node)
        val node2 = chain.loadNodeFromHash(node.hash!!)
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun c1_publish () {
        val chain = Name_Zeros("/ceu", 10.toByte()).load()
        val n1 = chain.publish("aaa", 0)
        val n2 = chain.publish("bbb", 1)
        val n3 = chain.publish("ccc", 2)

        assert(chain.contains(chain.toHeightHash()))
        //println(n1.toHeightHash())
        assert(chain.contains(n1.toHeightHash()))
        assert(chain.contains(n2.toHeightHash()))
        assert(chain.contains(n3.toHeightHash()))
        assert(!chain.contains(Height_Hash(2, "........")))
    }

    @Test
    fun c2_getBacks () {
        val chain = Name_Zeros("/ceu", 10.toByte()).load()
        val ret = chain.getBacksWithHeightOf(chain.heads[0],2)
        //println(ret)
        assert(ret.toString() == "[000d621b455be6f7a441dc662b7506a0ecd85ab835853c2528ab5f212d61b5c7]")
    }

    @Test
    fun d1_protobuf () {
        val bytes = ProtoBuf.dump(Header.serializer(), Header('F'.toByte(), 'C'.toByte(), 0x1000))
        //println("SIZE_PROTOBUF_HEADER: ${bytes.size}")
        assert(bytes.size == 7)
    }

    @Test
    fun d2_net () {
        thread { server() }
        Thread.sleep(100)
        val client = Socket("127.0.0.1", 8330)
        val header = Header('F'.toByte(), 'C'.toByte(), 0x1000)
        val bytes = ProtoBuf.dump(Header.serializer(), header)
        client.outputStream.write(bytes)
        client.close()
    }

    @Test
    fun e1_graph () {
        val chain = Chain("/graph",0)

        val a1 = Node(0,0,"a1", arrayOf(chain.toHeightHash()))
        val b1 = Node(0,0,"b1", arrayOf(chain.toHeightHash()))
        a1.setNonceHashWithZeros(0)
        b1.setNonceHashWithZeros(0)
        chain.saveNode(a1)
        chain.saveNode(b1)
        chain.heads = arrayOf(a1.toHeightHash(), b1.toHeightHash())

        val ab2 = chain.publish("ab2", 0)

        val b2 = Node(0,0,"b2", arrayOf(b1.toHeightHash()))
        b2.setNonceHashWithZeros(0)
        chain.saveNode(b2)
        chain.heads = arrayOf(chain.heads[0], b2.toHeightHash())

        val ret0 = chain.getBacksWithHeightOf(chain.heads[0],1)
        val ret1 = chain.getBacksWithHeightOf(chain.heads[1],1)
        //println(ret0)
        //println(ret1)
        assert(ret0.toString() == "[fdce30159fa932f438704eb7a646d5ed51938ea3bd8f928318f3e29a59403d54, ce274de26ef001a02cbe3f4d3adf360831fd1a27886cc55429fac0034daa4edc]")
        assert(ret1.toString() == "[ce274de26ef001a02cbe3f4d3adf360831fd1a27886cc55429fac0034daa4edc]")

        val ab3 = chain.publish("ab3", 0)
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
}
