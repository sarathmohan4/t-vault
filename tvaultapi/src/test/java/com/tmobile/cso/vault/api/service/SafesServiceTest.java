package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.*;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SafesServiceTest {

    @InjectMocks
    SafesService safesService;

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
    public void test_getFolders_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{  \"keys\": [    \"mysafe01\"  ]}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/sdb/list","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.getFolders(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(reqProcessor.process("/sdb","{\"path\":\"metadata/"+path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.getInfo(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createfolder_successfully() {

        String responseJson = "{  \"messages\": [    \"Folder created \"  ]}";
        String path = "shared/mysafe01";
        String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(ControllerUtil.isPathValid(Mockito.any())).thenReturn(true);
        when(reqProcessor.process("/sdb/createfolder",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.createfolder(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createfolder_successfully_noContent() {

        String responseJson = "{\"messages\":[\"Folder created \"]}";
        String path = "shared/mysafe01";
        String jsonStr ="{\"path\":\""+path +"\",\"data\":{\"default\":\"default\"}}";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        Response response = getMockResponse(HttpStatus.NO_CONTENT, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(ControllerUtil.isPathValid(Mockito.any())).thenReturn(true);
        when(reqProcessor.process("/sdb/createfolder",jsonStr,token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.createfolder(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createfolder_failure_400() {

        String responseJson = "{\"errors\":[\"Invalid path\"]}";
        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseJson);

        when(ControllerUtil.isPathValid(Mockito.any())).thenReturn(false);
        ResponseEntity<String> responseEntity = safesService.createfolder(token, path);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        String metadatajson = "{\"path\":\"metadata/shared/mysafe03\",\"data\":{\"name\":\"mysafe03\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(JSONUtil.getJSON(safe)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(true);
        when(reqProcessor.process("/sdb/create",jsonStr,token)).thenReturn(responseNoContent);
        when(ControllerUtil.convetToJson(Mockito.any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write",metadatajson,token)).thenReturn(responseNoContent);
        when(reqProcessor.process(eq("/access/update"),Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_failure_policies_creation() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        String metadatajson = "{\"path\":\"metadata/shared/mysafe03\",\"data\":{\"name\":\"mysafe03\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseBadRequest = getMockResponse(HttpStatus.BAD_REQUEST, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.MULTI_STATUS).body("{\"messages\":[\"Safe created however one ore more policy (read/write/deny) creation failed \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(JSONUtil.getJSON(safe)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(true);
        when(reqProcessor.process("/sdb/create",jsonStr,token)).thenReturn(responseNoContent);
        when(ControllerUtil.convetToJson(Mockito.any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write",metadatajson,token)).thenReturn(responseNoContent);
        when(reqProcessor.process(eq("/access/update"),Mockito.any(),eq(token))).thenReturn(responseBadRequest);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.MULTI_STATUS, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_failure_500() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        String jsonStr = "{ \"data\": {\"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\"}, \"path\": \"shared/mysafe01\"}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Server down\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }
        Response response500 = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"errors\":[\"Server down\"]}");
        when(ControllerUtil.converSDBInputsToLowerCase(JSONUtil.getJSON(safe))).thenReturn(jsonStr);
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(reqparams);

        when(ControllerUtil.areSDBInputsValid(safe)).thenReturn(true);
        when(JSONUtil.getJSON(safe)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(true);
        when(reqProcessor.process("/sdb/create",jsonStr,token)).thenReturn(response500);



        ResponseEntity<String> responseEntity = safesService.createSafe(token, safe);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_successfully() {

        String path = "shared/mysafe01";
        String _path = "metadata/"+path;
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
        Response response = getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(reqProcessor.process("/sdb/list","{\"path\":\""+path+"\"}",token)).thenReturn(response);
        when(reqProcessor.process("/sdb","{\"path\":\""+_path+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = safesService.getSafe(token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteSafe_failed_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe(path,safeBasicDetails);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(false);
        when(ControllerUtil.isValidDataPath(path)).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.deleteSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        String metadatajson = "{\"path\":\"metadata/shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\",\"aws-roles\":null,\"groups\":null,\"users\":null}}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response readResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"description\":\"My first safe\",\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\"}}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getCountOfSafesForGivenSafeName(safe.getSafeBasicDetails().getName(), token)).thenReturn(1);
        when(ControllerUtil.generateSafePath("mysafe01", "shared")).thenReturn("shared/mysafe01");
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(true);

        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);
        when(ControllerUtil.convetToJson(Mockito.any())).thenReturn(metadatajson);
        when(reqProcessor.process("/sdb/update",metadatajson,token)).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getCountOfSafesForGivenSafeName(safe.getSafeBasicDetails().getName(), token)).thenReturn(1);
        when(ControllerUtil.generateSafePath("mysafe01", "shared")).thenReturn("shared/mysafe01");
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_failure_404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        Response readResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error Fetching existing safe info. please check the path specified \"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValidForUpdate(reqparams)).thenReturn(true);
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getCountOfSafesForGivenSafeName(safe.getSafeBasicDetails().getName(), token)).thenReturn(1);
        when(ControllerUtil.generateSafePath("mysafe01", "shared")).thenReturn("shared/mysafe01");
        when(ControllerUtil.isValidSafePath(Mockito.any())).thenReturn(true);

        when(reqProcessor.process("/read","{\"path\":\"metadata/shared/mysafe01\"}",token)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_failure_invalidInput() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        String jsonStr = "{\"path\":\"shared/mysafe01\",\"data\":{\"name\":\"mysafe01\",\"owner\":\"youremail@yourcompany.com\",\"type\":\"\",\"description\":\"My first safe\"}}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        Map<String,Object> reqparams = null;
        try {
            reqparams = new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.parseJson(Mockito.any())).thenReturn(reqparams);
        when(ControllerUtil.areSDBInputsValid(reqparams)).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.updateSafe(token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), Mockito.any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.configureLDAPUser(eq("testuser1"),Mockito.any(),Mockito.any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");

        when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);

        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), Mockito.any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testuser1"),Mockito.any(),Mockito.any(),eq(token))).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        when(ControllerUtil.areSafeUserInputsValid(safeUser)).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.addUserToSafe(token, safeUser);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), Mockito.any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testuser1"),Mockito.any(),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(userResponse);
        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), Mockito.any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPUser(eq("testuser1"),Mockito.any(),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_failure_orphan_entries() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed. Please try again\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser1\"}",token)).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(ControllerUtil.getAllExistingSafeNames("shared", token)).thenReturn(null);
        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        String jsonStr = "{  \"path\": \"shared/mysafe01\",  \"username\": \"testuser1\",  \"access\": \"write\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        when(JSONUtil.getJSON(safeUser)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.removeUserFromSafe(token, safeUser);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");


        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(userResponse);
        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), Mockito.any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_shared_mysafe01\",\"w_shared_mysafe02\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"Group configuration failed.Please try again\"]}");


        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(userResponse);
        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(Mockito.any(), Mockito.any())).thenReturn("default,w_shared_mysafe01,w_shared_mysafe02");
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.configureLDAPGroup(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully_orphan_entries() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");
        String jsonstr = "{  \"path\": \"shared/mysafe01\",  \"groupname\": \"mygroup01\",  \"access\": \"read\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"Group association is removed \"}");


        when(JSONUtil.getJSON(safeGroup)).thenReturn(jsonstr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"mygroup01\"}",token)).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.removeGroupFromSafe(token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.areAWSRoleInputsValid(Mockito.any(Map.class))).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(readResponse);
        when(ControllerUtil.configureAWSIAMRole(eq("iam"),Mockito.any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try Again\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.areAWSRoleInputsValid(Mockito.any(Map.class))).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(readResponse);
        when(ControllerUtil.configureAWSIAMRole(eq("iam"),Mockito.any(),eq(token))).thenReturn(responseNotFound);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.areAWSRoleInputsValid(Mockito.any(Map.class))).thenReturn(false);
        //when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure_404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        String responseBody = "{ \"bound_account_id\": [ \"1234567890123\"],\"bound_ami_id\": [\"ami-fce3c696\" ], \"bound_iam_instance_profile_arn\": [\n" +
                "  \"arn:aws:iam::877677878:instance-profile/exampleinstanceprofile\" ], \"bound_iam_role_arn\": [\"arn:aws:iam::8987887:role/test-role\" ], " +
                "\"bound_vpc_id\": [    \"vpc-2f09a348\"], \"bound_subnet_id\": [ \"subnet-1122aabb\"],\"bound_region\": [\"us-east-2\"],\"policies\":" +
                " [ \"\\\"[prod\",\"dev\\\"]\" ], \"auth_type\":\"iam\"}";
        Response readResponse = getMockResponse(HttpStatus.OK, true, responseBody);
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(ControllerUtil.areAWSRoleInputsValid(Mockito.any(Map.class))).thenReturn(true);
        when(ControllerUtil.canAddPermission(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles","{\"role\":\"iam\"}",token)).thenReturn(readResponse);
        when(ControllerUtil.configureAWSIAMRole(eq("iam"),Mockito.any(),eq(token))).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNotFound);
        when(ControllerUtil.configureAWSRole(Mockito.any(),Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.addAwsRoleToSafe(token, awsRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");


        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles/delete","{\"role\":\"iam\"}",token)).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Please try again\"]}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, true, "");


        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles/delete","{\"role\":\"iam\"}",token)).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(Mockito.any(),eq(token))).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_404() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Role configuration failed.Try Again\"]}");
        Response responseBadRequest = getMockResponse(HttpStatus.BAD_REQUEST, true, "{  \"errors\": [   \"Invalid 'path' specified\"  ]}");


        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(true);
        when(ControllerUtil.isValidSafe(path, token)).thenReturn(true);
        when(reqProcessor.process("/auth/aws/roles/delete","{\"role\":\"iam\"}",token)).thenReturn(responseBadRequest);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_invalidPath() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        AWSRole awsRole = new AWSRole(path,"iam","read");
        String jsonStr = "{  \"access\": \"read\",  \"path\": \"shared/mysafe01\",  \"role\": \"iam\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid 'path' specified\"]}");

        when(JSONUtil.getJSON(awsRole)).thenReturn(jsonStr);
        when(ControllerUtil.isValidSafePath(path)).thenReturn(false);

        ResponseEntity<String> responseEntity = safesService.removeAWSRoleFromSafe(token, awsRole, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}
