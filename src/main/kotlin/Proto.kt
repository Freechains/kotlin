package freechains

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

@Serializable
data class Proto_Header (
    val F    : Byte,
    var C    : Byte,
    val type : Short
)
const val SIZE_PROTOBUF_HEADER = 7

@Serializable
data class Proto_1000_Chain (
    val name  : String,
    val zeros : Byte
)

@Serializable
data class Proto_1000_Height_Hash (
    val height : Long,
    val hash   : Array<Byte>
)

fun ByteArray.toHeader (): Proto_Header {
    return ProtoBuf.load(Proto_Header.serializer(), this)
}

fun server (host : Host) {
    val server = ServerSocket(host.port)
    println("Server is running on port ${server.localPort}")

    while (true) {
        val client = server.accept()
        println("Client connected: ${client.inetAddress.hostAddress}")
        thread { handler(client) }
    }

}

fun handler (client: Socket) {
    val reader = client.getInputStream()!!
    //val writer = client.getOutputStream()!!

    val header= reader.readNBytes(SIZE_PROTOBUF_HEADER).toHeader()
    assert(header.F.toChar()=='F' && header.C.toChar()=='C') { "invalid header signature" }

    println("Type: 0x${header.type.toString(16)}")
    /*
    when (header.type) {
        0x1000 -> xxx
        else   -> error("invalid header type")
    }
    */
}