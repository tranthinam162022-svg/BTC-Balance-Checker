package com.freelkee.btcbalancechecker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelkee.btcbalancechecker.model.BatchJob;
import com.freelkee.btcbalancechecker.model.Wallet;
import com.freelkee.btcbalancechecker.service.BatchService;
import com.freelkee.btcbalancechecker.service.BtcService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BatchSseIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private BatchService batchService;

    @MockBean
    private BtcService btcService;

    @Test
    void sseEmitsStatusAndCompletion() throws Exception {
        // mock slow responses to ensure multiple updates
        Mockito.when(btcService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    String addr = invocation.getArgument(1);
                    Wallet w = new Wallet();
                    w.setAddress(addr);
                    w.setAmount(0.1);
                    // simulate small delay per address
                    Thread.sleep(150);
                    return w;
                });

        String content = "aa\nbb\ncc";
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile("file", "list.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        BatchJob job = batchService.startBatchFromFile(file, "", 0);
        assertNotNull(job);

        // open SSE connection
        URI uri = new URI("http://localhost:" + port + "/batch/stream/" + job.getId());
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setReadTimeout((int) TimeUnit.SECONDS.toMillis(20));
        conn.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        ObjectMapper objectMapper = new ObjectMapper();

        boolean done = false;
        int updates = 0;
        long start = System.currentTimeMillis();
        while (!done && System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(20)) {
            String line = br.readLine();
            if (line == null) break;
            if (line.startsWith("data:")) {
                updates++;
                String json = line.substring(5).trim();
                @SuppressWarnings("unchecked")
                java.util.Map<String,Object> map = objectMapper.readValue(json, java.util.Map.class);
                Object doneVal = map.get("done");
                if (doneVal instanceof Boolean && ((Boolean) doneVal)) {
                    done = true;
                    break;
                }
            }
        }

        br.close();
        conn.disconnect();

        assertTrue(updates > 0, "Should receive at least one SSE update");
        assertTrue(job.isDone(), "Job should be marked done");
        assertTrue(job.getProcessed().get() >= 3);
    }
}