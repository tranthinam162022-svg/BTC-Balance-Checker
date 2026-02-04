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
class MyControllerIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BatchService batchService;

    @Autowired
    private BtcService btcService;

    @Test
    void batchLifecycle_statusAndResultEndpointsWork() throws Exception {
        // mock btcService behavior
        Mockito.when(btcService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    String addr = invocation.getArgument(1);
                    Wallet w = new Wallet();
                    w.setAddress(addr);
                    w.setAmount(0.5);
                    return w;
                });

        String content = "addrA\naddrB,addrC";
        MockMultipartFile file = new MockMultipartFile("file", "list.txt", "text/plain", content.getBytes());

        // start job via service to get jobId (controller upload uses same service)
        BatchJob job = batchService.startBatchFromFile(new org.springframework.mock.web.MockMultipartFile("file","list.txt","text/plain",content.getBytes()), "", 0);
        assertNotNull(job);
        assertEquals(3, job.getAddresses().size());

        // wait for completion
        long start = System.currentTimeMillis();
        while (!job.isDone() && System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(10)) {
            Thread.sleep(100);
        }
        assertTrue(job.isDone());

        // call status endpoint
        URI statusUri = new URI("http://localhost:" + port + "/batch/status/" + job.getId());
        ResponseEntity<String> statusResp = restTemplate.getForEntity(statusUri, String.class);
        assertEquals(200, statusResp.getStatusCodeValue());
        assertTrue(statusResp.getBody().contains("\"done\":true"));

        // call result page and expect addresses are present
        URI resultUri = new URI("http://localhost:" + port + "/batch/result/" + job.getId());
        ResponseEntity<String> resultResp = restTemplate.getForEntity(resultUri, String.class);
        assertEquals(200, resultResp.getStatusCodeValue());
        assertTrue(resultResp.getBody().contains("addrA"));
        assertTrue(resultResp.getBody().contains("addrB"));
        assertTrue(resultResp.getBody().contains("addrC"));
    }
}