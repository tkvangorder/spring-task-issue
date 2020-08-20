package com.example.demo;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusProperties;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager.ShutdownOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

//This just creates configuration creates a slightly modified version of the
//PrometheusPushGatewayManager and changes the shutdown to use the ContextClosedEvent.
//This works because the autoconfiguration will backoff and use this version of the manager.

@Configuration
public class PrometheusPushFixConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(PrometheusPushFixConfiguration.class);

	/**
	 * The fallback job name. We use 'spring' since there's a history of Prometheus
	 * spring integration defaulting to that name from when Prometheus integration
	 * didn't exist in Spring itself.
	 */
	private static final String FALLBACK_JOB = "spring";
	
	//And this fixed the shutdown issue when we push metrics to prometheus by
	//A) disabling the destroy method on the manager
	//B) adding an event listener for context closed event and triggering shutdown BEFORE beans are destroyed.
	@Bean(destroyMethod="")
	@ConditionalOnProperty(prefix = "push-gateway-fix", name = "enabled", havingValue = "true")
	public PrometheusPushGatewayManager prometheusPushGatewayManager(
			CollectorRegistry collectorRegistry,
			PrometheusProperties prometheusProperties,
			Environment environment,
			LoggingSystem loggingSystem) {

		PrometheusProperties.Pushgateway properties = prometheusProperties.getPushgateway();
		Duration pushRate = properties.getPushRate();
		String job = getJob(properties, environment);
		Map<String, String> groupingKey = properties.getGroupingKey();
		ShutdownOperation shutdownOperation = properties.getShutdownOperation();
		return new PrometheusPushGatewayManagerFix(getPushGateway(properties.getBaseUrl()), collectorRegistry,
				pushRate, job, groupingKey, shutdownOperation);
	}

	
	private PushGateway getPushGateway(String url) {
		try {
			return new PushGateway(new URL(url));
		}
		catch (MalformedURLException ex) {
			logger.warn("Invalid PushGateway base url '{}': update your configuration to a valid URL", url);
			return new PushGateway(url);
		}
	}

	private String getJob(PrometheusProperties.Pushgateway properties, Environment environment) {
		String job = properties.getJob();
		job = (job != null) ? job : environment.getProperty("spring.application.name");
		return (job != null) ? job : FALLBACK_JOB;
	}	
}
