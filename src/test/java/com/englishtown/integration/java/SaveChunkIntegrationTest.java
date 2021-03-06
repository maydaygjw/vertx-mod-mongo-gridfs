/*
 * The MIT License (MIT)
 * Copyright © 2013 Englishtown <opensource@englishtown.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.englishtown.integration.java;

import com.englishtown.vertx.GridFSModule;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import java.io.UnsupportedEncodingException;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

/**
 * Integration tests for the saveChunk operation
 */
public class SaveChunkIntegrationTest extends TestVerticle {

    private EventBus eventBus;
    private final String address = GridFSModule.DEFAULT_ADDRESS + "/saveChunk";

    @Test
    public void testSaveFile_Empty_Bytes() {

        Buffer message = new Buffer();

        eventBus.send(address, message, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                IntegrationTestHelper.verifyErrorReply(message, "error parsing byte[] message.  see the documentation for the correct format");
            }
        });

    }

    @Test
    public void testSaveFile_Invalid_Json() throws Exception {

        byte[] invalid = "{\"property\": 1".getBytes("UTF-8");
        Buffer buffer = new Buffer();
        buffer.appendInt(invalid.length);
        buffer.appendBytes(invalid);
        buffer.appendBytes(new byte[10]);

        eventBus.send(address, buffer, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                IntegrationTestHelper.verifyErrorReply(message, "error parsing byte[] message.  see the documentation for the correct format");
            }
        });

    }

    @Test
    public void testSaveFile_No_Data() throws Exception {

        Buffer message = getMessage(new JsonObject(), new byte[0]);

        eventBus.send(address, message, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                IntegrationTestHelper.verifyErrorReply(message, "chunk data is missing");
            }
        });

    }

    @Test
    public void testSaveFile_Empty_Json() throws Exception {

        Buffer message = getMessage(new JsonObject(), new byte[10]);

        eventBus.send(address, message, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                IntegrationTestHelper.verifyErrorReply(message, "files_id must be specified");
            }
        });

    }

    @Test
    public void testSaveFile_Missing_N() throws Exception {

        ObjectId id = new ObjectId();
        JsonObject jsonObject = new JsonObject().putString("files_id", id.toString());
        Buffer message = getMessage(jsonObject, new byte[10]);

        eventBus.send(address, message, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                IntegrationTestHelper.verifyErrorReply(message, "n must be specified");
            }
        });

    }

    @Test
    public void testSaveFile() throws Exception {

        String files_id = new ObjectId().toString();
        int n = 0;
        String bucket = "it";

        final JsonObject jsonObject = new JsonObject()
                .putString("files_id", files_id)
                .putNumber("n", n)
                .putString("bucket", bucket);

        Buffer message = getMessage(jsonObject, new byte[10]);

        eventBus.send(address, message, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                assertEquals("ok", message.body().getString("status"));

                jsonObject.putString("action", "getChunk");

                eventBus.send(GridFSModule.DEFAULT_ADDRESS, jsonObject, new Handler<Message<byte[]>>() {
                    @Override
                    public void handle(Message<byte[]> reply) {
                        assertEquals(10, reply.body().length);
                        testComplete();
                    }
                });
            }
        });

    }

    private Buffer getMessage(JsonObject jsonObject, byte[] data) throws UnsupportedEncodingException {

        Buffer buffer = new Buffer();
        byte[] jsonBytes = jsonObject.encode().getBytes("UTF-8");

        buffer.appendInt(jsonBytes.length);
        buffer.appendBytes(jsonBytes);
        buffer.appendBytes(data);

        return buffer;
    }

    @Override
    public void start(Future<Void> startedResult) {
        eventBus = vertx.eventBus();
        IntegrationTestHelper.onVerticleStart(this, startedResult, "/config.json");
    }

}
