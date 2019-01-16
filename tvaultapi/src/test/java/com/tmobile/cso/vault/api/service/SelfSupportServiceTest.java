package com.tmobile.cso.vault.api.service;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.SafeUtils;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages={"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class})
@PowerMockIgnore({"javax.management.*"})
public class SelfSupportServiceTest {

    @InjectMocks
    SelfSupportService selfSupportService;

    @Mock
    SafesService safesService;

    @Mock
    AuthorizationUtils authorizationUtils;

    @Mock
    SafeUtils safeUtils;

    @Mock
    PolicyUtils policyUtils;

    @Mock
    VaultAuthService vaultAuthService;

    @Mock
    AWSAuthService awsAuthService;

    @Mock
    AWSIAMAuthService awsiamAuthService;

    @Mock
    AppRoleService appRoleService;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);
        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
    }

    UserDetails getMockUser(boolean isAdmin) {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = new UserDetails();
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(isAdmin);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        return userDetails;
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

    private void mockIsAuthorized(UserDetails userDetails, boolean isAuthorized) {
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid("shared/mysafe01")).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(isAuthorized);
    }

    @Test
    public void test_createSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        ReflectionTestUtils.setField(selfSupportService, "safeQuota", "2");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        String [] policies = {"s_shared_s1"};
        when(policyUtils.getCurrentPolicies(token, "normaluser")).thenReturn(policies);
        String [] safes = {"s1"};
        when(safeUtils.getManagedSafes(policies, "shared")).thenReturn(safes);
        when(safesService.createSafe(token, safe)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = selfSupportService.createSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_failure() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":\"You have reached the limit of number of allowed safes that can be created\"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":\"You have reached the limit of number of allowed safes that can be created\"}");
        ReflectionTestUtils.setField(selfSupportService, "safeQuota", "2");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        String [] policies = {"s_shared_s1, s_shared_s2"};
        when(policyUtils.getCurrentPolicies(token, "normaluser")).thenReturn(policies);
        String [] safes = {"s1", "s2"};
        when(safeUtils.getManagedSafes(policies, "shared")).thenReturn(safes);
        when(safesService.createSafe(token, safe)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = selfSupportService.createSafe(userDetails, token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("",safeBasicDetails);

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":\"Invalid input values\"}");

        ResponseEntity<String> responseEntity = selfSupportService.createSafe(userDetails, token, safe);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createSafe_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe and associated read/write/deny policies created \"]}");
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(safesService.createSafe(token, safe)).thenReturn(readResponse);

        ResponseEntity<String> responseEntity = selfSupportService.createSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }


    @Test
    public void test_getFoldersRecursively_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String[] policies = {"s_shared_mysafe01"};
        String[] safes = {"mysafe01"};
        String responseJson = "{\"keys\":[\"mysafe01\"]}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);

        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(safeUtils.getManagedSafes(policies, path)).thenReturn(safes);
        when(safesService.getFoldersRecursively(token, path)).thenReturn(response);
        when(JSONUtil.getJSON(Mockito.any(HashMap.class))).thenReturn("{\"keys\":[\"mysafe01\"]}");
        ResponseEntity<String> responseEntity = selfSupportService.getFoldersRecursively(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getFoldersRecursively_admin() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String responseJson = "{\"keys\":[\"mysafe01\"]}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getFoldersRecursively(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getFoldersRecursively(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
        when(safesService.addUserToSafe(token, safeUser, userDetails)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addUserToSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(true);

        ResponseEntity<String> response =               ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(true);
        when(safesService.addUserToSafe(token, safeUser, userDetails)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addUserToSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addUserToSafe_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(true);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Can't add user. Possible reasons: Invalid path specified, 2. Changing access/permission of safe owner is not allowed, 3. Safeowner/safeadmin have are authorized to change permission of safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Can't add user. Possible reasons: Invalid path specified, 2. Changing access/permission of safe owner is not allowed, 3. Safeowner/safeadmin have are authorized to change permission of safe\"]}");

        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "addUser")).thenReturn(false);
        when(safesService.addUserToSafe(token, safeUser, userDetails)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addUserToSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(safesService.removeUserFromSafe(token, safeUser)).thenReturn(response);
        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "removeUser")).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.removeUserFromSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeUserFromSafe_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeUser safeUser = new SafeUser(path, "testuser1","write");
        UserDetails userDetails = getMockUser(true);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"Message\":\"User association is removed \"}");

        when(safesService.removeUserFromSafe(token, safeUser)).thenReturn(response);
        when(safeUtils.canAddOrRemoveUser(userDetails, safeUser, "removeUser")).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.removeUserFromSafe(userDetails, token, safeUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getInfo(token, path)).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        ResponseEntity<String> responseEntity = selfSupportService.getInfo(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo_admin() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String responseJson = "{\"data\": { \"description\": \"My first safe\", \"name\": \"mysafe01\", \"owner\": \"youremail@yourcompany.com\", \"type\": \"\" }}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getInfo(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getInfo(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getInfo_failure_403() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{\"errors\":[\"Access denied: no permission to get this safe info\"]}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseJson);
        when(safesService.getInfo(token, path)).thenReturn(response);
        mockIsAuthorized(userDetails, false);
        ResponseEntity<String> responseEntity = selfSupportService.getInfo(userDetails, token, path);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_failure_403() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{\"errors\":[\"Not authorized to get Safe information\"]}";

        String[] policies = {"s_shared_mysafe01"};
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01", safeBasicDetails);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseJson);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(safesService.getSafe(token, path)).thenReturn(response);

        when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);

        when(authorizationUtils.isAuthorized(eq(userDetails),eq(safe),eq(policies),Mockito.any(), eq(false))).thenReturn(false);
        ResponseEntity<String> responseEntity = selfSupportService.getSafe(userDetails, token, path);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_successfully() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";

        String[] policies = {"s_shared_mysafe01"};
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01", safeBasicDetails);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(ControllerUtil.getSafeType(path)).thenReturn("shared");
        when(ControllerUtil.getSafeName(path)).thenReturn("mysafe01");
        when(safesService.getSafe(token, path)).thenReturn(response);
        when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(authorizationUtils.isAuthorized(eq(userDetails),eq(safe),eq(policies),Mockito.any(), eq(false))).thenReturn(true);
        ResponseEntity<String> responseEntity = selfSupportService.getSafe(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getSafe_successfully_admin() {

        String path = "shared/mysafe01";
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String responseJson = "{  \"keys\": [ \"mysafe01\" ]}";
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(responseJson);
        when(safesService.getSafe(token, path)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.getSafe(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");

        when(safesService.updateSafe(token, safe)).thenReturn(readResponse);
        mockIsAuthorized(userDetails, true);
        ResponseEntity<String> responseEntity = selfSupportService.updateSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Safe updated \"]}");

        when(safesService.updateSafe(token, safe)).thenReturn(readResponse);
        ResponseEntity<String> responseEntity = selfSupportService.updateSafe(userDetails, token, safe);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);

        ResponseEntity<String> readResponse = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update this safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update this safe\"]}");

        mockIsAuthorized(userDetails, false);
        ResponseEntity<String> responseEntity = selfSupportService.updateSafe(userDetails, token, safe);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        when(safesService.deletefolder(token, path)).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        ResponseEntity<String> responseEntity = selfSupportService.deletefolder(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteSafe_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"SDB deleted\"]}");
        when(safesService.deletefolder(token, path)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.deletefolder(userDetails, token, path);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to delete this safe\"]}");

        mockIsAuthorized(userDetails, false);
        ResponseEntity<String> responseEntity = selfSupportService.deletefolder(userDetails, token, path);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup(path, "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        when(safesService.addGroupToSafe(token, safeGroup)).thenReturn(response);
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid(path)).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_successfully_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup(path, "group1", "write");
        UserDetails userDetails = getMockUser(true);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Safe\"]}");
        when(safesService.addGroupToSafe(token, safeGroup)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addGroupToSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String path = "shared/mysafe01";
        SafeGroup safeGroup = new SafeGroup(path, "group1", "write");
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add group to the safe\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add group to the safe\"]}");
        when(safesService.addGroupToSafe(token, safeGroup)).thenReturn(response);

        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid(path)).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(false);

        ResponseEntity<String> responseEntity = selfSupportService.addGroupToSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");

        when(safesService.removeGroupFromSafe(token, safeGroup)).thenReturn(response);
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid("shared/mysafe01")).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(true);

        ResponseEntity<String> responseEntity = selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove group from the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove group from the safe\"]}");

        when(safesService.removeGroupFromSafe(token, safeGroup)).thenReturn(response);
        String[] policies = {"s_shared_mysafe01"};
        ArrayList<String> policiesTobeChecked = new ArrayList<String>();
        policiesTobeChecked.add("s_shared_mysafe01");
        SafeBasicDetails safeBasicDetails = new SafeBasicDetails("mysafe01", "youremail@yourcompany.com", null, "My first safe");
        Safe safe = new Safe("shared/mysafe01",safeBasicDetails);
        when(ControllerUtil.isPathValid("shared/mysafe01")).thenReturn(true);
        when(ControllerUtil.getSafeType("shared/mysafe01")).thenReturn("shared");
        when(ControllerUtil.getSafeName("shared/mysafe01")).thenReturn("mysafe01");when(safeUtils.getSafeMetaData(Mockito.any(), eq("shared"), eq("mysafe01"))).thenReturn(safe);
        when(policyUtils.getCurrentPolicies(Mockito.any(), eq("normaluser"))).thenReturn(policies);
        when(policyUtils.getPoliciesTobeCheked("shared", "mysafe01")).thenReturn(policiesTobeChecked);
        when(authorizationUtils.isAuthorized(userDetails, safe, policies, policiesTobeChecked, false)).thenReturn(false);

        ResponseEntity<String> responseEntity = selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeGroupFromSafe_successfully_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        SafeGroup safeGroup = new SafeGroup("shared/mysafe01","mygroup01","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group association is removed \"]}");

        when(safesService.removeGroupFromSafe(token, safeGroup)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.removeGroupFromSafe(userDetails, token, safeGroup);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

        when(safesService.addAwsRoleToSafe(token, awsRole)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add AWS role to the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add AWS role to the safe\"]}");

        when(safesService.addAwsRoleToSafe(token, awsRole)).thenReturn(response);
        mockIsAuthorized(userDetails, false);


        ResponseEntity<String> responseEntity = selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_addAwsRoleToSafe_successfully_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role is successfully associated \"]}");

        when(safesService.addAwsRoleToSafe(token, awsRole)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.addAwsRoleToSafe(userDetails, token, awsRole);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");

        when(safesService.removeAWSRoleFromSafe(eq(token), eq(awsRole), eq(false), Mockito.any())).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, false);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove AWS role from the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove AWS role from the safe\"]}");

        when(safesService.removeAWSRoleFromSafe(eq(token), eq(awsRole), eq(false), Mockito.any())).thenReturn(response);
        mockIsAuthorized(userDetails, false);

        ResponseEntity<String> responseEntity = selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, false);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_removeAWSRoleFromSafe_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSRole awsRole = new AWSRole("shared/mysafe01","ec2","read");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");

        when(safesService.removeAWSRoleFromSafe(eq(token), eq(awsRole), eq(true), Mockito.any())).thenReturn(response);
        Response responseOK = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.canDeleteRole(awsRole.getRole(), token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH)).thenReturn(responseOK);
        ResponseEntity<String> responseEntity = selfSupportService.removeAWSRoleFromSafe(userDetails, token, awsRole, true);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");

        when(safesService.associateApproletoSDB(eq(token), Mockito.any())).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(true);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle :approle1 is successfully associated with SDB\"]}");

        when(safesService.associateApproletoSDB(eq(token), Mockito.any())).thenReturn(response);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add Approle to the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add Approle to the safe\"]}");

        when(safesService.associateApproletoSDB(eq(token), Mockito.any())).thenReturn(response);
        mockIsAuthorized(userDetails, false);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(true);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_associateApproletoSDB_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        when(safesService.associateApproletoSDB(eq(token), Mockito.any())).thenReturn(response);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(false);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.associateApproletoSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteApproleFromSDB_successfully() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");

        when(safesService.removeApproleFromSafe(token, jsonStr)).thenReturn(response);
        mockIsAuthorized(userDetails, true);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(true);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.deleteApproleFromSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteApproleFromSDB_successfully_admin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Role association is removed \"]}");

        when(safesService.removeApproleFromSafe(token, jsonStr)).thenReturn(response);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.deleteApproleFromSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteApproleFromSDB_failure_403() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove approle from the safe\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove approle from the safe\"]}");

        mockIsAuthorized(userDetails, false);
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(true);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.deleteApproleFromSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_deleteApproleFromSDB_failure_400() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String jsonStr = "{\"role_name\":\"approle1\",\"path\":\"shared/mysafe01\",\"access\":\"write\"}";

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");

        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("path", "shared/mysafe01");
        requestMap.put("role_name", "aprole1");
        requestMap.put("access", "write");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);
        when(ControllerUtil.areSafeAppRoleInputsValid(requestMap)).thenReturn(false);
        SafeAppRoleAccess safeAppRoleAccess = new SafeAppRoleAccess("aprole1", "shared/mysafe01", "write");
        when(JSONUtil.getJSON(Mockito.any(SafeAppRoleAccess.class))).thenReturn(jsonStr);
        ResponseEntity<String> responseEntity = selfSupportService.deleteApproleFromSDB(userDetails, token, safeAppRoleAccess);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_successfully() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        when(awsAuthService.createRole(eq(token), eq(awsLoginRole), Mockito.any())).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.createRole(userDetails, token, awsLoginRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_successfully_admin() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");

        when(awsAuthService.createRole(eq(token), eq(awsLoginRole), Mockito.any())).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.createRole(userDetails, token, awsLoginRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createRole_failure_403() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to create AWS role\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to create AWS role\"]}");

        when(awsAuthService.createRole(eq(token), eq(awsLoginRole), Mockito.any())).thenReturn(response);
        mockIsAuthorized(userDetails, false);

        ResponseEntity<String> responseEntity = selfSupportService.createRole(userDetails, token, awsLoginRole,"shared/mysafe01");
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateRole_successfully() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{ \"messages\": [\"AWS Role updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{ \"messages\": [\"AWS Role updated \"]}");

        when(awsAuthService.updateRole(token, awsLoginRole)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.updateRole(userDetails, token, awsLoginRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateRole_successfully_admin() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{ \"messages\": [\"AWS Role updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{ \"messages\": [\"AWS Role updated \"]}");

        when(awsAuthService.updateRole(token, awsLoginRole)).thenReturn(response);
        ResponseEntity<String> responseEntity = selfSupportService.updateRole(userDetails, token, awsLoginRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateRole_failure_403() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSLoginRole awsLoginRole = new AWSLoginRole("ec2", "mytestawsrole", "ami-fce3c696",
                "1234567890123", "us-east-2", "vpc-2f09a348", "subnet-1122aabb",
                "arn:aws:iam::8987887:role/test-role", "arn:aws:iam::877677878:instance-profile/exampleinstanceprofile",
                "\"[prod, dev\"]");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update AWS role\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update AWS role\"]}");

        when(awsAuthService.updateRole(token, awsLoginRole)).thenReturn(response);
        mockIsAuthorized(userDetails, false);

        ResponseEntity<String> responseEntity = selfSupportService.updateRole(userDetails, token, awsLoginRole,"shared/mysafe01");
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createIAMRole_successfully() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        when(awsiamAuthService.createIAMRole(awsiamRole, token, userDetails)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.createIAMRole(userDetails, token, awsiamRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createIAMRole_failure_403() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to create AWS IAM role\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to create AWS IAM role\"]}");
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        when(awsiamAuthService.createIAMRole(awsiamRole, token, userDetails)).thenReturn(response);
        mockIsAuthorized(userDetails, false);

        ResponseEntity<String> responseEntity = selfSupportService.createIAMRole(userDetails, token, awsiamRole,"shared/mysafe01");
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createIAMRole_successfully_admin() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS IAM Role created successfully \"]}");
        when(ControllerUtil.createMetadata(Mockito.any(), eq(token))).thenReturn(true);
        when(awsiamAuthService.createIAMRole(awsiamRole, token, userDetails)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.createIAMRole(userDetails, token, awsiamRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateIAMRole_successfully() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");

        when(awsiamAuthService.updateIAMRole(token, awsiamRole)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.updateIAMRole(userDetails, token, awsiamRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateIAMRole_failure_403() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update AWS IAM role\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update AWS IAM role\"]}");

        when(awsiamAuthService.updateIAMRole(token, awsiamRole)).thenReturn(response);
        mockIsAuthorized(userDetails, false);

        ResponseEntity<String> responseEntity = selfSupportService.updateIAMRole(userDetails, token, awsiamRole,"shared/mysafe01");
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_updateIAMRole_successfully_admin() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        AWSIAMRole awsiamRole = new AWSIAMRole();
        awsiamRole.setAuth_type("iam");
        String[] arns = {"arn:aws:iam::123456789012:user/tst"};
        awsiamRole.setBound_iam_principal_arn(arns);
        String[] policies = {"default"};
        awsiamRole.setPolicies(policies);
        awsiamRole.setResolve_aws_unique_ids(true);
        awsiamRole.setRole("string");

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");

        when(awsiamAuthService.updateIAMRole(token, awsiamRole)).thenReturn(response);

        ResponseEntity<String> responseEntity = selfSupportService.updateIAMRole(userDetails, token, awsiamRole,"shared/mysafe01");
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getAllSafeNames_successfully() {

        UserDetails userDetails = getMockUser(false);

        HashMap<String, List<String>> safeNames = new HashMap<>();
        List<String> appSafes = new ArrayList<>();
        appSafes.add("safe1");
        appSafes.add("safe2");
        List<String> userSafes = new ArrayList<>();
        userSafes.add("safe3");
        userSafes.add("safe4");
        List<String> sharedSafes = new ArrayList<>();
        sharedSafes.add("safe5");
        sharedSafes.add("safe6");
        safeNames.put("apps", appSafes);
        safeNames.put("users", userSafes);
        safeNames.put("shared", sharedSafes);
        when(ControllerUtil.getAllExistingSafeNames(Mockito.any())).thenReturn(safeNames);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeNames));

        ResponseEntity<String> responseEntity = selfSupportService.getAllSafeNames(userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_getAllSafeNames_failure() {

        UserDetails userDetails = getMockUser(false);

        HashMap<String, List<String>> safeNames = new HashMap<>();

        when(ControllerUtil.getAllExistingSafeNames(Mockito.any())).thenReturn(safeNames);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("No safes are available");

        ResponseEntity<String> responseEntity = selfSupportService.getAllSafeNames(userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createAppRole_successfully() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, "1", "100m", 0);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created successfully\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created successfully\"]}");

        when(appRoleService.createAppRole(token, appRole, userDetails)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void test_createAppRole_successfully_admin() throws TVaultValidationException {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(true);
        String [] policies = {"default"};
        AppRole appRole = new AppRole("approle1", policies, true, "1", "100m", 0);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created successfully\"]}");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AppRole created successfully\"]}");

        when(appRoleService.createAppRole(token, appRole, userDetails)).thenReturn(response);
        mockIsAuthorized(userDetails, true);

        ResponseEntity<String> responseEntity = selfSupportService.createAppRole(token, appRole, userDetails);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }
}
