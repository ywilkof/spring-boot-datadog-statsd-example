package com.yonatanwilkof.example;

import com.codahale.metrics.MetricRegistry;
import org.coursera.metrics.datadog.DatadogReporter;
import org.coursera.metrics.datadog.transport.UdpTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnBean(value = UdpTransport.class)
public class DatadogReporterConfiguration {

    @Autowired
    private MetricRegistry registry;


    @Autowired
    private UdpTransport udpTransport;


    @PostConstruct
    public void createReporter() {
        final DatadogReporter reporter = DatadogReporter.forRegistry(registry)
                .withTransport(udpTransport)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(60,TimeUnit.SECONDS);
    }
}
