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
class BatchCancelIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BatchService batchService;

    @Autowired
    private BtcService btcService;

    @Test
    void cancelStopsProcessing() throws Exception {
        // make btcService slow so we can cancel mid-way
        Mockito.when(btcService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    String addr = invocation.getArgument(1);
                    Wallet w = new Wallet();
                    w.setAddress(addr);
                    w.setAmount(0.1);
                    Thread.sleep(500);
                    return w;
                });

        String content = "a\nb\nc\nd\ne\nf\ng\nh";
        MockMultipartFile file = new MockMultipartFile("file", "list.txt", "text/plain", content.getBytes());
        BatchJob job = batchService.startBatchFromFile(file, "", 0);
        assertNotNull(job);

        // wait briefly so processing starts
        Thread.sleep(600);

        // cancel via controller endpoint
        URI uri = new URI("http://localhost:" + port + "/batch/cancel/" + job.getId());
        ResponseEntity<String> resp = restTemplate.postForEntity(uri, null, String.class);
        assertEquals(200, resp.getStatusCodeValue());
        assertTrue(resp.getBody().contains("\"cancelled\":true"));

        // wait for job to be marked done
        long start = System.currentTimeMillis();
        while (!job.isDone() && System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(10)) {
            Thread.sleep(100);
        }
        assertTrue(job.isDone());
        assertTrue(job.isCancelled());
        assertTrue(job.getProcessed().get() < job.getAddresses().size());
    }
}