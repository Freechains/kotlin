import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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

        val node = Node(0,0,"111", arrayOf(chain.toHash()))
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
}
