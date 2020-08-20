package com.example.demo;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;

public class PrometheusPushGatewayManagerFix extends PrometheusPushGatewayManager {

	PrometheusPushGatewayManagerFix(PushGateway pushGateway, CollectorRegistry registry, Duration pushRate,
			String job, Map<String, String> groupingKeys, ShutdownOperation shutdownOperation) {
		super(pushGateway, registry, new PushGatewayTaskScheduler(), pushRate, job, groupingKeys, shutdownOperation);
	}

	@EventListener
	public void shutdown(ContextClosedEvent event) {
		shutdown();
	}

	/**
	 * {@link TaskScheduler} used when the user doesn't specify one.
	 */
	static class PushGatewayTaskScheduler extends ThreadPoolTaskScheduler {

		private static final long serialVersionUID = 1L;

		PushGatewayTaskScheduler() {
			setPoolSize(1);
			setDaemon(true);
			setThreadGroupName("prometheus-push-gateway");
		}

		@Override
		public ScheduledExecutorService getScheduledExecutor() {
			return Executors.newSingleThreadScheduledExecutor(this::newThread);
		}

	}

}
