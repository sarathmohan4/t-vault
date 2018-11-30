package com.tmobile.cso.vault.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.AppRole;
import com.tmobile.cso.vault.api.model.AppRoleIdSecretId;
import com.tmobile.cso.vault.api.model.AppRoleNameSecretId;
import com.tmobile.cso.vault.api.model.AppRoleSecretData;
import com.tmobile.cso.vault.api.model.SafeAppRoleAccess;
import com.tmobile.cso.vault.api.model.SecretData;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore( {"javax.management.*"})
public class AppRoleServiceTest {

    @InjectMocks
    AppRoleService appRoleService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException{
    	PowerMockito.mockStatic(ControllerUtil.class);
    	PowerMockito.mockStatic(JSONUtil.class);
    	
    	Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(Mockito.any(ImmutableMap.class))).thenReturn("log");
 	
        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/vault/v2/sdb");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);

    }

    private Response getMockResponse(HttpStatus status, boolean success, String expectedBody) {
        Response response = new Response();
        response.setHttpstatus(status);
        response.setSuccess(success);
        if (expectedBody!="") {
            response.setResponse(expectedBody);
        }
        return response;
    }

    @Test
    public void test_createAppRole_successfully() {

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseList = getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    \"myvaultapprole\"  ]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, "1", "100m", 0);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created succssfully\"]}");
        
        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);

        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_createAppRole_InvalidAppRoleInputs() {

        Response response =getMockResponse(HttpStatus.BAD_REQUEST, true, "");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("", policies, true, "1", "100m", 0);
        String jsonStr = "{\"role_name\":\"\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values for AppRole creation\"]}");
        
        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(false);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_createAppRole_Failure_404() {

        String responseBody = "{\"errors\":[\"Invalid input values for AppRole creation\"]}";
        Response response =getMockResponse(HttpStatus.NOT_FOUND, true, responseBody);
        Response responseList = getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    \"myvaultapprole\"  ]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String [] policies = {"default"};
        AppRole appRole = new AppRole("", policies, true, "1", "100m", 0);
        String jsonStr = "{\"role_name\":\"\",\"policies\":[\"default\"],\"bind_secret_id\":true,\"secret_id_num_uses\":\"1\",\"secret_id_ttl\":\"100m\",\"token_num_uses\":0,\"token_ttl\":null,\"token_max_ttl\":null}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseBody);
        
        when(reqProcessor.process("/auth/approle/role/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.areAppRoleInputsValid(appRole)).thenReturn(true);
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(ControllerUtil.convertAppRoleInputsToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/list","{}",token)).thenReturn(responseList);
        ResponseEntity<String> responseEntityActual = appRoleService.createAppRole(token, appRole);
        
        assertEquals(HttpStatus.NOT_FOUND, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
    
    @Test
    public void test_readAppRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\":{ \"bind_secret_id\": true, \"policies\": [\"test-access-policy\"]}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\":{ \"bind_secret_id\": true, \"policies\": [\"test-access-policy\"]}}");
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRole = "approle1";
        
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+appRole+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRole(token, appRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    @Test
    public void test_readAppRoleId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\":{ \"role_id\": \"f1f72163-287e-b3a4-1fdc-fd21a35c7d57\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\":{ \"role_id\": \"f1f72163-287e-b3a4-1fdc-fd21a35c7d57\"}}");
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRoleName = "approle1";
        
        when(reqProcessor.process("/auth/approle/role/readRoleID","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.readAppRoleRoleId(token, appRoleName);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_createSecretId_successfully() {
        String responseJson = "{\"data\":{ \"secret_id\": \"5973a6de-38c1-0402-46a3-6d76e38b773c\", \"secret_id_accessor\": \"cda12712-9845-c271-aeb1-833681fac295\"}}";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Secret ID created for AppRole\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AppRoleSecretData appRoleSecretData = new AppRoleSecretData("approle1", new SecretData("dev", "appl"));

        String jsonStr = "{\"role_name\":\"approle1\",\"data\":{\"env\":\"dev\",\"appname\":\"appl\"}}";
        
        when(reqProcessor.process("/auth/approle/secretid/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.convertAppRoleSecretIdToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(JSONUtil.getJSON(appRoleSecretData)).thenReturn(jsonStr);
        
        ResponseEntity<String> responseEntityActual = appRoleService.createsecretId(token, appRoleSecretData);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_createSecretId_failure_500() {
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"messages\":[\"Internal server error\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Internal server error\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        AppRoleSecretData appRoleSecretData = new AppRoleSecretData("approle1", new SecretData("dev", "appl"));

        String jsonStr = "{\"role_name\":\"approle1\",\"data\":{\"env\":\"dev\",\"appname\":\"appl\"}}";

        when(reqProcessor.process("/auth/approle/secretid/create", jsonStr,token)).thenReturn(response);
        when(ControllerUtil.convertAppRoleSecretIdToLowerCase(Mockito.any())).thenReturn(jsonStr);
        when(JSONUtil.getJSON(appRoleSecretData)).thenReturn(jsonStr);

        ResponseEntity<String> responseEntityActual = appRoleService.createsecretId(token, appRoleSecretData);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_readSecretId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\":{ \"secret_id\": \"5973a6de-38c1-0402-46a3-6d76e38b773c\", \"secret_id_accessor\": \"cda12712-9845-c271-aeb1-833681fac295\"}}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        String appRoleName = "approle1";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        
        when(reqProcessor.process("/auth/approle/secretid/lookup","{\"role_name\":\""+appRoleName+"\"}",token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.readSecretId(token, appRoleName);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteAppRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole deleted\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";
        
        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);
        
        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_deleteAppRole_failure_500() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"messages\":[\"Internal server error\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Internal server error\"]}");
        AppRole appRole = new AppRole();
        appRole.setRole_name(appRoleId);
        String jsonStr = "{\"role_name\":\"approle1\",\"policies\":null,\"bind_secret_id\":false,\"secret_id_num_uses\":null,\"secret_id_ttl\":null,\"token_num_uses\":null,\"token_ttl\":null,\"token_max_ttl\":null}";

        when(JSONUtil.getJSON(appRole)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/role/delete",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntityActual = appRoleService.deleteAppRole(token, appRole);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_deleteSecretId_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        AppRoleNameSecretId appRoleNameSecretId = new AppRoleNameSecretId(appRoleId, "5973a6de-38c1-0402-46a3-6d76e38b773c");
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String jsonStr = "{\"role_name\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SecretId for AppRole deleted\"]}");
        
        when(JSONUtil.getJSON(appRoleNameSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/secret/delete",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteSecretId(token, appRoleNameSecretId);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_deleteSecretId_failure_500() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String appRoleId = "approle1";
        AppRoleNameSecretId appRoleNameSecretId = new AppRoleNameSecretId(appRoleId, "5973a6de-38c1-0402-46a3-6d76e38b773c");
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"messages\":[\"Internal server error\"]}");
        String jsonStr = "{\"role_name\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Internal server error\"]}");

        when(JSONUtil.getJSON(appRoleNameSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/secret/delete",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntityActual = appRoleService.deleteSecretId(token, appRoleNameSecretId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_AssociateAppRole_succssfully() throws Exception {

        Response response = getMockResponse(HttpStatus.OK, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle associated to SDB\"]}");
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("approle1", "shared/mysafe01", "write");
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";
        Map<String, Object> requestMap = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        when(ControllerUtil.parseJson(Mockito.anyString())).thenReturn(requestMap);
        when(reqProcessor.process(any(String.class),any(String.class),any(String.class))).thenReturn(response);
        when(ControllerUtil.isValidSafePath(Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.isValidSafe(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        when(ControllerUtil.areSafeAppRoleInputsValid(Mockito.anyMap())).thenReturn(true);
        when(ControllerUtil.canAddPermission(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  appRoleService.associateApprole(token, safeAppRoleAccess);
        
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void test_loginWithApprole_successfully() {
        String expectedLoginResponse = "{  \"auth\": {   \"renewable\": true,    \"lease_duration\": 2764800,    \"metadata\": {},    \"policies\": [      \"default\"    ],    \"accessor\": \"5d7fb475-07cb-4060-c2de-1ca3fcbf0c56\",    \"client_token\": \"98a4c7ab-b1fe-361b-ba0b-e307aacfd587\"  }}";
        Response response =getMockResponse(HttpStatus.OK, true, expectedLoginResponse);
        AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId("approle1", "5973a6de-38c1-0402-46a3-6d76e38b773c");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedLoginResponse);
        String jsonStr = "{\"role_id\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";
        
        when(JSONUtil.getJSON(appRoleIdSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntityActual = appRoleService.login(appRoleIdSecretId);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
        
    }

    @Test
    public void test_loginWithApprole_failure_500() {
        String expectedLoginResponse = "{\"messages\":[\"Internal server error\"]}";
        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, expectedLoginResponse);
        AppRoleIdSecretId appRoleIdSecretId = new AppRoleIdSecretId("approle1", "5973a6de-38c1-0402-46a3-6d76e38b773c");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle Login Failed.\"]}HTTP STATUSCODE  :500");
        String jsonStr = "{\"role_id\":\"approle1\",\"secret_id\":\"5973a6de-38c1-0402-46a3-6d76e38b773c\"}";

        when(JSONUtil.getJSON(appRoleIdSecretId)).thenReturn(jsonStr);
        when(reqProcessor.process("/auth/approle/login",jsonStr,"")).thenReturn(response);

        ResponseEntity<String> responseEntityActual = appRoleService.login(appRoleIdSecretId);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }
}
