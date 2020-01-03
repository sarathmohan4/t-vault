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
package com.tmobile.cso.vault.api.v2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.main.Application;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.service.DatabaseSecretService;
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@WebAppConfiguration
public class DatabaseSecretControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DatabaseSecretService databaseSecretService;

    @InjectMocks
    private DatabaseSecretController databaseSecretController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(databaseSecretController).build();
    }

    @Test
    public void test_createRole() throws Exception {
        DatabaseRole databaseRole = new DatabaseRole();
        databaseRole.setDb_name("testdb");
        databaseRole.setRole_name("testrole");
        String creationStmt[] = {"Create User '{{name}}'@'%' identified by '{{password}}'; Grant All on vault.* to '{{name}}'@'%';"};
        databaseRole.setCreation_statements(creationStmt);
        databaseRole.setDefault_ttl("1h");
        databaseRole.setMax_ttl("24h");

        String inputJson = new ObjectMapper().writeValueAsString(databaseRole);
        String responseMessage = "{\"messages\":[\"Database role created successfully\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.createRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/database/roles")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_createStaticRole() throws Exception {
        DatabaseStaticRole databaseRole = new DatabaseStaticRole();
        databaseRole.setDb_name("testdb");
        databaseRole.setName("testrole");
        String rotationStmt[] = {"ALTER USER '{{name}}' IDENTIFIED BY '{{password}}';"};
        databaseRole.setRotation_statements(rotationStmt);
        databaseRole.setUsername("testuser1");
        databaseRole.setRotation_period("1h");
        String inputJson = new ObjectMapper().writeValueAsString(databaseRole);
        String responseMessage = "{\"messages\":[\"Static database role created successfully\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.createStaticRole(eq("5PDrOhsy4ig8L3EpsJZSLAMg"), Mockito.any())).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/database/static-roles")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .content(inputJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_getStaticCredentials() throws Exception {

        String responseMessage = "{\n" +
                "  \"username\": \"statuser1\",\n" +
                "  \"password\": \"A1a-dl625IaehGUp8ZTS\",\n" +
                "  \"last_vault_rotation\": \"2020-01-02T17:23:45.9407189+05:30\",\n" +
                "  \"rotation_period\": 30,\n" +
                "  \"ttl\": 27\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.getStaticCredentials("rolename1", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/database/static-creds/rolename1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_getTemporaryCredentials() throws Exception {

        String responseMessage = "{\n" +
                "  \"data\": {\n" +
                "    \"username\": \"root-1430158508-126\",\n" +
                "    \"password\": \"132ae3ef-5a64-7499-351e-bfe59f3a2a21\"\n" +
                "  }\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.getTemporaryCredentials("rolename1", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/database/role/credentials/rolename1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_readRole() throws Exception {

        String responseMessage = "{\n" +
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
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.readRole("rolename1", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/database/roles/rolename1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_listRoles() throws Exception {

        String responseMessage = "{\n" +
                "  \"auth\": null,\n" +
                "  \"data\": {\n" +
                "    \"keys\": [\"dev\", \"prod\"]\n" +
                "  },\n" +
                "  \"lease_duration\": 2764800,\n" +
                "  \"lease_id\": \"\",\n" +
                "  \"renewable\": false\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.listRoles("5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/database/roles/")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_deleteRole() throws Exception {

        // Mock response
        String responseMessage = "";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.deleteRole("rolename1", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/database/roles/rolename1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_readStaticRole() throws Exception {

        String responseMessage = "{\n" +
                "    \"data\": {\n" +
                "        \"db_name\": \"mysql\",\n" +
                "    \"username\":\"static-user\",\n" +
                "    \"rotation_statements\": [\"ALTER USER \"{{name}}\" WITH PASSWORD '{{password}}';\"],\n" +
                "    \"rotation_period\":\"1h\",\n" +
                "    },\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.readStaticRole("rolename1", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/database/static-roles/rolename1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_listStaticRoles() throws Exception {

        String responseMessage = "{\n" +
                "  \"auth\": null,\n" +
                "  \"data\": {\n" +
                "    \"keys\": [\"dev-static\", \"prod-static\"]\n" +
                "  }\n" +
                "}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.listStaticRoles("5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.get("/v2/database/static-roles/")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_deleteStaticRole() throws Exception {

        // Mock response
        String responseMessage = "";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.deleteStaticRole("rolename1", "5PDrOhsy4ig8L3EpsJZSLAMg")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.delete("/v2/database/static-roles/rolename1")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

    @Test
    public void test_rotateStaticRoleCredential() throws Exception {

        String responseMessage = "";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseMessage);

        when(databaseSecretService.rotateStaticRoleCredential("5PDrOhsy4ig8L3EpsJZSLAMg", "testrole")).thenReturn(responseEntityExpected);

        mockMvc.perform(MockMvcRequestBuilders.post("/v2/database/rotate-role")
                .header("vault-token", "5PDrOhsy4ig8L3EpsJZSLAMg")
                .header("Content-Type", "application/json;charset=UTF-8")
                .param("role_name", "testrole"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(responseMessage)));
    }

}
