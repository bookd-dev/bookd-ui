package com.bookd.app.data.vm

import com.bookd.app.data.api.NoNetworkConfigException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppErrorEventTest {

    @Test
    fun `given error consumed by first collector when page collector is rebuilt then error is not replayed`() {
        runBlocking {
            val errorEvents = AppErrorEventQueue()
            errorEvents.send(NoNetworkConfigException())

            assertIs<NoNetworkConfigException>(errorEvents.events.first())

            val replayedError = withTimeoutOrNull(100) { errorEvents.events.first() }
            assertNull(replayedError)
        }
    }

    @Test
    fun `given no active collector when error occurs then buffered event is delivered once collector starts`() {
        runBlocking {
            val errorEvents = AppErrorEventQueue()

            errorEvents.send(NoNetworkConfigException())

            assertIs<NoNetworkConfigException>(errorEvents.events.first())
        }
    }
}
