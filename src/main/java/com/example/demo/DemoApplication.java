package com.example.demo;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
@IntegrationComponentScan
@EnableIntegrationManagement
@EnableTask
public class DemoApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
	    return new RabbitAdmin(connectionFactory);
	}
	
	@Bean(name="out")
	public MessageChannel channelOut() {
		return MessageChannels.direct().get();
	}
	@Bean(name="in")
	public MessageChannel channelIn() {
		return MessageChannels.direct().get();
	}
	
	@Bean
	public Queue testQueue() {
		return QueueBuilder
			.durable("test.q")
			.build();
	}

	@Bean
	public IntegrationFlow flowTestOutToRabbit(RabbitTemplate rabbitTemplate) {
		return IntegrationFlows
				.from("out")
				.handle(Amqp.outboundAdapter(rabbitTemplate).routingKey("test.q"))
				.get();
	}
	@Bean
	public IntegrationFlow flowRabbitInToTest(ConnectionFactory factory) {

		LoggingHandler loggingHandler =  new LoggingHandler(LoggingHandler.Level.INFO.name());
		loggingHandler.setLoggerName("com.example.demo");

		return IntegrationFlows
				.from(
						Amqp.inboundAdapter(factory, "test.q")
						)
				.channel("in")
				.handle(loggingHandler)
				.get();
	}
	
	@Bean
	public CommandLineRunner commandLineRunner(TestGateway gateway) {
		return args -> {
			for(int index = 0; index < 100; index++) {
				gateway.sendMessage("TestyMcTestFace " + index);
			}
			//Just sleeping so the listener has a chance to drain the queue
			Thread.sleep(1000);
		};
	}	
	
}
