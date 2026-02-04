package com.freelkee.btcbalancechecker.service;

import com.freelkee.btcbalancechecker.model.BatchJob;
import com.freelkee.btcbalancechecker.model.Wallet;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Service
public class BatchServiceImpl implements BatchService {

    @Autowired
    private BtcService btcService;

    @Value("${batch.concurrency:5}")
    private int concurrency;

    private final ConcurrentMap<String, BatchJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Emitters for SSE subscriptions per job
    private final ConcurrentMap<String, java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter>> emitters = new ConcurrentHashMap<>();

    // Keep per-job pools so we can cancel them
    private final ConcurrentMap<String, ExecutorService> jobPools = new ConcurrentHashMap<>();

    @Override
    public BatchJob startBatchFromFile(MultipartFile file, String currency, int offset) throws IOException {
        List<String> addresses = parseAddresses(file);
        String jobId = UUID.randomUUID().toString();
        BatchJob job = new BatchJob(jobId, currency, offset, addresses, Collections.synchronizedList(new ArrayList<>()), new java.util.concurrent.atomic.AtomicInteger(0), new java.util.concurrent.atomic.AtomicInteger(0), false);
        jobs.put(jobId, job);

        // initialize per-address status
        for (String addr : addresses) {
            job.getStatuses().put(addr, "PENDING");
        }

        // create a limited thread pool per job to control concurrency
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, concurrency));

        // store pool so it can be cancelled
        jobPools.put(jobId, pool);

        for (String addr : addresses) {
            pool.submit(() -> {
                // check cancellation status quickly
                if (job.isCancelled()) return;
                job.getStatuses().put(addr, "IN_PROGRESS");
                notifyJobUpdate(job);
                try {
                    Wallet w = btcService.getWallet(currency == null || currency.isBlank() ? "" : currency, addr, offset);
                    job.getResults().add(w);
                    job.getResultsMap().put(addr, w);
                    job.getStatuses().put(addr, "DONE");
                } catch (Exception e) {
                    Wallet w = new Wallet();
                    w.setAddress(addr);
                    w.setAmount(0);
                    job.getResults().add(w);
                    job.getResultsMap().put(addr, w);
                    job.getStatuses().put(addr, "FAILED");
                    job.getFailed().incrementAndGet();
                } finally {
                    job.getProcessed().incrementAndGet();
                    // push update to SSE subscribers
                    notifyJobUpdate(job);
                }
            });
        }

        // when all tasks submitted, shutdown pool and mark completion async
        executor.submit(() -> {
            pool.shutdown();
            try {
                pool.awaitTermination(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            job.setDone(true);
            notifyJobUpdate(job);
            jobPools.remove(jobId);
        });

        return job;
    }

    private void notifyJobUpdate(BatchJob job) {
        java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> list = emitters.get(job.getId());
        if (list == null || list.isEmpty()) return;
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("processed", job.getProcessed().get());
        data.put("total", job.getAddresses().size());
        data.put("failed", job.getFailed().get());
        data.put("done", job.isDone());
        data.put("cancelled", job.isCancelled());
        data.put("statuses", job.getStatuses());

        for (org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter : new java.util.ArrayList<>(list)) {
            try {
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("status").data(data));
                if (job.isDone()) {
                    emitter.complete();
                    list.remove(emitter);
                }
            } catch (Exception e) {
                try { emitter.completeWithError(e);} catch (Exception ex) {}
                list.remove(emitter);
            }
        }
    }

    @Override
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribe(String jobId) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(0L);
        emitters.computeIfAbsent(jobId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(emitter);

        // cleanup on completion/timeout/error
        emitter.onCompletion(() -> emitters.getOrDefault(jobId, java.util.Collections.emptyList()).remove(emitter));
        emitter.onTimeout(() -> {
            try { emitter.complete(); } catch (Exception ignore) {}
            emitters.getOrDefault(jobId, java.util.Collections.emptyList()).remove(emitter);
        });
        emitter.onError((ex) -> emitters.getOrDefault(jobId, java.util.Collections.emptyList()).remove(emitter));

        // send initial status if job exists
        BatchJob job = jobs.get(jobId);
        if (job != null) {
            try {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("processed", job.getProcessed().get());
                data.put("total", job.getAddresses().size());
                data.put("failed", job.getFailed().get());
                data.put("done", job.isDone());
                data.put("cancelled", job.isCancelled());
                emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("status").data(data));
            } catch (Exception ignored) {}
        }

        return emitter;
    }

    private List<String> parseAddresses(MultipartFile file) throws IOException {
        List<String> ret = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("[,;\\s]+");
                for (String p : parts) {
                    String t = p.trim();
                    if (!t.isEmpty()) ret.add(t);
                }
            }
        }
        return ret;
    }

    @Override
    public BatchJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        for (ExecutorService es : jobPools.values()) {
            es.shutdownNow();
        }
    }

    @Override
    public boolean cancel(String jobId) {
        BatchJob job = jobs.get(jobId);
        if (job == null) return false;
        job.setCancelled(true);
        ExecutorService pool = jobPools.get(jobId);
        if (pool != null) {
            pool.shutdownNow();
            jobPools.remove(jobId);
        }
        job.setDone(true);
        notifyJobUpdate(job);
        return true;
    }
}
