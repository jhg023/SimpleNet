package com.github.simplenet;/*
 * MIT License
 *
 * Copyright (c) 2020 Jacob Glickman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

final class ConnectionTest {
    
    private static final String HOST = "localhost";
    
    private static final int PORT = 43594;
    
    private Client client;
    
    private Server server;
    
    private CountDownLatch latch;
    
    @BeforeEach
    void beforeEach() {
        client = new Client();
        server = new Server();
        latch = new CountDownLatch(1);
        server.bind(HOST, PORT, 1);
    }
    
    @AfterEach
    void afterEach() throws InterruptedException {
        client.connect(HOST, PORT);
        
        try {
            if (!latch.await(500L, TimeUnit.MILLISECONDS)) {
                fail();
            }
        } finally {
            client.close();
            server.close();
        }
    }
    
    @Test
    void testConnectedClientsAfterServerCloseClient() {
        server.onConnect(client -> {
            assertEquals(1, server.getNumConnectedClients());
            client.close();
            assertEquals(0, server.getNumConnectedClients());
            latch.countDown();
        });
    }
}
