package com.meetingsum;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.meetingsum.config.AppProperties;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AppProperties.class)
public class MeetingSumApplication implements CommandLineRunner {

    private final AppProperties props;

    public MeetingSumApplication(AppProperties props) {
        this.props = props;
    }

    public static void main(String[] args) {
        SpringApplication.run(MeetingSumApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Files.createDirectories(Path.of(props.getUploadDir()));
        Files.createDirectories(Path.of(props.getOutputDir()));
    }
}
