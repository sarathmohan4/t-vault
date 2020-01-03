// =========================================================================
// Copyright 2019 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// See the readme.txt file for additional language around disclaimer of warranties.
// =========================================================================
package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.model.DatabaseRole;
import com.tmobile.cso.vault.api.model.DatabaseStaticRole;
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
import org.mockito.cglib.core.CollectionUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({CollectionUtils.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class DatabaseSecretServiceTest {

    @InjectMocks
    DatabaseSecretService databaseSecretService;

    @Mock
    RequestProcessor reqProcessor;

    @Before
    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException{
        PowerMockito.mockStatic(CollectionUtils.class);
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
    public void test_getTemporaryCredentials_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole1";
        String responseJson = "{\n" +
                "  \"data\": {\n" +
                "    \"username\": \"root-1430158508-126\",\n" +
                "    \"password\": \"132ae3ef-5a64-7499-351e-bfe59f3a2a21\"\n" +
                "  }\n" +
                "}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/database/creds/","{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.getTemporaryCredentials(role_name, token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_successfully() {

        Response response =getMockResponse(HttpStatus.OK, true, "");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        DatabaseRole databaseRole = new DatabaseRole();
        databaseRole.setDb_name("testdb");
        databaseRole.setRole_name("testrole");
        String creationStmt[] = {"Create User '{{name}}'@'%' identified by '{{password}}'; Grant All on vault.* to '{{name}}'@'%';"};
        databaseRole.setCreation_statements(creationStmt);
        databaseRole.setDefault_ttl("1h");
        databaseRole.setMax_ttl("24h");

        String jsonStr = JSONUtil.getJSON(databaseRole);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Database role created successfully\"]}");
        when(reqProcessor.process("/database/roles/create/",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = databaseSecretService.createRole(token, databaseRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_failed() {

        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"error\":[\"failed to find entry for connection with name: \"testdb123\"]}");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        DatabaseRole databaseRole = new DatabaseRole();
        databaseRole.setDb_name("testdb123");
        databaseRole.setRole_name("testrole");
        String creationStmt[] = {"Create User '{{name}}'@'%' identified by '{{password}}'; Grant All on vault.* to '{{name}}'@'%';"};
        databaseRole.setCreation_statements(creationStmt);
        databaseRole.setDefault_ttl("1h");
        databaseRole.setMax_ttl("24h");

        String jsonStr = JSONUtil.getJSON(databaseRole);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":[\"failed to find entry for connection with name: \"testdb123\"]}");
        when(reqProcessor.process("/database/roles/create/",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = databaseSecretService.createRole(token, databaseRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createStaticRole_successfully() {

        Response response =getMockResponse(HttpStatus.OK, true, "");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        DatabaseStaticRole databaseRole = new DatabaseStaticRole();
        databaseRole.setDb_name("testdb");
        databaseRole.setName("testrole");
        String rotationStmt[] = {"ALTER USER '{{name}}' IDENTIFIED BY '{{password}}';"};
        databaseRole.setRotation_statements(rotationStmt);
        databaseRole.setUsername("testuser1");
        databaseRole.setRotation_period("1h");

        String jsonStr = JSONUtil.getJSON(databaseRole);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Static database role created successfully\"]}");
        when(reqProcessor.process("/database/static-roles/create/",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = databaseSecretService.createStaticRole(token, databaseRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createStaticRole_failed() {

        Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{\"error\":[\"failed to find entry for connection with name: \"testdb123\"]}");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        DatabaseStaticRole databaseRole = new DatabaseStaticRole();
        databaseRole.setDb_name("testdb");
        databaseRole.setName("testrole");
        String rotationStmt[] = {"ALTER USER '{{name}}' IDENTIFIED BY '{{password}}';"};
        databaseRole.setRotation_statements(rotationStmt);
        databaseRole.setUsername("testuser1");
        databaseRole.setRotation_period("1h");

        String jsonStr = JSONUtil.getJSON(databaseRole);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":[\"failed to find entry for connection with name: \"testdb123\"]}");
        when(reqProcessor.process("/database/static-roles/create/",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = databaseSecretService.createStaticRole(token, databaseRole);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getStaticCredentials_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole1";
        String responseJson = "{\n" +
                "  \"data\": {\n" +
                "    \"username\": \"static-user\",\n" +
                "    \"password\": \"132ae3ef-5a64-7499-351e-bfe59f3a2a21\"\n" +
                "    \"last_vault_rotation\": \"2019-05-06T15:26:42.525302-05:00\",\n" +
                "    \"rotation_period\": 30,\n" +
                "    \"ttl\": 28,\n" +
                "  }\n" +
                "}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/database/static-creds/","{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.getStaticCredentials(role_name, token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_readRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole1";
        String responseJson = "{\n" +
                "    \"data\": {\n" +
                "        \"creation_statements\": [\"CREATE ROLE \\\"{{name}}\\\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}';\", \"GRANT SELECT ON ALL TABLES IN SCHEMA public TO \\\"{{name}}\\\";\"],\n" +
                "        \"db_name\": \"mysql\",\n" +
                "        \"default_ttl\": 3600,\n" +
                "        \"max_ttl\": 86400,\n" +
                "        \"renew_statements\": [],\n" +
                "        \"revocation_statements\": [],\n" +
                "        \"rollback_statements\": []\n" +
                "    },\n" +
                "}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/database/roles/","{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.readRole(role_name, token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_listRoles_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\n" +
                "  \"auth\": null,\n" +
                "  \"data\": {\n" +
                "    \"keys\": [\"dev\", \"prod\"]\n" +
                "  },\n" +
                "  \"lease_duration\": 2764800,\n" +
                "  \"lease_id\": \"\",\n" +
                "  \"renewable\": false\n" +
                "}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/database/roles/list/","{}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.listRoles(token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

        when(reqProcessor.process("/database/roles/delete/","{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.deleteRole(role_name, token);

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    public void test_readStaticRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole1";
        String responseJson = "{\n" +
                "    \"data\": {\n" +
                "        \"db_name\": \"mysql\",\n" +
                "    \"username\":\"static-user\",\n" +
                "    \"rotation_statements\": [\"ALTER USER \"{{name}}\" WITH PASSWORD '{{password}}';\"],\n" +
                "    \"rotation_period\":\"1h\",\n" +
                "    },\n" +
                "}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/database/static-roles/","{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.readStaticRole(role_name, token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_listStaticRoles_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String responseJson = "{\n" +
                "  \"auth\": null,\n" +
                "  \"data\": {\n" +
                "    \"keys\": [\"dev-static\", \"prod-static\"]\n" +
                "  }\n" +
                "}";
        Response response =getMockResponse(HttpStatus.OK, true, responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(reqProcessor.process("/database/static-roles/list/","{}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.listStaticRoles(token);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteStaticRole_successfully() {

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole1";
        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

        when(reqProcessor.process("/database/static-roles/delete/","{\"role_name\":\""+role_name+"\"}",token)).thenReturn(response);
        ResponseEntity<String> responseEntity = databaseSecretService.deleteStaticRole(role_name, token);

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    public void test_rotateStaticRoleCredential_successfully() {

        Response response =getMockResponse(HttpStatus.NO_CONTENT, true, "");

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String role_name = "testrole";
        String jsonStr = "{\"name\":\""+role_name+"\"}";

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.NO_CONTENT).body("");
        when(reqProcessor.process("/database/rotate-role/",jsonStr,token)).thenReturn(response);

        ResponseEntity<String> responseEntity = databaseSecretService.rotateStaticRoleCredential(token, "testrole");
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }
}