package com.vmware.loginsight.examples.api.query;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;

/*
 * Sample code showing how to invoke vRealize Log Insight's Query API from Java.
 * Note this sample code is not supported and is meant to serve as an example of how the API can be called.
 * Sample output on vSphere logs:
 *   Full response: {"complete":true,"events":[{"text":"2016-02-29T21:00:44.040Z sg01-1-esx61.oc.vmware.com Vpxa: verbose vpxa[295B9B70] [Originator@6876 sub=VpxaHalCnxHostagent opID=WFU-79aec678] [WaitForUpdatesDone] Completed callback","timestamp":1456779694063,"fields":[{"name":"hostname","startPosition":25,"length":26},{"name":"event_type","content":"v4_59330762"},{"name":"appname","startPosition":52,"length":4},{"name":"vcore","content":"1"},{"name":"source","content":"10.120.14.25"},{"name":"silo","content":"sg01"},{"name":"tenant","content":"onecloud"}]},{"text":"2016-02-29T21:00:44.037Z sg01-1-esx61.oc.vmware.com Vpxa: verbose vpxa[28EE4B70] [Originator@6876 sub=VpxaHalCnxHostagent opID=WFU-5b949c80] [WaitForUpdatesDone] Completed callback","timestamp":1456779694063,"fields":[{"name":"hostname","startPosition":25,"length":26},{"name":"event_type","content":"v4_59330762"},{"name":"appname","startPosition":52,"length":4},{"name":"vcore","content":"1"},{"name":"source","content":"10.120.14.25"},{"name":"silo","content":"sg01"},{"name":"tenant","content":"onecloud"}]},{"text":"2016-02-29T20:59:22.603Z sg01-1-esx22.oc.vmware.com Vpxa: verbose vpxa[FFDD9B70] [Originator@6876 sub=VpxaHalCnxHostagent opID=WFU-1456864d] [WaitForUpdatesDone] Completed callback","timestamp":1456779694045,"fields":[{"name":"hostname","startPosition":25,"length":26},{"name":"event_type","content":"v4_59330762"},{"name":"appname","startPosition":52,"length":4},{"name":"vcore","content":"1"},{"name":"source","content":"10.120.14.25"},{"name":"silo","content":"sg01"},{"name":"tenant","content":"onecloud"}]},{"text":"2016-02-29T20:59:22.599Z sg01-1-esx22.oc.vmware.com Vpxa: verbose vpxa[59B17B70] [Originator@6876 sub=VpxaHalCnxHostagent opID=WFU-73f3f10c] [WaitForUpdatesDone] Completed callback","timestamp":1456779694044,"fields":[{"name":"hostname","startPosition":25,"length":26},{"name":"event_type","content":"v4_59330762"},{"name":"appname","startPosition":52,"length":4},{"name":"vcore","content":"1"},{"name":"source","content":"10.120.14.25"},{"name":"silo","content":"sg01"},{"name":"tenant","content":"onecloud"}]},{"text":"2016-02-29T21:00:06.465Z sg01-1-esx57.oc.vmware.com Vpxa: verbose vpxa[3D4D8B70] [Originator@6876 sub=VpxaHalCnxHostagent opID=WFU-3beb4fbf] [WaitForUpdatesDone] Completed callback","timestamp":1456779694000,"fields":[{"name":"hostname","startPosition":25,"length":26},{"name":"event_type","content":"v4_59330762"},{"name":"appname","startPosition":52,"length":4},{"name":"vcore","content":"1"},{"name":"source","content":"10.120.14.25"},{"name":"silo","content":"sg01"},{"name":"tenant","content":"onecloud"}]}]}
 *   First event's 'source' field: 10.120.14.25
 */

public class InvokeQueryAPI {

    @Test
    public void invokeQueryAPI() throws Exception {
        // change me
        final String HOST = "myloginsight.example.com";
        final String USERNAME = "my_username";
        final String PASSWORD = "my_password";
        // don't change me
        final String SCHEME = "https";
        final String API_PREFIX = "/api/v1/";
        final String URL = SCHEME + "://" + HOST + API_PREFIX;
        final String SESSIONS = "sessions";
        final String EVENTS = "events";
        final String CONTENT_TYPE = "Content-Type", JSON = "application/json";
        final String AUTHORIZATION = "Authorization";
        final String BEARER = "Bearer ";
        // trust all certificates & hostnames (don't do this in production!)
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(
            null,
            new TrustManager[] {
                new X509TrustManager() {
                    @Override public X509Certificate[] getAcceptedIssuers() { return null; }
                    @Override public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    @Override public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                 }
            },
            new SecureRandom());
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        // HTTP client
        HttpClient client = HttpClients.custom().setSSLSocketFactory(sslSocketFactory).build();
        // authenticate
        String sessionId = null;
        HttpPost request1 = new HttpPost(URL + SESSIONS);
        request1.setHeader(CONTENT_TYPE, JSON);
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("username", USERNAME).put("password", PASSWORD);
        request1.setEntity(new StringEntity(payload.toString()));
        HttpResponse response = client.execute(request1);
        payload = (ObjectNode) new ObjectMapper().readTree(IOUtils.toString(response.getEntity().getContent()));
        sessionId = payload.get("sessionId").asText(); // something like 1LOZGXF0A...Yms0xx7jUY=
        // and finally.,.. retrieve the five most-recent event containing "completed callback"
        HttpGet request2 = new HttpGet(URL + EVENTS + "/text/completed+callback?limit=5");
        request2.setHeader(AUTHORIZATION, BEARER + sessionId);
        response = client.execute(request2);
        payload = (ObjectNode) (new ObjectMapper().readTree(IOUtils.toString(response.getEntity().getContent())));
        System.out.println("Full response: " + payload);
        System.out.println("First event's 'source' field: " +
            Arrays.asList(Iterators.toArray(payload.get("events").get(0).get("fields").elements(), JsonNode.class)).
            stream().
            filter(node -> node.get("name").asText().equals("source")).
            findFirst().
            get().
            get("content").
            asText());
    }

}
