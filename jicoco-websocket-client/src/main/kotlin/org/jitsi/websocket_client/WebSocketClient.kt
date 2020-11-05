/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.websocket_client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jitsi.utils.logging2.Logger
import org.jitsi.utils.logging2.createChildLogger
import java.net.ConnectException

/**
 * A websocket client which sends messages and invokes a handler upon receiving
 * messages from the far side.  Sending is non-blocking, and the client has no
 * notion of correlating "responses" to "requests": if request/response
 * semantics are required then they must be implemented by a layer on top of
 * this class.
 */
class WebSocketClient(
    private val host: String,
    private val wsProtocol: WsProtocol,
    private val port: Int,
    /**
     * The path of the remote websocket URL
     */
    private val path: String,
    parentLogger: Logger,
    private val incomingMessageHandler: (Frame) -> Unit = {},
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets)
    },
    /**
     * The dispatcher which will be used for all of the request and response
     * processing.
     */
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger = createChildLogger(parentLogger)
    private val job = Job()
    private val coroutineScope = CoroutineScope(dispatcher + job)
    private val msgsToSend = Channel<Frame>(Channel.RENDEZVOUS)
    private var wsSession: DefaultClientWebSocketSession? = null

    fun sendString(data: String) {
        coroutineScope.launch {
            msgsToSend.send(Frame.Text(data))
        }
    }

    // Starts the run loop for sending and receiving websocket messages
    private suspend fun DefaultClientWebSocketSession.startLoop() {
        launch {
            for (msg in incoming) {
                incomingMessageHandler(msg)
            }
        }
        try {
            for (msg in msgsToSend) {
                send(msg)
            }
        } catch (e: ClosedReceiveChannelException) {
            logger.info("Websocket was closed")
            return
        } catch (e: CancellationException) {
            logger.info("Websocket job was cancelled")
            throw e
        } catch (t: Throwable) {
            logger.error("Error in websocket connection: ", t)
            return
        }
    }

    /**
     * Attempt to connect to the websocket server, returns true if the connection was
     * successful, false otherwise.
     *
     * Known exceptions (not necessarily exhaustive):
     * [ConnectException] if we can't connect
     * [IllegalArgumentException] if expected WSS, but the server is WS
     * [EOFException] if expected WS, but the server is WSS
     * [SunCertPathBuilderException] WSS cert issue
     */
    fun connect(): Boolean {
        return try {
            wsSession = runBlocking {
                client.webSocketSession {
                    url {
                        protocol = wsProtocol.toUrlProtocol()
                        host = this@WebSocketClient.host
                        port = this@WebSocketClient.port
                        path(path)
                    }
                }
            }
            true
        } catch (t: Throwable) {
            logger.error("Error connecting", t)
            false
        }
    }

    /**
     * Start the (asynchronous) loops to handle sending and receiving messages
     */
    fun run() {
        coroutineScope.launch {
            requireNotNull(wsSession)
            wsSession?.startLoop()
        }
    }

    /**
     * Stop and close the websocket connection
     */
    fun stop() {
        logger.info("Stopping")
        runBlocking {
            wsSession?.close(CloseReason(CloseReason.Codes.NORMAL, "bye"))
            job.cancelAndJoin()
        }
    }

    private fun WsProtocol.toUrlProtocol(): URLProtocol {
        return when (this) {
            WsProtocol.WS -> URLProtocol.WS
            WsProtocol.WSS -> URLProtocol.WSS
        }
    }
}
