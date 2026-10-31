package com.mdarman0964.vela

import org.junit.Assert.assertEquals
import org.junit.Test

class ExnessSigningTest {
    @Test fun ed25519SignatureMatchesRfc8032EmptyMessageVector() {
        val seed = "9d61b19deffd5a60ba844af492ec2cc44449c5697b326919703bac031cae7f60"
        val client = ExnessReadOnlyClient(
            ExnessCredentialVault.Credentials(
                gateway = "https://api.exness.com",
                accountId = "123",
                apiKey = "test-public-key-id",
                privateKey = seed,
            )
        )
        val signature = client.sign(ByteArray(0), seed)
        assertEquals(
            "5VZDAMNgrHKQhuLMgG6CioSHfx645dl02HPgZSJJAVVfuIIVkKM7rMYeOXAc-bRr0lv18FlbviRlUUFDjnoQCw",
            signature,
        )
    }
}
