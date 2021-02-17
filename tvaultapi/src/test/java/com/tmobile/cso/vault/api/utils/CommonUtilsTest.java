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
package com.tmobile.cso.vault.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.junit.Before;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class CommonUtilsTest {

    @InjectMocks
    CommonUtils commonUtils;

    @Mock
    RequestProcessor reqProcessor;
    @Before
    public void setUp() {

		PowerMockito.mockStatic(JSONUtil.class);
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
		if (expectedBody != "") {
			response.setResponse(expectedBody);
		}
		return response;
	}

    
	@Test
    public void test_getPoliciesAsArray() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = "{\"id\":\"1JRRXfKHYuN2zbl1gkRM12zA\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":[\"normal_user\",\"default\"], \"identity_policies\":[\"default2\"],\"creation_ttl\":1800000,\"username\":\"normaluser\"}";
        String[] expectedPolicies = {"approle_normal_user", "default"};
        String[] policies = commonUtils.getPoliciesAsArray(objMapper, jsonStr);

    }

    @Test
    public void test_getPoliciesAsArray_string() throws Exception {
        ObjectMapper objMapper = new ObjectMapper();
        String jsonStr = "{\"id\":\"1JRRXfKHYuN2zbl1gkRM12zA\",\"last_renewal_time\":null,\"renewable\":false,\"policies\":\"approle_normal_user\", \"identity_policies\":[\"default2\"], \"creation_ttl\":1800000,\"username\":\"normaluser\"}";
        String[] expectedPolicies = {"approle_normal_user"};
        String[] policies = commonUtils.getPoliciesAsArray(objMapper, jsonStr);

    }

    @Test
    public void test_getModifiedByInfo() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setEmail("normaluser@company.com");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        String modifiedBy = commonUtils.getModifiedByInfo(userDetails);
        assertEquals("normaluser@company.com", modifiedBy);
    }

    @Test
    public void test_getModifiedByInfo_approle() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("approle");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse("{\"meta\": {\"role_name\": \"role1\"}}");
        Mockito.when(reqProcessor.process("/auth/tvault/lookup", "{}", token)).thenReturn(response);

        String modifiedBy = commonUtils.getModifiedByInfo(userDetails);
        assertEquals("role1 (AppRole)", modifiedBy);
    }

    @Test
    public void test_getModifiedByInfo_approle_empty() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("approle");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse("{\"meta\": {}}");
        Mockito.when(reqProcessor.process("/auth/tvault/lookup", "{}", token)).thenReturn(response);

        String modifiedBy = commonUtils.getModifiedByInfo(userDetails);
        assertEquals("AppRole", modifiedBy);
    }

    @Test
    public void test_getModifiedByInfo_awsrole() throws Exception {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("aws");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);

        String modifiedBy = commonUtils.getModifiedByInfo(userDetails);
        assertEquals("AWS Role", modifiedBy);
    }
    
    @Test
	public void test_isAuthorizedToken_sucessfully() {

		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

		String responsejson = "{\"id\":\"5PDrOhsy4ig8L3EpsJZSLAMg\",\"policies\":[\"root\"],\"username\":null,\"identity_policies\":null,\"display_name\":\"root\"}";

		Response response = getMockResponse(HttpStatus.OK, true, responsejson);
		when(reqProcessor.process(Mockito.eq("/auth/tvault/lookup"), Mockito.eq("{}"), Mockito.eq(token)))
				.thenReturn(response);

		boolean responseEntity = commonUtils.isAuthorizedToken(token);

		assertEquals(Boolean.TRUE, responseEntity);
	}

	@Test
	public void test_sAuthorizedToken_failed() {

		String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

		String responsejson = "{\"id\":\"5PDrOhsy4ig8L3EpsJZSLAMg\",\"policies\":[\"default\"],\"username\":null,\"identity_policies\":null,\"display_name\":\"root\"}";

		Response response = getMockResponse(HttpStatus.OK, false, responsejson);
		when(reqProcessor.process(Mockito.eq("/auth/tvault/lookup"), Mockito.eq("{}"), Mockito.eq(token)))
				.thenReturn(response);

		boolean responseEntity = commonUtils.isAuthorizedToken(token);

		assertEquals(Boolean.FALSE, responseEntity);
	}
}
