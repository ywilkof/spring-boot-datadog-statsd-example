package com.yonatanwilkof.example;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by yonatan on 9/24/16.
 */
public class MetricExportConfigurationTest {

    @Test
    public void testGetEndpointCounterTags() throws Exception {
        final List<String> output = MetricExportConfiguration.getEndpointCounterTags("counter.status.200.api.v1.pets");
        Assert.assertEquals(output.get(0),"endpoint:api.v1.pets");
        Assert.assertEquals(output.get(1),"status-code:200");
    }

    @Test
    public void testGetEndpointTimingTag() throws Exception {
        final Optional<String> output = MetricExportConfiguration.getEndpointTimingTag("gauge.response.api.v1.pets");
        Assert.assertEquals(output,Optional.of("endpoint:api.v1.pets"));
    }
}