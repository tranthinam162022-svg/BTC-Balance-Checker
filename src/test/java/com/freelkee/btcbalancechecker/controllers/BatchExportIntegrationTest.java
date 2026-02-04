package com.freelkee.btcbalancechecker.controllers;

import com.freelkee.btcbalancechecker.model.BatchJob;
import com.freelkee.btcbalancechecker.model.Wallet;
import com.freelkee.btcbalancechecker.service.BatchService;
import com.freelkee.btcbalancechecker.service.BtcService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=update"
})
class BatchExportIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BatchService batchService;

    @Autowired
    private BtcService btcService;

    @Test
    void exportCsvAndJsonWork() throws Exception {
        Mockito.when(btcService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    String addr = invocation.getArgument(1);
                    Wallet w = new Wallet();
                    w.setAddress(addr);
                    w.setAmount(0.5);
                    w.setCurrency("");
                    return w;
                });

        String content = "x\ny\nz";
        MockMultipartFile file = new MockMultipartFile("file", "list.txt", "text/plain", content.getBytes());
        BatchJob job = batchService.startBatchFromFile(file, "", 0);

        long start = System.currentTimeMillis();
        while (!job.isDone() && System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(10)) {
            Thread.sleep(100);
        }
        assertTrue(job.isDone());

        URI csvUri = new URI("http://localhost:" + port + "/batch/export/" + job.getId() + "?format=csv");
        ResponseEntity<String> csvResp = restTemplate.getForEntity(csvUri, String.class);
        assertEquals(200, csvResp.getStatusCodeValue());
        assertTrue(csvResp.getBody().contains("address,status,amount"));
        assertTrue(csvResp.getBody().contains("x,"));

        URI jsonUri = new URI("http://localhost:" + port + "/batch/export/" + job.getId() + "?format=json");
        ResponseEntity<String> jsonResp = restTemplate.getForEntity(jsonUri, String.class);
        assertEquals(200, jsonResp.getStatusCodeValue());
        assertTrue(jsonResp.getBody().startsWith("["));
        assertTrue(jsonResp.getBody().contains("\"address\":\"x\""));
    }
}