package com.example.demo;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface TestGateway {

	@Gateway(requestChannel = "out")
	void sendMessage(String message);
	
}
