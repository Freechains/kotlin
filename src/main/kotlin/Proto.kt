package org.freechains.kotlin

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf

@Serializable
data class Proto_Header (
    val F    : Byte,
    var C    : Byte,
    val type : Short
)

@Serializable
data class Proto_Node_HH (
    val height : Long,
    val hash   : ByteArray
)

@Serializable
data class Proto_Get (
    val nw: Chain_NW,
    val hh: Node_HH
)

@Serializable
data class Proto_Put (
    val nw:  Chain_NW,
    val pay: ByteArray
)

// CONVERSIONS

fun Proto_Node_HH.toNodeHH () : Node_HH {
    return Node_HH(this.height, this.hash.toHexString())
}

fun ByteArray.toHeader () : Proto_Header {
    return ProtoBuf.load(Proto_Header.serializer(), this)
}

fun ByteArray.toChainNW () : Chain_NW {
    return ProtoBuf.load(Chain_NW.serializer(), this)
}

fun ByteArray.toProtoNodeHH () : Proto_Node_HH {
    return ProtoBuf.load(Proto_Node_HH.serializer(), this)
}

fun ByteArray.toProtoGet () : Proto_Get {
    return ProtoBuf.load(Proto_Get.serializer(), this)
}

fun ByteArray.toProtoPut () : Proto_Put {
    return ProtoBuf.load(Proto_Put.serializer(), this)
}