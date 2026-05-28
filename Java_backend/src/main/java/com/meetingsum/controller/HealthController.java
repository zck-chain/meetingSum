package com.meetingsum.controller;

import com.meetingsum.config.AppProperties;
import com.meetingsum.model.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final AppProperties props;

    public HealthController(AppProperties props) {
        this.props = props;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", props.getAppName(), props.getAppVersion());
    }
}
