import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.MethodOrderer.Alphanumeric
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.Test

import java.io.File

import kotlin.concurrent.thread

import org.freechains.kotlin.*

/*
 *  TODO:
 *  - 948 -> 852 -> 841 LOC
 *  - typedef Hash
 *  - uplinks, new algo send/recv
 *  - android
 *  - testes antigos
 *  - crypto (chain e host)
 *  - chain locks
 *  - freechains chain listen
 *  - freechains host configure (json)
 *    - peer/chain configurations in host
 *  - freechains host restart
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
    }

    @Test
    fun b1_chain () {
        val host1 = Host_create("tests/local/")
        val chain1 = Chain("tests/local/", "/uerj", 0)
        //println("Chain /uerj/0: ${chain1.toHash()}")
        chain1.save()
        val chain2 = host1.loadChain(chain1.toPath())
        assertThat(chain1.hashCode()).isEqualTo(chain2.hashCode())
    }

    @Test
    fun b2_node () {
        val chain = Chain("tests/local/", "/uerj",0)
        val node = Node(0,0,"111", arrayOf(chain.toGenHash()))
        node.setNonceHashWithWork(0)
        chain.saveNode(node)
        val node2 = chain.loadNodeFromHash(node.hash!!)
        assertThat(node.hashCode()).isEqualTo(node2.hashCode())
    }

    @Test
    fun c1_publish () {
        val host = Host_load("tests/local/")
        val chain = host.createChain("/ceu/10")
        val n1 = chain.publish("aaa", 0)
        val n2 = chain.publish("bbb", 1)
        val n3 = chain.publish("ccc", 2)

        assert(chain.containsNode(chain.toGenHash()))
        //println(n1.toHeightHash())
        assert(chain.containsNode(n1.hash!!))
        assert(chain.containsNode(n2.hash!!))
        assert(chain.containsNode(n3.hash!!))
        assert(!chain.containsNode("2_........"))
    }

    @Test
    fun d2_proto () {
        val local = Host_load("tests/local/")
        thread { daemon(local) }
        Thread.sleep(100)

        main(arrayOf("host","stop"))
        Thread.sleep(100)
    }
    @Test
    fun d3_proto () {
        a_reset()

        // SOURCE
        val src = Host_create("tests/src/")
        val src_chain = src.createChain("/d3/5")
        src_chain.publish("aaa", 0)
        src_chain.publish("bbb", 0)
        thread { daemon(src) }

        // DESTINY
        val dst = Host_create("tests/dst/", 8331)
        dst.createChain("/d3/5")
        thread { daemon(dst) }
        Thread.sleep(100)

        main(arrayOf("chain","send","/d3/5","localhost:8331"))
        Thread.sleep(100)

        main(arrayOf("--host=localhost:8331","host","stop"))
        main(arrayOf("host","stop"))
        Thread.sleep(100)

        // TODO: check if dst == src
        // $ diff -r tests/dst/ tests/src/
    }

    @Test
    fun e1_graph () {
        val chain = Chain("tests/local/", "/graph",0)
        chain.save()
        val genesis = Node(0,0, "", emptyArray())
        genesis.hash = chain.toGenHash()
        chain.saveNode(genesis)

        val a1 = Node(0,0,"a1", arrayOf(chain.toGenHash()))
        val b1 = Node(0,0,"b1", arrayOf(chain.toGenHash()))
        a1.setNonceHashWithWork(0)
        b1.setNonceHashWithWork(0)
        chain.saveNode(a1)
        chain.saveNode(b1)
        chain.reheads(a1)
        chain.reheads(b1)

        //val ab2 =
        chain.publish("ab2", 0)

        val b2 = Node(0,0,"b2", arrayOf(b1.hash!!))
        b2.setNonceHashWithWork(0)
        chain.saveNode(b2)
        chain.reheads(b2)

        chain.publish("ab3", 0)
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
        val h1_chain = h1.createChain("/xxx/0")
        h1_chain.publish("h1_1", 0)
        h1_chain.publish("h1_2", 0)

        val h2 = Host_create("tests/h2/", 8331)
        val h2_chain = h2.createChain("/xxx/0")
        h2_chain.publish("h2_1", 0)
        h2_chain.publish("h2_2", 0)

        Thread.sleep(100)
        thread { daemon(h1) }
        thread { daemon(h2) }
        Thread.sleep(100)
        main(arrayOf("--host=localhost:8331","chain","send","/xxx/0","localhost"))
        Thread.sleep(100)
        main(arrayOf("--host=localhost:8331","host","stop"))
        main(arrayOf("host","stop"))
        Thread.sleep(100)

        // TODO: check if 8332 (h2) < 8331 (h1)
        // $ diff -r tests/h1 tests/h2/
    }

    @Test
    fun m1_args () {
        //a_reset()
        main(arrayOf("host","create","tests/M1/"))
        thread {
            main(arrayOf("host","start","tests/M1/"))
        }
        Thread.sleep(100)
        main(arrayOf("chain","create","/xxx/0"))
        main(arrayOf("chain","put","/xxx/0","text","aaa"))
        main(arrayOf("chain","put","/xxx/0","file","tests/M1/host"))
        main(arrayOf("chain","get","--host=localhost:8330","/xxx/0", "0_826ffb4505831e6355edc141f49b1ccf5b489b9f03760f0f2fed4eeed419c6fe"))
        main(arrayOf("chain","get","/xxx/0", "0_826ffb4505831e6355edc141f49b1ccf5b489b9f03760f0f2fed4eeed419c6fe"))
        main(arrayOf("host","stop"))

        // TODO: check genesis 2x, "aaa", "host"
        // $ cat tests/M1/chains/xxx/0/*
    }
}
