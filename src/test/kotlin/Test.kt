import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.MethodOrderer.Alphanumeric;
//import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test

import java.net.Socket

import kotlin.concurrent.thread

import kotlinx.serialization.protobuf.ProtoBuf

import freechains.*

//@TestMethodOrder(Alphanumeric.class)
class Tests {
    @Test
    fun chain () {
        val chain1 = Chain("/uerj", 0)
        println("Chain /uerj/0: ${chain1.toHash()}")
        chain1.save()

        val chain2 = chain1.toPair().load()
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    fun node () {
        val chain = Chain("/uerj",0)

        val node = Node(0,0,"111", arrayOf(Height_Hash(0,chain.toHash())))
        node.setNonceHashWithZeros(0)
        println("Node /uerj/0/111: ${node.hash!!}")
        chain.saveNode(node)

        val node2 = chain.loadNodeFromHash(node.hash!!)
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun publish () {
        val chain = Name_Zeros("/ceu", 10.toByte()).load()
        val n1 = chain.publish("aaa", 0)
        val n2 = chain.publish("bbb", 1)

        assert(chain.contains(Height_Hash(0, chain.hash)))
        //println(n1.toHeightHash())
        assert(chain.contains(n1.toHeightHash()))
        assert(chain.contains(n2.toHeightHash()))
        assert(!chain.contains(Height_Hash(2, "........")))
    }

    @Test
    fun getBacks () {
        val chain = Name_Zeros("/ceu", 10.toByte()).load()
        //println(chain.heads[0])
        val ret = chain.getBacksWithHeightOf(chain.heads[0],10)
        println(ret)
        // TODO: testar forks e joins
    }

    @Test
    fun protobuf () {
        val bytes = ProtoBuf.dump(Header.serializer(), Header('F'.toByte(), 'C'.toByte(), 0x1000))
        println("SIZE_PROTOBUF_HEADER: ${bytes.size}")
    }

    @Test
    fun net () {
        thread { server() }
        Thread.sleep(100)
        val client = Socket("127.0.0.1", 8330)
        val header = Header('F'.toByte(), 'C'.toByte(), 0x1000)
        val bytes = ProtoBuf.dump(Header.serializer(), header)
        client.outputStream.write(bytes)
        client.close()
    }
}
