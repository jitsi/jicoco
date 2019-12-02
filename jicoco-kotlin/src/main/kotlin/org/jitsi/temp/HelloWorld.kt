package org.jitsi.temp


class HelloWorldProvider {
    fun getHelloWorld(): String = "Hello, world"
}

fun main() {
    println(HelloWorldProvider().getHelloWorld())
}
