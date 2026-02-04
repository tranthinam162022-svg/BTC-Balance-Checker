package com.freelkee.btcbalancechecker.service;

import com.freelkee.btcbalancechecker.model.BatchJob;
import com.freelkee.btcbalancechecker.model.Wallet;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BatchServiceImplTest {

    @Test
    void startBatchFromFile_parsesAndProcessesAllAddresses() throws Exception {
        BtcService btcService = Mockito.mock(BtcService.class);
        Mockito.when(btcService.getWallet(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    String addr = invocation.getArgument(1);
                    Wallet w = new Wallet();
                    w.setAddress(addr);
                    w.setAmount(0.123);
                    return w;
                });

        BatchServiceImpl service = new BatchServiceImpl();
        // inject mock
        java.lang.reflect.Field f = BatchServiceImpl.class.getDeclaredField("btcService");
        f.setAccessible(true);
        f.set(service, btcService);

        // use small input
        String content = "addr1\naddr2,addr3";
        MockMultipartFile file = new MockMultipartFile("file", "list.txt", "text/plain", content.getBytes());

        BatchJob job = service.startBatchFromFile(file, "", 0);
        assertNotNull(job);
        assertEquals(3, job.getAddresses().size());

        // wait for completion with timeout
        long start = System.currentTimeMillis();
        while (!job.isDone() && System.currentTimeMillis() - start < TimeUnit.SECONDS.toMillis(10)) {
            Thread.sleep(100);
        }

        assertTrue(job.isDone(), "Job should complete within timeout");
        assertEquals(3, job.getProcessed().get());
        assertEquals(0, job.getFailed().get());
        assertEquals(3, job.getResults().size());
    }

    @Test
    void subscribe_returnsEmitter() {
        BatchServiceImpl service = new BatchServiceImpl();
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = service.subscribe("nonexistent");
        assertNotNull(emitter);
    }
}