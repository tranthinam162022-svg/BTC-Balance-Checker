package com.freelkee.btcbalancechecker.service;

import com.freelkee.btcbalancechecker.model.BatchJob;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public interface BatchService {
    BatchJob startBatchFromFile(MultipartFile file, String currency, int offset) throws IOException;
    BatchJob getJob(String jobId);

    /**
     * Subscribe to SSE events for a given jobId. Returns an {@link SseEmitter} that will receive
     * periodic status updates and a final completion event.
     */
    SseEmitter subscribe(String jobId);

    /** Cancel a currently running job. Returns true if the job was found and cancellation requested. */
    boolean cancel(String jobId);
}
