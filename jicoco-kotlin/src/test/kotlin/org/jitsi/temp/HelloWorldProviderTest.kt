package org.jitsi.temp

import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec

class HelloWorldProviderTest : ShouldSpec() {

    init {
        "HelloWorldProvider" {
            val x = HelloWorldProvider()
            should("return the correct string") {
                x.getHelloWorld() shouldBe "Hello, world"
            }
        }
    }
}