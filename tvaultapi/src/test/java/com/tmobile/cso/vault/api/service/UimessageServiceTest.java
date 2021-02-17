package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.Message;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CommonUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = { "com.tmobile.cso.vault.api" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ ControllerUtil.class, JSONUtil.class })
@PowerMockIgnore({ "javax.management.*" })
public class UimessageServiceTest {
	@InjectMocks
	UimessageService uimessageService;
	@Mock
	RequestProcessor reqProcessor;
	@Mock
	CommonUtils commonUtils;
	@Mock
	TokenUtils tokenUtils;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(ControllerUtil.class);
		PowerMockito.mockStatic(JSONUtil.class);

		Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
		when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

		Map<String, String> currentMap = new HashMap<>();
		currentMap.put("apiurl", "http://localhost:8080/v2/safes/message");
		currentMap.put("user", "");
		ThreadLocalContext.setCurrentMap(currentMap);
	}

	Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
		Response response = new Response();
		response.setHttpstatus(status);
		response.setSuccess(success);
		if (expectedBody != "") {
			response.setResponse(expectedBody);
		}
		return response;
	}

	@Test
	public void test_keyexistingcheck() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String path = "metadata/message";
		String responseJson = "{\"data\":{\"message1\":\"value1\",\"message2\":\"value2\"}}";

		String writeJson = "{\"path\":\"metadata/message\",\"data\":{\"message1\":\"value1\",\"message2\":\"value2\"}}";

		HashMap<String, String> data = new HashMap<>();
		data.put("message1", "value1");
		data.put("message2", "value2");
		Message message = new Message(data);
		when(JSONUtil.getJSON(message)).thenReturn(writeJson);
		when(commonUtils.isAuthorizedToken(token)).thenReturn(true);
		when(ControllerUtil.isFolderExisting(path, token)).thenReturn(true);

		Response response = getMockResponse(HttpStatus.OK, true, responseJson);
		Response response1 = getMockResponse(HttpStatus.OK, true, "");

		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"message saved to vault\"]}");

		when(reqProcessor.process(Mockito.eq("/read"), Mockito.eq("{\"path\":\"" + path + "\"}"), Mockito.eq(token)))
				.thenReturn(response);
		when(reqProcessor.process(Mockito.eq("/write"), Mockito.eq(writeJson), Mockito.eq(token)))
				.thenReturn(response1);

		ResponseEntity<String> responseEntity = uimessageService.writeMessage(token, message);

		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		assertEquals(responseEntityExpected, responseEntity);

	}

	@Test
	public void test_isFolderExistingInvalid() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String writeJson = "{\"path\":\"metadata/message\",\"data1\":{\"message1\":\"value1\",\"message2\":\"value2\"}}";
		String path = "metadata/folder";

		HashMap<String, String> data = new HashMap<>();
		data.put("message1", "value1");
		data.put("message2", "value2");
		Message message = new Message(data);

		when(commonUtils.isAuthorizedToken(token)).thenReturn(true);
		Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
		Response response = getMockResponse(HttpStatus.OK, true, "{\"messages\":[\"message saved to vault\"]}");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
				.body("{\"messages\":[\"message saved to vault\"]}");

		when(JSONUtil.getJSON(message)).thenReturn(writeJson);

		when(ControllerUtil.isFolderExisting(path, token)).thenReturn(false);

		when(reqProcessor.process(Mockito.eq("/sdb/createfolder"), Mockito.anyString(), Mockito.eq(token)))
				.thenReturn(responseNoContent);

		when(reqProcessor.process(Mockito.eq("/write"), Mockito.anyString(), Mockito.eq(token))).thenReturn(response);
		ResponseEntity<String> responseEntity = uimessageService.writeMessage(token, message);
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void isAuthorizedTokenFailed() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String writeJson = "{\"path\":\"metadata/message\",\"data\":{\"message1\":\"value1\",\"message2\":\"value2\"}}";
		HashMap<String, String> data = new HashMap<>();
		data.put("message1", "value1");
		data.put("message2", "value2");
		Message message = new Message(data);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("{\"errors\":[\"Access Denied: No enough permission to access this API\"]}");
		when(JSONUtil.getJSON(message)).thenReturn(writeJson);
		when(commonUtils.isAuthorizedToken(token)).thenReturn(false);
		ResponseEntity<String> responseEntity = uimessageService.writeMessage(token, message);
		assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);

	}

	@Test
	public void test_isdataNullorEmptyFailed() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		HashMap<String, String> data = new HashMap<>();
		data.put("message1", null);
		data.put("message2", "value2");
		Message message = new Message(data);
		when(commonUtils.isAuthorizedToken(token)).thenReturn(true);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Invalid input values\"]}");
		ResponseEntity<String> responseEntity = uimessageService.writeMessage(token, message);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);

	}

	@Test
	public void test_datacheck() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		HashMap<String, String> data = null;
		Message message = new Message(data);
		when(commonUtils.isAuthorizedToken(token)).thenReturn(true);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Invalid request.Check json data\"]}");
		ResponseEntity<String> responseEntity = uimessageService.writeMessage(token, message);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void test_CreateFolderFailed() {

		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String writeJson = "{\"path\":\"metadata/message\",\"data1\":{\"message1\":\"value1\",\"message2\":\"value2\"}}";
		String path = "metadata/folder";

		HashMap<String, String> data = new HashMap<>();
		data.put("message1", "value1");
		data.put("message2", "value2");
		Message message = new Message(data);

		when(commonUtils.isAuthorizedToken(token)).thenReturn(true);
		Response responseNoContent = getMockResponse(HttpStatus.BAD_REQUEST, false, "");
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("{\"errors\":[\"Invalid path\"]}");
		when(JSONUtil.getJSON(message)).thenReturn(writeJson);

		when(ControllerUtil.isFolderExisting(path, token)).thenReturn(false);

		when(reqProcessor.process(Mockito.eq("/sdb/createfolder"), Mockito.anyString(), Mockito.eq(token)))
				.thenReturn(responseNoContent);

		ResponseEntity<String> responseEntity = uimessageService.writeMessage(token, message);
		assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

	@Test
	public void test_readMessage_successfully() {
		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
		String metadataJson = "data\":{\"message1\":\"value1\",\"message2\":\"value2\"}";
		String path = "{\"path\":\"metadata/message\"}";

		Response response = getMockResponse(HttpStatus.OK, true, metadataJson);
		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(metadataJson);

		when(tokenUtils.getSelfServiceToken()).thenReturn(token);
		when(reqProcessor.process(Mockito.eq("/sdb"), Mockito.eq(path), Mockito.eq(token))).thenReturn(response);
		ResponseEntity<String> responseEntity = uimessageService.readMessage();
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertEquals(responseEntityExpected, responseEntity);
	}

}
