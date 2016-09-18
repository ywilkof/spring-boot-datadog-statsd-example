package com.yonatanwilkof.example;

import com.codahale.metrics.MetricRegistry;
import org.coursera.metrics.datadog.model.DatadogGauge;
import org.coursera.metrics.datadog.transport.UdpTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.TomcatPublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Configuration
@ConditionalOnBean(value = UdpTransport.class)
@EnableScheduling
public class MetricExportConfiguration {

    private static final String COUNTER_STATUS_PREFIX = "counter.status.";
    private static final String GAUGE_RESPONSE = "gauge.response.";
    private static final String STATUS_CODE_TAG = "status-code:";
    private static final String ENDPOINT_TAG = "endpoint:";

    @Autowired
    private TomcatPublicMetrics tomcatPublicMetrics;

    @Autowired
    @Qualifier("dropwizardPublicMetrics")
    private MetricReaderPublicMetrics metricReaderPublicMetrics;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private UdpTransport transport;

    private static String HOSTNAME;

    static {
        try {
            final InetAddress address = InetAddress.getLocalHost();
            HOSTNAME = address.getHostName();
        } catch (UnknownHostException e) {
            HOSTNAME = "unknown";
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    void exportPublicMetrics() throws IOException {
        for (Metric<?> metric : metricReaderPublicMetrics.metrics()) {
            transport.prepare().addGauge(new DatadogGauge("endpoint.counter",
                    metric.getValue().longValue(),
                    metric.getTimestamp().getTime() / 1000,
                    HOSTNAME,
                    getEndpointCounterTags(metric.getName())));
            getEndpointTimingTag(metric.getName()).ifPresent(t-> {
                try {
                    transport.prepare()
                            .addGauge(new DatadogGauge("endpoint.timer",
                                    metric.getValue().longValue(),
                                    metric.getTimestamp().getTime() / 1000,
                                    HOSTNAME,
                                    Collections.singletonList(t)));
                } catch (IOException e) {
//                    log.error("failed sending endpoint.timer gauge");
                }
            });
        }
    }

    // e.g. counter.status.200.api.v1.pets
    private static List<String> getEndpointCounterTags(String metric) {
        if (metric.startsWith(COUNTER_STATUS_PREFIX)) {
            final String statusCode = metric.substring(15, 18);
            final String endpoint = metric.substring(19);
            return Arrays.asList(ENDPOINT_TAG + endpoint, STATUS_CODE_TAG + statusCode);
        } else {
            return Collections.emptyList();
        }
    }

    // e.g. gauge.response.api.v1.pets
    private static Optional<String> getEndpointTimingTag(String metric) {
        return Optional.of(metric).filter(m -> m.startsWith(GAUGE_RESPONSE))
                .map(m-> ENDPOINT_TAG + m.substring(15));
    }
}
