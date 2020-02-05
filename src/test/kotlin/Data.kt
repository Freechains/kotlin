import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

import data.*

class Tests {
    @Test
    @DisplayName("Teste 1")
    fun Test1 () {
        assertThat(Chain("oi",0)).isEqualTo(Chain("oi",0))

        val node = Node(0,0,"", emptyArray(), "")
        node.setNonceHashWithZeros(5)
        println(node.hash)

        node.saveToFS()
    }
}
