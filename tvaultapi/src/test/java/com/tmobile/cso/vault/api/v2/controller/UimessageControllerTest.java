package com.tmobile.cso.vault.api.v2.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.service.UimessageService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages = { "com.tmobile.cso.vault.api" })
@WebAppConfiguration
public class UimessageControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UimessageService uimessageService;
	@InjectMocks
	private UimessageController uimessageController; 

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		this.mockMvc = MockMvcBuilders.standaloneSetup(uimessageController).build();
	}

	@Test
	public void test_writeMessage() throws Exception {
		String inputJson = "{\"path\":\"metadata/message\",\"data\":{\"message1\":\"value1\",\"message2\":\"value2\"}}";
		String responseMessage = "{\"messages\":[\"Message saved to vault\"]}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

		when(uimessageService.writeMessage(eq("s.2oefXc9A7iPbP9rGfGtKLUMf"), Mockito.any()))
				.thenReturn(responseEntityExpected);
		mockMvc.perform(
				MockMvcRequestBuilders.post("/v2/safes/message").header("vault-token", "s.2oefXc9A7iPbP9rGfGtKLUMf")
						.header("Content-Type", "application/json;charset=UTF-8").content(inputJson))
				.andExpect(status().isOk()).andExpect(content().string(containsString(responseMessage)));
	}

	@Test
	public void test_readmessage() throws Exception {
		String responseMessage = "{ \"data\": {\"message1\": \"value1\",\"message1\": \"value2\" }}";
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);
		when(uimessageService.readMessage()).thenReturn(responseEntityExpected);
		mockMvc.perform(MockMvcRequestBuilders.get("/v2/safes/message?path=metadata/message").header("Content-Type",
				"application/json;charset=UTF-8")).andExpect(status().isOk())
				.andExpect(content().string(containsString(responseMessage)));
	}

}
