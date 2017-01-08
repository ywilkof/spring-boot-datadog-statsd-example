package com.yonatanwilkof.example;

import com.codahale.metrics.Gauge;
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
    void exportPublicMetricsAndTransport() throws IOException {
        for (Metric<?> metric : metricReaderPublicMetrics.metrics()) {
            if (metric.getName().startsWith("counter.status.")) {
                final String statusCode = metric.getName().substring(15, 18);
                final String endpoint = metric.getName().substring(19);
                final List<String> tags = Arrays.asList("endpoint:" + endpoint, "status-code:" + statusCode);
                transport.prepare().addGauge(new DatadogGauge("endpoint.counter",
                        metric.getValue().longValue(),
                        metric.getTimestamp().getTime() / 1000,
                        HOSTNAME,
                        tags));
            }
            else if (metric.getName().startsWith("gauge.response.")) {
                transport.prepare()
                        .addGauge(new DatadogGauge("endpoint.timer",
                                metric.getValue().longValue(),
                                metric.getTimestamp().getTime() / 1000,
                                HOSTNAME,
                                Collections.singletonList(metric.getName().substring(15))));
            }
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    void exportPublicMetricsAndRegister() throws IOException {
        for (Metric<?> metric : metricReaderPublicMetrics.metrics()) {

            final Gauge<Long> gauge = () -> metric.getValue().longValue();
            if (metric.getName().startsWith("counter.status.")) {

                final String statusCode = metric.getName().substring(15, 18);
                final String endpoint = metric.getName().substring(19);

                final String countMetricWithTags = "endpoint.counter[" +
                        String.join(",", "endpoint:" + endpoint, "status-code:" + statusCode)
                        + "]";
                metricRegistry.remove(countMetricWithTags); // to avoid IllegalArgumentException
                metricRegistry.register(countMetricWithTags, gauge);

            } else if (metric.getName().startsWith("gauge.response.")) {
                final String timerMetricWithTag = "endpoint.timer[endpoint:" + metric.getName().substring(15) + "]";
                metricRegistry.remove(timerMetricWithTag);
                metricRegistry.register(timerMetricWithTag, gauge);
            }
        }
    }
    
}
