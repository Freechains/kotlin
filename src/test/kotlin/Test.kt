import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import freechains.*

class Tests {
    @Test
    @DisplayName("Chain")
    fun Chain () {
        val chain1 = Chain("/uerj", 0)
        println(chain1.toHash())
        chain1.saveJsonToFS()

        val chain2 = chain1.toID().fromIDLoadFromFS()
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    @DisplayName("Node")
    fun Node () {
        val chain = Chain("/uerj",0)

        val node = Node(0,0,"", emptyArray())
        node.setNonceHashWithZeros(5)
        println(node.hash!!)
        node.saveJsonToFS(chain)

        val node2 = node.hash!!.fromHashLoadFromFS(chain)
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }
}
