package org.jitsi.config

import com.typesafe.config.ConfigFactory
import io.kotlintest.IsolationMode
import io.kotlintest.specs.ShouldSpec
import io.kotlintest.shouldBe
import org.jitsi.utils.ConfigUtils

class ConfigExtensionsKtTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        "mask" {
            val config = ConfigFactory.parseString("""
                a {
                    pass-prop = s3cr3t
                    normal-prop = 10
                    b {
                        nested-pass-prop = 42
                        nested-normal-prop = hello
                    }
                }
            """.trimIndent())
            ConfigUtils.PASSWORD_SYS_PROPS = "pass"
            val maskedConfig = config.mask()
            should("mask out the right values") {
                maskedConfig.getString("a.pass-prop") shouldBe MASK
                maskedConfig.getString("a.b.nested-pass-prop") shouldBe MASK
            }
            should("not mask out other values") {
                maskedConfig.getInt("a.normal-prop") shouldBe 10
                maskedConfig.getString("a.b.nested-normal-prop") shouldBe "hello"
            }
            should("not affect the original config") {
                config.getString("a.pass-prop") shouldBe "s3cr3t"
                config.getInt("a.normal-prop") shouldBe 10
                config.getInt("a.b.nested-pass-prop") shouldBe 42
                config.getString("a.b.nested-normal-prop") shouldBe "hello"
            }
        }
    }
}