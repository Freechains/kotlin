import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.net.Socket

import kotlin.concurrent.thread

import kotlinx.serialization.protobuf.ProtoBuf

import freechains.*

class Tests {
    @Test
    fun chain () {
        val chain1 = Chain("/uerj", 0)
        println("Chain /uerj/0: ${chain1.toHash()}")
        chain1.saveJsonToFS()

        val chain2 = chain1.toPair().loadFromFS()
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    fun node () {
        val chain = Chain("/uerj",0)

        val node = Node(0,0,"111", arrayOf(Height_Hash(0,chain.toHash())))
        node.setNonceHashWithZeros(0)
        println("Node /uerj/0/111: ${node.hash!!}")
        node.saveJsonToFS(chain)

        val node2 = node.hash!!.fromHashLoadFromFS(chain)
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun publish () {
        val chain = Pair("/ceu",10.toByte()).loadFromFS()
        chain.publish("aaa", 0)
        chain.publish("bbb", 1)
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
