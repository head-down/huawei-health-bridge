package com.headdown.healthbridge

import com.headdown.healthbridge.data.SleepData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExampleTest {

    interface GreetingService {
        suspend fun greet(name: String): String
    }

    @Test
    fun `basic assertions work`() {
        val list = listOf(1, 2, 3)
        assertEquals(3, list.size)
        assertTrue(list.contains(2))
    }

    @Test
    fun `coroutine test runs suspend functions`() = runTest {
        suspend fun delayedGreeting(name: String): String {
            return "Hello, $name!"
        }

        val greeting = delayedGreeting("Kotlin")
        assertEquals("Hello, Kotlin!", greeting)
    }

    @Test
    fun `mockk verifies suspend function interaction`() = runTest {
        val service = mockk<GreetingService>()
        coEvery { service.greet("World") } returns "Hello, World!"

        val result = service.greet("World")
        assertEquals("Hello, World!", result)
        coVerify { service.greet("World") }
    }

    @Test
    fun `project data class works in test`() {
        val sleep = SleepData(
            startTime = 1700000000000,
            endTime = 1700003600000,
            sleepState = 2
        )

        assertEquals(1700000000000, sleep.startTime)
        assertEquals(1700003600000, sleep.endTime)
        assertEquals(2, sleep.sleepState)
    }
}
