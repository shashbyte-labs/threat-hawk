package com.threathawk.controller;

import com.threathawk.config.AppConstants;
import com.threathawk.model.Alert;
import com.threathawk.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConstants.PATH_API)
public class AlertController {

    private static final Logger log = LoggerFactory.getLogger(AlertController.class);

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @PostMapping(AppConstants.PATH_EVENTS)
    public void postEvent(@RequestBody String raw) {
        log.info("Received event payload of length={}", raw != null ? raw.length() : 0);
    }

    @GetMapping(AppConstants.PATH_ALERTS)
    public Page<Alert> getAlerts(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return alertRepository.findAll(PageRequest.of(page, size));
    }

    @GetMapping(AppConstants.PATH_ALERTS_ALL)
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }
}