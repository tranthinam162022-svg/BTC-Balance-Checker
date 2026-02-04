package com.freelkee.btcbalancechecker.controllers;

import com.freelkee.btcbalancechecker.model.Wallet;
import com.freelkee.btcbalancechecker.service.BtcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class MyController {
    @Autowired
    private BtcService btcService;

    @GetMapping("/")
    public String showStartPage() {
        return "start";
    }

    @PostMapping("/process")
    public String processForm(@RequestParam("currency") String currency,
                              @RequestParam("bitcoinAddress") String bitcoinAddress,
                              @RequestParam("offset") int offset,
                              Model model) throws IOException {

        Wallet wallet = btcService.getWallet(currency, bitcoinAddress, offset);
        model.addAttribute("wallet", wallet);
        model.addAttribute("offset", offset);
        return "result";
    }

    @GetMapping("/info")
    public String showInfoPage() {
        return "info";
    }

    @GetMapping("/contacts")
    public String showContactsPage() {
        return "contacts";
    }

    @GetMapping("/batch")
    public String showBatchPage() {
        return "batch";
    }

    @PostMapping("/processBatch")
    public String processBatch(@RequestParam("currency") String currency,
                               @RequestParam("addresses") String addresses,
                               @RequestParam(value = "offset", defaultValue = "0") int offset,
                               Model model) throws IOException {

        java.util.List<String> list = java.util.Arrays.stream(addresses.split("[,\\r?\\n]+"))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toList());

        java.util.List<com.freelkee.btcbalancechecker.model.Wallet> wallets = list.parallelStream().map(addr -> {
            try {
                return btcService.getWallet(currency, addr, offset);
            } catch (Exception e) {
                com.freelkee.btcbalancechecker.model.Wallet w = new com.freelkee.btcbalancechecker.model.Wallet();
                w.setAddress(addr);
                w.setAmount(0);
                return w;
            }
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("wallets", wallets);
        model.addAttribute("offset", offset);
        return "batchResult";
    }

    @Autowired
    private com.freelkee.btcbalancechecker.service.BatchService batchService;

    @PostMapping("/batch/upload")
    public String uploadBatchFile(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                  @RequestParam(value = "currency", required = false) String currency,
                                  @RequestParam(value = "offset", defaultValue = "0") int offset,
                                  Model model) throws IOException {
        com.freelkee.btcbalancechecker.model.BatchJob job = batchService.startBatchFromFile(file, currency, offset);
        model.addAttribute("jobId", job.getId());
        return "batchProgress";
    }

    @GetMapping("/batch/status/{jobId}")
    @ResponseBody
    public java.util.Map<String, Object> batchStatus(@PathVariable String jobId) {
        com.freelkee.btcbalancechecker.model.BatchJob job = batchService.getJob(jobId);
        if (job == null) return java.util.Collections.singletonMap("error", "not found");
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("processed", job.getProcessed().get());
        m.put("total", job.getAddresses().size());
        m.put("failed", job.getFailed().get());
        m.put("done", job.isDone());
        m.put("cancelled", job.isCancelled());
        return m;
    }

    @GetMapping("/batch/status/detail/{jobId}")
    @ResponseBody
    public java.util.Map<String, Object> batchStatusDetail(@PathVariable String jobId) {
        com.freelkee.btcbalancechecker.model.BatchJob job = batchService.getJob(jobId);
        if (job == null) return java.util.Collections.singletonMap("error", "not found");
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("statuses", job.getStatuses());
        m.put("results", job.getResultsMap().keySet());
        m.put("processed", job.getProcessed().get());
        m.put("total", job.getAddresses().size());
        m.put("done", job.isDone());
        return m;
    }

    @GetMapping("/batch/stream/{jobId}")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamBatch(@PathVariable String jobId) {
        return batchService.subscribe(jobId);
    }

    @PostMapping("/batch/cancel/{jobId}")
    @ResponseBody
    public java.util.Map<String,Object> cancelBatch(@PathVariable String jobId) {
        boolean ok = batchService.cancel(jobId);
        return java.util.Collections.singletonMap("cancelled", ok);
    }

    @GetMapping("/batch/result/{jobId}")
    public String batchResult(@PathVariable String jobId, Model model) {
        com.freelkee.btcbalancechecker.model.BatchJob job = batchService.getJob(jobId);
        if (job == null) return "batch";
        model.addAttribute("wallets", job.getResults());
        model.addAttribute("offset", job.getOffset());
        return "batchResult";
    }

}
