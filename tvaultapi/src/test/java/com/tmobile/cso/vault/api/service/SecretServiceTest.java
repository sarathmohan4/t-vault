package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.SafeNode;
import com.tmobile.cso.vault.api.model.Secret;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import sun.nio.ch.Secrets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SecretServiceTest {

    @InjectMocks
    SecretService secretService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);

        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
    }

    Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody!="") {
            response.setResponse(expectedBody);
        }
        return response;
    }

    @Test
    public void test_write_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ReflectionTestUtils.setField(secretService, "secretLimit", "2");
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret saved to vault\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        Response recursiveResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bb\":\"bb\"}}");

        SafeNode childNode = new SafeNode();
        childNode.setId("users/s22/q/b");
        childNode.setParentId("users/s22/q/b");
        childNode.setType("secret");
        childNode.setValue("{\"data\":{\"bb\":\"bb\"}}");

        List<SafeNode> nodeList = new ArrayList<>();
        nodeList.add(childNode);

        SafeNode safeNode = new SafeNode();
        safeNode.setId("users/s22/q/b");
        safeNode.setParentId(null);
        safeNode.setType("folder");
        safeNode.setValue(null);
        safeNode.setChildren(nodeList);


        Map<Response, SafeNode> res = new HashMap<>();
        res.put(recursiveResponse, safeNode);
        when(ControllerUtil.getRecursiveReadResponse(Mockito.any(),eq(token),Mockito.any(), Mockito.any())).thenReturn(res);
        try {
            when(JSONUtil.getObj(Mockito.any(), eq(SafeNode.class))).thenReturn(safeNode);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ResponseEntity<String> responseEntity = secretService.write(token, secret);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_failure_limit_reached() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ReflectionTestUtils.setField(secretService, "secretLimit", "2");

        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        data.put("secret2", "value2");
        data.put("secret3", "value3");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"You have reached the limit of number of secrets that can be created under the safe shared/mysafe01\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        Response recursiveResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bb\":\"bb\"}}");

        SafeNode childNode = new SafeNode();
        childNode.setId("users/s22/q/b");
        childNode.setParentId("users/s22/q/b");
        childNode.setType("secret");
        childNode.setValue("{\"data\":{\"bb\":\"bb\", \"cc\":\"cc\"}}");

        List<SafeNode> nodeList = new ArrayList<>();
        nodeList.add(childNode);

        SafeNode safeNode = new SafeNode();
        safeNode.setId("users/s22/q/b");
        safeNode.setParentId(null);
        safeNode.setType("folder");
        safeNode.setValue(null);
        safeNode.setChildren(nodeList);


        Map<Response, SafeNode> res = new HashMap<>();
        res.put(recursiveResponse, safeNode);
        when(ControllerUtil.getRecursiveReadResponse(Mockito.any(),eq(token),Mockito.any(), Mockito.any())).thenReturn(res);
        try {
            when(JSONUtil.getObj(Mockito.any(), eq(SafeNode.class))).thenReturn(safeNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.getSafePath(Mockito.any())).thenReturn("shared/mysafe01");

        ResponseEntity<String> responseEntity = secretService.write(token, secret);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_failure_invalidPath() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(false);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = secretService.write(token, secret);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_write_failure_500() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ReflectionTestUtils.setField(secretService, "secretLimit", "2");
        String jsonStr = "{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}";
        HashMap<String, String> data = new HashMap<>();
        data.put("secret1", "value1");
        Secret secret = new Secret("shared/mysafe01/myfolder", data);
        Response response = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Writing secret failed\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Writing secret failed\"]}");

        when(ControllerUtil.addDefaultSecretKey(jsonStr)).thenReturn("{\"path\":\"shared/mysafe01/myfolder\",\"data\":{\"secret1\":\"value1\",\"secret2\":\"value2\"}}");
        when(ControllerUtil.areSecretKeysValid(jsonStr)).thenReturn(true);
        when(ControllerUtil.isPathValid("shared/mysafe01/myfolder")).thenReturn(true);
        when(reqProcessor.process("/write",jsonStr,token)).thenReturn(response);
        when(JSONUtil.getJSON(secret)).thenReturn(jsonStr);

        Response recursiveResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bb\":\"bb\"}}");

        SafeNode childNode = new SafeNode();
        childNode.setId("users/s22/q/b");
        childNode.setParentId("users/s22/q/b");
        childNode.setType("secret");
        childNode.setValue("{\"data\":{\"bb\":\"bb\"}}");

        List<SafeNode> nodeList = new ArrayList<>();
        nodeList.add(childNode);

        SafeNode safeNode = new SafeNode();
        safeNode.setId("users/s22/q/b");
        safeNode.setParentId(null);
        safeNode.setType("folder");
        safeNode.setValue(null);
        safeNode.setChildren(nodeList);


        Map<Response, SafeNode> res = new HashMap<>();
        res.put(recursiveResponse, safeNode);
        when(ControllerUtil.getRecursiveReadResponse(Mockito.any(),eq(token),Mockito.any(), Mockito.any())).thenReturn(res);
        try {
            when(JSONUtil.getObj(Mockito.any(), eq(SafeNode.class))).thenReturn(safeNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ResponseEntity<String> responseEntity = secretService.write(token, secret);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_readFromVault_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responsejson = "{  \"data\": {    \"secret1\": \"value1\",    \"secret2\": \"value2\"  }}";

        Response response = getMockResponse(HttpStatus.OK, true, responsejson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responsejson);

        when(reqProcessor.process("/read","{\"path\":\"shared/mysafe01/myfolder\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = secretService.readFromVault(token, "shared/mysafe01/myfolder");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteFromVault_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01/myfolder";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secrets deleted\"]}");

        when(ControllerUtil.isValidDataPath(path)).thenReturn(true);
        when(reqProcessor.process("/delete","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = secretService.deleteFromVault(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteFromVault_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01/myfolder";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");

        when(ControllerUtil.isValidDataPath(path)).thenReturn(false);

        ResponseEntity<String> responseEntity = secretService.deleteFromVault(token, path);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    /*@Test
    public void test_readFromVaultRecursive() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01/myfolder";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");

        Response response = new Response();
        SafeNode safeNode = new SafeNode();
        safeNode.setType("safe");
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        ResponseEntity<String> responseEntity = secretService.readFromVaultRecursive(token, path);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);

    }*/

}
