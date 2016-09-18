package com.yonatanwilkof.example;

import org.coursera.metrics.datadog.transport.UdpTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "monitoring.metrics.export.statsd",name = {"host","port"})
public class UdpTransportConfiguration {

    @Configuration
    @ConfigurationProperties(prefix = "monitoring.metrics.export.statsd")
    public static class StatsDConfiguration {
        private String host;
        private Integer port;
        private String prefix;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    @Bean
    public UdpTransport udpTransport(StatsDConfiguration configuration) {
        return new UdpTransport.Builder()
                .withPort(configuration.getPort())
                .withPrefix(configuration.getPrefix())
                .withStatsdHost(configuration.getHost())
                .build();
    }


}
