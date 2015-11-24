//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http.HttpGenerator.ResponseInfo;
import org.eclipse.jetty.util.BufferUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HttpGeneratorServerTest
{
    @Test
    public void testSendServerXPoweredBy() throws Exception
    {
        ByteBuffer header = BufferUtil.allocate(8096);
        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_1, new HttpFields(), -1, 200, null, false);
        HttpFields fields = new HttpFields();
        fields.add(HttpHeader.SERVER, "SomeServer");
        fields.add(HttpHeader.X_POWERED_BY, "SomePower");
        ResponseInfo infoF = new ResponseInfo(HttpVersion.HTTP_1_1, fields, -1, 200, null, false);
        String head;

        HttpGenerator gen = new HttpGenerator(true, true);
        gen.generateResponse(info, header, null, null, true);
        head = BufferUtil.toString(header);
        BufferUtil.clear(header);
        assertThat(head, containsString("HTTP/1.1 200 OK"));
        assertThat(head, containsString("Server: Jetty(9.x.x)"));
        assertThat(head, containsString("X-Powered-By: Jetty(9.x.x)"));
        gen.reset();
        gen.generateResponse(infoF, header, null, null, true);
        head = BufferUtil.toString(header);
        BufferUtil.clear(header);
        assertThat(head, containsString("HTTP/1.1 200 OK"));
        assertThat(head, not(containsString("Server: Jetty(9.x.x)")));
        assertThat(head, containsString("Server: SomeServer"));
        assertThat(head, containsString("X-Powered-By: Jetty(9.x.x)"));
        assertThat(head, containsString("X-Powered-By: SomePower"));
        gen.reset();

        gen = new HttpGenerator(false, false);
        gen.generateResponse(info, header, null, null, true);
        head = BufferUtil.toString(header);
        BufferUtil.clear(header);
        assertThat(head, containsString("HTTP/1.1 200 OK"));
        assertThat(head, not(containsString("Server: Jetty(9.x.x)")));
        assertThat(head, not(containsString("X-Powered-By: Jetty(9.x.x)")));
        gen.reset();
        gen.generateResponse(infoF, header, null, null, true);
        head = BufferUtil.toString(header);
        BufferUtil.clear(header);
        assertThat(head, containsString("HTTP/1.1 200 OK"));
        assertThat(head, not(containsString("Server: Jetty(9.x.x)")));
        assertThat(head, containsString("Server: SomeServer"));
        assertThat(head, not(containsString("X-Powered-By: Jetty(9.x.x)")));
        assertThat(head, containsString("X-Powered-By: SomePower"));
        gen.reset();
    }

    @Test
    public void testResponseNoContent() throws Exception
    {
        ByteBuffer header = BufferUtil.allocate(8096);

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_1, new HttpFields(), -1, 200, null, false);
        info.getHttpFields().add("Last-Modified", DateGenerator.__01Jan1970);

        result = gen.generateResponse(info, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);

        result = gen.generateResponse(info, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String head = BufferUtil.toString(header);
        BufferUtil.clear(header);

        result = gen.generateResponse(null, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(0, gen.getContentPrepared());
        assertThat(head, containsString("HTTP/1.1 200 OK"));
        assertThat(head, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertThat(head, containsString("Content-Length: 0"));
    }

    @Test
    public void testResponseUpgrade() throws Exception
    {
        ByteBuffer header = BufferUtil.allocate(8096);

        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_1, new HttpFields(), -1, 101, null, false);
        info.getHttpFields().add("Upgrade", "WebSocket");
        info.getHttpFields().add("Connection", "Upgrade");
        info.getHttpFields().add("Sec-WebSocket-Accept", "123456789==");

        result = gen.generateResponse(info, header, null, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        String head = BufferUtil.toString(header);
        BufferUtil.clear(header);

        result = gen.generateResponse(info, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertEquals(0, gen.getContentPrepared());

        assertThat(head, startsWith("HTTP/1.1 101 Switching Protocols"));
        assertThat(head, containsString("Upgrade: WebSocket\r\n"));
        assertThat(head, containsString("Connection: Upgrade\r\n"));
    }

    @Test
    public void testResponseWithChunkedContent() throws Exception
    {
        ByteBuffer header = BufferUtil.allocate(4096);
        ByteBuffer chunk = BufferUtil.allocate(HttpGenerator.CHUNK_SIZE);
        ByteBuffer content0 = BufferUtil.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtil.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_1, new HttpFields(), -1, 200, null, false);
        info.getHttpFields().add("Last-Modified", DateGenerator.__01Jan1970);
        result = gen.generateResponse(info, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtil.toString(header);
        BufferUtil.clear(header);
        out += BufferUtil.toString(content0);
        BufferUtil.clear(content0);

        result = gen.generateResponse(null, null, chunk, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtil.toString(chunk);
        BufferUtil.clear(chunk);
        out += BufferUtil.toString(content1);
        BufferUtil.clear(content1);

        result = gen.generateResponse(null, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
        out += BufferUtil.toString(chunk);
        BufferUtil.clear(chunk);

        result = gen.generateResponse(null, null, chunk, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertThat(out, containsString("HTTP/1.1 200 OK"));
        assertThat(out, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertThat(out, not(containsString("Content-Length")));
        assertThat(out, containsString("Transfer-Encoding: chunked"));
        assertThat(out, containsString("\r\n\r\nD\r\n"));
        assertThat(out, containsString("\r\nHello World! \r\n"));
        assertThat(out, containsString("\r\n2E\r\n"));
        assertThat(out, containsString("\r\nThe quick brown fox jumped over the lazy dog. \r\n"));
        assertThat(out, containsString("\r\n0\r\n"));
    }

    @Test
    public void testResponseWithKnownContent() throws Exception
    {
        ByteBuffer header = BufferUtil.allocate(4096);
        ByteBuffer content0 = BufferUtil.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtil.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(null, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_1, new HttpFields(), 59, 200, null, false);
        info.getHttpFields().add("Last-Modified", DateGenerator.__01Jan1970);
        result = gen.generateResponse(info, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        String out = BufferUtil.toString(header);
        BufferUtil.clear(header);
        out += BufferUtil.toString(content0);
        BufferUtil.clear(content0);

        result = gen.generateResponse(null, null, null, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtil.toString(content1);
        BufferUtil.clear(content1);

        result = gen.generateResponse(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertThat(out, containsString("HTTP/1.1 200 OK"));
        assertThat(out, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertThat(out, not(containsString("chunked")));
        assertThat(out, containsString("Content-Length: 59"));
        assertThat(out, containsString("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
    }

    @Test
    public void test100ThenResponseWithContent() throws Exception
    {
        ByteBuffer header = BufferUtil.allocate(4096);
        ByteBuffer content0 = BufferUtil.toBuffer("Hello World! ");
        ByteBuffer content1 = BufferUtil.toBuffer("The quick brown fox jumped over the lazy dog. ");
        HttpGenerator gen = new HttpGenerator();

        HttpGenerator.Result result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, null, null, null, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, header, null, null, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMPLETING_1XX, gen.getState());
        String out = BufferUtil.toString(header);

        result = gen.generateResponse(null, null, null, null, false);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        assertThat(out, containsString("HTTP/1.1 100 Continue"));

        result = gen.generateResponse(null, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_INFO, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_1, new HttpFields(), 59, 200, null, false);
        info.getHttpFields().add("Last-Modified", DateGenerator.__01Jan1970);
        result = gen.generateResponse(info, null, null, content0, false);
        assertEquals(HttpGenerator.Result.NEED_HEADER, result);
        assertEquals(HttpGenerator.State.START, gen.getState());

        result = gen.generateResponse(info, header, null, content0, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

        out = BufferUtil.toString(header);
        BufferUtil.clear(header);
        out += BufferUtil.toString(content0);
        BufferUtil.clear(content0);

        result = gen.generateResponse(null, null, null, content1, false);
        assertEquals(HttpGenerator.Result.FLUSH, result);
        assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
        out += BufferUtil.toString(content1);
        BufferUtil.clear(content1);

        result = gen.generateResponse(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.CONTINUE, result);
        assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

        result = gen.generateResponse(null, null, null, null, true);
        assertEquals(HttpGenerator.Result.DONE, result);
        assertEquals(HttpGenerator.State.END, gen.getState());

        assertThat(out, containsString("HTTP/1.1 200 OK"));
        assertThat(out, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
        assertThat(out, not(containsString("chunked")));
        assertThat(out, containsString("Content-Length: 59"));
        assertThat(out, containsString("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
    }

    @Test
    public void testConnectionKeepAliveWithAdditionalCustomValue() throws Exception
    {
        HttpGenerator generator = new HttpGenerator();

        HttpFields fields = new HttpFields();
        fields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE);
        String customValue = "test";
        fields.add(HttpHeader.CONNECTION, customValue);
        ResponseInfo info = new ResponseInfo(HttpVersion.HTTP_1_0, fields, -1, 200, "OK", false);
        ByteBuffer header = BufferUtil.allocate(4096);
        HttpGenerator.Result result = generator.generateResponse(info, header, null, null, true);
        Assert.assertSame(HttpGenerator.Result.FLUSH, result);
        String headers = BufferUtil.toString(header);
        Assert.assertTrue(headers.contains(HttpHeaderValue.KEEP_ALIVE.asString()));
        Assert.assertTrue(headers.contains(customValue));
    }
}
