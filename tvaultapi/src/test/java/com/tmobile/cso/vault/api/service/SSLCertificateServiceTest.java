package com.tmobile.cso.vault.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CertificateUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.validator.TokenValidator;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@ComponentScan(basePackages = {"com.tmobile.cso.vault.api"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@PrepareForTest({ControllerUtil.class, JSONUtil.class,EntityUtils.class,HttpClientBuilder.class})
@PowerMockIgnore({"javax.management.*", "javax.net.ssl.*"})
public class SSLCertificateServiceTest {

    private MockMvc mockMvc;

    @InjectMocks
    SSLCertificateService sSLCertificateService;

    @Mock
    private RequestProcessor reqProcessor;

    @Mock
    UserDetails userDetails;

    @Mock
    VaultAuthService vaultAuthService;

    @Mock
    PolicyUtils policyUtils;

    @Mock
    TokenValidator tokenValidator;

    String token;

    @Mock
    CloseableHttpResponse httpResponse;

    @Mock
    HttpClientBuilder httpClientBuilder;

    @Mock
    StatusLine statusLine;

    @Mock
    HttpEntity mockHttpEntity;

    @Mock
    CloseableHttpClient httpClient1;

    @Mock
    CertificateData certificateData;

    @Mock
    private WorkloadDetailsService workloadDetailsService;

    @Mock
    CertificateUtils certificateUtils;

    @Mock
	private AppRoleService appRoleService;
    
    @Mock
    ObjectMapper obj;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ControllerUtil.class);
        PowerMockito.mockStatic(JSONUtil.class);
        PowerMockito.mockStatic(HttpClientBuilder.class);
        PowerMockito.mockStatic(EntityUtils.class);


        Whitebox.setInternalState(ControllerUtil.class, "log", LogManager.getLogger(ControllerUtil.class));
        when(JSONUtil.getJSON(any(ImmutableMap.class))).thenReturn("log");

        Map<String, String> currentMap = new HashMap<>();
        currentMap.put("apiurl", "http://localhost:8080/v2/sslcert");
        currentMap.put("user", "");
        ThreadLocalContext.setCurrentMap(currentMap);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "userpass");
        ReflectionTestUtils.setField(sSLCertificateService, "certManagerDomain", "https://mobile.com:3004/");
        ReflectionTestUtils.setField(sSLCertificateService, "tokenGenerator", "token?grant_type=client_credentials");
        ReflectionTestUtils.setField(sSLCertificateService, "targetSystemGroups", "targetsystemgroups/");
        ReflectionTestUtils.setField(sSLCertificateService, "certificateEndpoint", "certificates/");
        ReflectionTestUtils.setField(sSLCertificateService, "targetSystems", "targetsystems");
        ReflectionTestUtils.setField(sSLCertificateService, "targetSystemServies", "targetsystemservices");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollUrl", "enroll?entityId=entityid&entityRef=SERVICE");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollCAUrl", "policy/ca?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=0");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollTemplateUrl", "policy/ca/caid/templates?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=0");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollKeysUrl", "policy/keytype?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=templateId");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollCSRUrl", "policy/csr?entityRef=SERVICE&entityId=entityid&allowedOnly=true&withTemplateById=templateId");
        ReflectionTestUtils.setField(sSLCertificateService, "findTargetSystem", "targetsystemgroups/tsgid/targetsystems");
        ReflectionTestUtils.setField(sSLCertificateService, "findTargetSystemService", "targetsystems/tsgid/targetsystemservices");
        ReflectionTestUtils.setField(sSLCertificateService, "enrollUpdateCSRUrl", "policy/csr?entityRef=SERVICE&entityId=entityid&allowedOnly=true&enroll=true");
        ReflectionTestUtils.setField(sSLCertificateService, "findCertificate", "certificates?freeText=certname&containerId=cid");
        ReflectionTestUtils.setField(sSLCertificateService, "certManagerUsername", "dGVzdGluZw==");
        ReflectionTestUtils.setField(sSLCertificateService, "certManagerPassword", "dGVzdGluZw==");
        ReflectionTestUtils.setField(sSLCertificateService, "retrycount", 1);
        ReflectionTestUtils.setField(sSLCertificateService, "getCertifcateReasons", "certificates/certID/revocationreasons");
        ReflectionTestUtils.setField(sSLCertificateService, "issueRevocationRequest", "certificates/certID/revocationrequest");

        ReflectionTestUtils.setField(sSLCertificateService, "certificateNameTailText", ".t-mobile.com");
        ReflectionTestUtils.setField(sSLCertificateService, "renewDelayTime", 3000);
		ReflectionTestUtils.setField(sSLCertificateService, "renewCertificateEndpoint", "certificates/certID/renew");

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails.setUsername("normaluser");
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setSelfSupportToken(token);
        when(vaultAuthService.lookup(anyString())).thenReturn(new ResponseEntity<>(HttpStatus.OK));


        ReflectionTestUtils.setField(sSLCertificateService, "workloadEndpoint", "http://appdetails.com");
       // when(ControllerUtil.getCwmToken()).thenReturn("dG9rZW4=");
        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);
        String responseStr = "{\"spec\":{\"akmid\":\"103001\",\"brtContactEmail\":\" contacteops@email.com\",\"businessUnit\":\"\"," +
                "\"classification\":\"\",\"directorEmail\":\"john.mathew@email.com\",\"directorName\":\"test jin\",\"executiveSponsor\":" +
                "\"robert sam\",\"executiveSponsorEmail\":\"kim.tim@email.com\",\"id\":\"tvt\"," +
                "\"intakeDate\":\"2018-01-01\",\"opsContactEmail\":\"abc.def@gmail.com\",\"projectLeadEmail\":\"abc.def@gmail.com\"," +
                "\"tag\":\"T-Vault\",\"tier\":\"Tier II\",\"workflowStatus\":\"Open_CCP\",\"workload\":\"Adaptive Security\"}}";
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        InputStream inputStream = new ByteArrayInputStream(responseStr.getBytes());
        when(mockHttpEntity.getContent()).thenReturn(inputStream);

        String workloadApiResponse = "{\"kind\":\"Application\",\"spec\":{\"akmid\":\"103001\",\"brtContactEmail\":\"" +
                " testspec@mail.com\",\"businessUnit\":\"\",\"classification\":\"\",\"directorEmail\":\"abc.joe@mail.com\"," +
                "\"directorName\":\"abc amith\",\"executiveSponsor\":\"Dar Web\",\"opsContactEmail\":\"rick.nick@test.com\"," +
                "\"organizationalUnits\":[\"tvt\"],\"projectLeadEmail\":\"rick.nick@test.com\",\"scope\":\"Production\",\"summary\":" +
                "\"T-Vault\",\"tag\":\"T-Vault\",\"tier\":\"Tier II\",\"workflowStatus\":\"Open_CCP\",\"workload\":\"Adaptive Security\"}}";
        when(workloadDetailsService.getWorkloadDetailsByAppName(anyString())).
                thenReturn(ResponseEntity.status(HttpStatus.OK).body(workloadApiResponse));

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
    public void login_failure() throws Exception {
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        CertManagerLogin  certManagerLogin= sSLCertificateService.login(certManagerLoginRequest);
        assertNull(certManagerLogin);
    }

    @Test
    public void login_success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        CertManagerLogin  certManagerLogin= sSLCertificateService.login(certManagerLoginRequest);
        assertNotNull(certManagerLogin);
        assertEquals(certManagerLogin.getAccess_token(),"12345");
    }


    @Test
    public void test_validateInputData(){
        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        sslCertificateRequest.setCertificateName("qeqeqwe");
        ResponseEntity<?> enrollResponse = sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());

        sslCertificateRequest.setCertificateName("abc.t-mobile.com");
        sslCertificateRequest.getTargetSystem().setAddress("abc def");
        ResponseEntity<?> enrollResponse1= sSLCertificateService.generateSSLCertificate(sslCertificateRequest,
                userDetails,token);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse1.getStatusCode());


        sslCertificateRequest.setCertificateName("qeqeqwe.t-mobile.com");
        sslCertificateRequest.getTargetSystem().setAddress("abcdef");
        sslCertificateRequest.getTargetSystemServiceRequest().setHostname("abc abc");
        ResponseEntity<?> enrollResponse2= sSLCertificateService.generateSSLCertificate(sslCertificateRequest,
                userDetails,token);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse2.getStatusCode());
    }


    @Test
    public void test_authenticate_success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername\",  \"password\": \"testpassword\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        ResponseEntity<String> responseEntity = sSLCertificateService.authenticate(certManagerLoginRequest);
        assertEquals(HttpStatus.OK,responseEntity.getStatusCode());
    }

    @Test
    public void test_authenticate_Unauthorized() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.UNAUTHORIZED);
        response.setResponse("Success");
        response.setSuccess(true);
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest("testusername", "testpassword");
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(jsonStr);
        when(reqProcessor.processCert(Mockito.anyString(), Mockito.anyObject(), Mockito.anyString(), Mockito.anyString())).thenReturn(response);
        ResponseEntity<String> responseEntity = sSLCertificateService.authenticate(certManagerLoginRequest);
        assertEquals(HttpStatus.UNAUTHORIZED,responseEntity.getStatusCode());

    }
    
    @Test
    public void generateSSLCertificate_Success() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");
        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);
        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"," +
                "\"targetSystemID\": \"29\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr1);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);
        
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");
        
        
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        String certType = "internal";
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", certType)).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }




    @Test
    public void generateSSLCertificate_With_Target_System_Failure() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);

        CertResponse response2 = new CertResponse();
        response2.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response2.setResponse(jsonStr);
        response2.setSuccess(false);

        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }


    @Test
    public void generateSSLCertificate_With_Target_System_Service_Failure() throws Exception   {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response2.setResponse(jsonStr1);
        response2.setSuccess(false);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);


        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_Certificate_Already_Exists() throws Exception {
        String jsonStr = "{ \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        String jsonStr1 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=certificatename.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);


        Map<String, Object> requestMap1= new HashMap<>();
        requestMap1.put("certificates", "certificates");
        requestMap1.put("certificateStatus", "Active");
        when(ControllerUtil.parseJson(jsonStr1)).thenReturn(requestMap1);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.BAD_REQUEST);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response1);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.BAD_REQUEST, enrollResponse.getStatusCode());
    }



    @Test
    public void generateSSLCertificate_With_PolicyFailure() throws Exception {

        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGETSYSTEM1");
        createTargetSystemMap.put("description", "TARGETSYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr1);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGETSYSTEMService\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGETSYSTEMSERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        UserDetails userDetails1 = new UserDetails();
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails1.setUsername("admin");
        userDetails1.setAdmin(true);
        userDetails1.setClientToken(token);
        userDetails1.setSelfSupportToken(token);

        Response responseNoContent = getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails1,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }
   
    @Test
    public void generateSSLCertificate_With_existing_target_system() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);

        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        String jsonStr1 ="{\"targetSystems\":[{\"address\":\"abcUser.t-mobile.com\",\"allowedOperations\":[\"targetsystems_delete\"],\"name\":\"Target Name\",\"targetSystemGroupID\":29,\"targetSystemID\":7239}]}";
        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);


        Map<String, Object> requestMap1= new HashMap<>();
        requestMap1.put("targetSystems", "targetSystems");
        requestMap1.put("name", "Target Name");
        requestMap1.put("targetSystemID", 29);
        when(ControllerUtil.parseJson(jsonStr1)).thenReturn(requestMap1);

        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr2 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr2);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGET SYSTEM Service\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGET SYSTEM SERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");

        Response responseOkContent = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseOkContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_With_existing_target_Service_system() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        userDetails = new UserDetails();
        userDetails.setAdmin(true);
        userDetails.setClientToken(token);
        userDetails.setUsername("testusername1");
        userDetails.setSelfSupportToken(token);

        String userDetailToken = userDetails.getSelfSupportToken();

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_CertificateName.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");

        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);

        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        String jsonStr1 ="{\"targetSystems\":[{\"address\":\"abcUser.t-mobile.com\",\"allowedOperations\":[\"targetsystems_delete\"],\"name\":\"Target Name\",\"targetSystemGroupID\":29,\"targetSystemID\":7239}]}";
        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr1);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);


        Map<String, Object> requestMap1= new HashMap<>();
        requestMap1.put("targetSystems", "targetSystems");
        requestMap1.put("name", "Target Name");
        requestMap1.put("targetSystemID", 29);
        when(ControllerUtil.parseJson(jsonStr1)).thenReturn(requestMap1);

        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        String createTargetSystemServiceResponse =
                "{\"targetsystemservices\":[{\"name\":\"Target System Service Name\",\"targetSystemGroupId\":29,\"targetSystemId\":7239,\"targetSystemServiceId\":9990}]}";

        CertResponse response2 = new CertResponse();
        String jsonStr2 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr2);
        response2.setSuccess(true);
        response2.setResponse(createTargetSystemServiceResponse);

       when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);

        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("targetsystemservices", "targetsystemservices");
        createTargetSystemServiceMap.put("name", "Target System Service Name");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);
        
        String metaDataStr = "{ \"data\": {\"certificateName\": \"certificatename.t-mobile.com\", \"appName\": \"tvt\", \"certType\": \"internal\", \"certOwnerNtid\": \"testusername1\"}, \"path\": \"sslcerts/certificatename.t-mobile.com\"}";
        String metadatajson = "{\"path\":\"sslcerts/certificatename.t-mobile.com\",\"data\":{\"certificateName\":\"certificatename.t-mobile.com\",\"appName\":\"tvt\",\"certType\":\"internal\",\"certOwnerNtid\":\"testusername1\"}}";
        Map<String, Object> createCertPolicyMap = new HashMap<>();
        createCertPolicyMap.put("certificateName", "certificatename.t-mobile.com");
        createCertPolicyMap.put("appName", "tvt");
        createCertPolicyMap.put("certType", "internal");
        createCertPolicyMap.put("certOwnerNtid", "testusername1");

        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(true);
        when(reqProcessor.process(eq("/access/update"),any(),eq(userDetailToken))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_CertificateName.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "CertificateName.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);
        
        when(JSONUtil.getJSON(Mockito.any())).thenReturn(metaDataStr);
        when(ControllerUtil.parseJson(metaDataStr)).thenReturn(createCertPolicyMap);
        when(ControllerUtil.convetToJson(any())).thenReturn(metadatajson);
        when(reqProcessor.process("/write", metadatajson, token)).thenReturn(responseNoContent);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void generateSSLCertificate_With_MetaDataFailure() throws Exception {

        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);


        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(response);

        CertResponse response1 = new CertResponse();
        response1.setHttpstatus(HttpStatus.OK);
        response1.setResponse(jsonStr);
        response1.setSuccess(true);

        //Create Target System Validation
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystem"), anyObject(), anyString(), anyString())).thenReturn(response1);
        String createTargetSystemResponse = "{  \"name\": \"TARGET SYSTEM1\",  \"password\": \"testpassword1\"}";
        response1.setResponse(createTargetSystemResponse);
        Map<String, Object> createTargetSystemMap = new HashMap<>();
        createTargetSystemMap.put("targetSystemID", 29);
        createTargetSystemMap.put("name", "TARGET SYSTEM1");
        createTargetSystemMap.put("description", "TARGET SYSTEM1");
        createTargetSystemMap.put("address", "address");
        when(ControllerUtil.parseJson(createTargetSystemResponse)).thenReturn(createTargetSystemMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystem/create"), anyObject(), anyString(), anyString())).thenReturn(response1);

        // loadTargetSystemServiceData();

        //Create Target System Validation
        CertResponse response2 = new CertResponse();
        String jsonStr1 = "{  \"name\": \"targetService\",  \"address\": \"targetServiceaddress\"}";
        response2.setHttpstatus(HttpStatus.OK);
        response2.setResponse(jsonStr1);
        response2.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findTargetSystemService"), anyObject(), anyString(), anyString())).thenReturn(response2);
        String createTargetSystemServiceResponse =
                "{  \"name\": \"TARGETSYSTEMService\",  \"password\": , \"testpassword1\"}";
        response2.setResponse(createTargetSystemServiceResponse);
        Map<String, Object> createTargetSystemServiceMap = new HashMap<>();
        createTargetSystemServiceMap.put("targetSystemServiceId", 40);
        createTargetSystemServiceMap.put("hostname", "TARGETSYSTEMSERVICEHOST");
        createTargetSystemServiceMap.put("name", "TARGETSYSTEMSERVICE");
        createTargetSystemServiceMap.put("port", 443);
        createTargetSystemServiceMap.put("targetSystemGroupId", 11);
        createTargetSystemServiceMap.put("targetSystemId", 12);

        Response responseNoContent = getMockResponse(HttpStatus.OK, true, "");
        when(ControllerUtil.createMetadata(Mockito.any(), any())).thenReturn(false);
        when(reqProcessor.process(eq("/access/update"),any(),eq(token))).thenReturn(responseNoContent);

        when(ControllerUtil.parseJson(createTargetSystemServiceResponse)).thenReturn(createTargetSystemServiceMap);
        when(reqProcessor.processCert(eq("/certmanager/targetsystemservice/create"), anyObject(), anyString(), anyString())).thenReturn(response2);

        //getEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///putEnrollCA Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCA"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCAResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollTemplate Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollTemplates"), anyObject(), anyString(), anyString())).thenReturn(getEnrollTemplateResponse());

        ///getEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollkeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///putEnrollKeys Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollKeys"), anyObject(), anyString(), anyString())).thenReturn(getEnrollKeysResponse());

        ///getEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/getEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        ///putEnrollCSR Validation
        when(reqProcessor.processCert(eq("/certmanager/putEnrollCSR"), anyObject(), anyString(), anyString())).thenReturn(getEnrollCSRResponse());

        //enroll
        when(reqProcessor.processCert(eq("/certmanager/enroll"), anyObject(), anyString(), anyString())).thenReturn(getEnrollResonse());

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());
    }






    @Test
    public void generateSSLCertificate_Failure() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertManagerLoginRequest certManagerLoginRequest = getCertManagerLoginRequest();
        certManagerLoginRequest.setUsername("username");
        certManagerLoginRequest.setPassword("password");

        SSLCertificateRequest sslCertificateRequest = getSSLCertificateRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse(jsonStr);
        response.setSuccess(true);
       doThrow(new TVaultValidationException("Exception while creating certificate"))
                .when(reqProcessor).processCert(anyString(), anyObject(), anyString(), anyString());
        ResponseEntity<?> enrollResponse = sSLCertificateService.generateSSLCertificate(sslCertificateRequest,
                userDetails,token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, enrollResponse.getStatusCode());

    }

    @Test
    public void getSSLCertificate_Succes()throws Exception{
    	 String token = "12345";

         Response response =getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
         Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

         token = "5PDrOhsy4ig8L3EpsJZSLAMg";
         UserDetails user1 = new UserDetails();
         user1.setUsername("normaluser");
         user1.setAdmin(true);
         user1.setClientToken(token);
         user1.setSelfSupportToken(token);


         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);

         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "",1,0);

         assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
    }

    @Test
    public void getSSLCertificate_Failure()throws Exception{
    	 String token = "12345";

         Response response =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
         Response certResponse =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

         token = "5PDrOhsy4ig8L3EpsJZSLAMg";
         UserDetails user1 = new UserDetails();
         user1.setUsername("normaluser");
         user1.setAdmin(true);
         user1.setClientToken(token);
         user1.setSelfSupportToken(token);


         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

         when(reqProcessor.process("/sslcert", "{\"path\":\"metadata/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);

         ResponseEntity<String> responseEntityActual = sSLCertificateService.getServiceCertificates(token, user1, "",1,0);

         assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
    }

    private CertResponse getEnrollResonse() {
        CertResponse enrollResponse = new CertResponse();
        enrollResponse.setHttpstatus(HttpStatus.NO_CONTENT);
        enrollResponse.setResponse("Certificate Created Successfully In NCLM");
        enrollResponse.setSuccess(Boolean.TRUE);

        return enrollResponse;
    }


    private CertResponse getEnrollCSRResponse() {
        String enrollCSRResponse = "{\"subject\":{\"items\":[{\"typeName\":\"cn\",\"parameterId\":0," +
                "\"removable\":false,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"\",\"entityId\":0,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"\",\"entityId\":0,\"displayName\":\"\"}},\"value\":[{\"id\":0,\"parentId\":null,\"locked\":false,\"value\":\"\",\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"c\",\"parameterId\":119,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31216,\"parentId\":null,\"locked\":false,\"value\":\"US\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"o\",\"parameterId\":122,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31219,\"parentId\":null,\"locked\":false,\"value\":\"T-Mobile USA, Inc.\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"ou\",\"parameterId\":123,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31215,\"parentId\":null,\"locked\":false,\"value\":\"Business Systems\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"l\",\"parameterId\":121,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31218,\"parentId\":null,\"locked\":false,\"value\":\"Bothell\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null},{\"typeName\":\"st\",\"parameterId\":126,\"removable\":true,\"denyMore\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"required\":{\"id\":0,\"value\":false,\"disabled\":false,\"owner\":{\"entityRef\":\"SERVICE\",\"entityId\":13819,\"displayName\":\"\"}},\"value\":[{\"id\":31217,\"parentId\":null,\"locked\":false,\"value\":\"Washington\",\"owner\":{\"entityRef\":\"CONTAINER\",\"entityId\":1284,\"displayName\":\"Private Certificates\"},\"disabled\":false}],\"whitelist\":null,\"blacklist\":null}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollCSRResponse);
        response.setSuccess(true);

        return response;
    }


    private CertResponse getEnrollKeysResponse() {
        String enrollKeyResponse = "{\"keyType\":{\"selectedId\":57,\"items\":[{\"id\":22598,\"displayName\":\"RSA " +
                "2048\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":57,\"linkId\":2,\"linkType\":\"key\"}]}}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(enrollKeyResponse);
        response.setSuccess(true);

        return response;
    }

    private CertResponse getEnrollCAResponse() {

        String getEnrollCAResponse = "{\"ca\":{\"selectedId\":40,\"items\":[{\"id\":46,\"displayName\":\"T-Mobile Issuing CA 01 - SHA2\"," +
                "\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":40,\"linkId\":4,\"linkType\":\"CA\"," +
                "\"hasTemplates\":true}]}}";

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(getEnrollCAResponse);
        response.setSuccess(true);

        return response;
    }


    private CertResponse getEnrollTemplateResponse() {

        String getEnrollCAResponse = "{\"template\":{\"selectedId\":46,\"items\":[{\"id\":49," +
                "\"displayName\":\"BarnacleDomainControllerAuthenticationNCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":44,\"linkId\":15,\"linkType\":\"TEMPLATE\"},{\"id\":50,\"displayName\":" +
                "\"T-Mobile USA Mutual Web Authentication2 NCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":51,\"linkId\":18,\"linkType\":\"TEMPLATE\"},{\"id\":52,\"displayName\":" +
                "\"T-MobileUSAConcentratorIPSec(Offlinerequest)NCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":49,\"linkId\":16," +
                "\"linkType\":\"TEMPLATE\"},{\"id\":51,\"displayName\":\"T-MobileUSASimpleClientAuthNCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":54,\"linkId\":19,\"linkType\":\"TEMPLATE\"},{\"id\":53,\"displayName\":\"T-MobileUSAWebServerOfflineNCLM\",\"availableInSubs\":true,\"allowed\":true,\"policyLinkId\":46," +
                "\"linkId\":26,\"linkType\":\"TEMPLATE\"}]}}";

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(getEnrollCAResponse);
        response.setSuccess(true);

        return response;
    }


    private CertManagerLoginRequest getCertManagerLoginRequest() {
        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest();
        certManagerLoginRequest.setPassword("password");
        certManagerLoginRequest.setUsername("username");
        return certManagerLoginRequest;
    }

    private SSLCertificateRequest getSSLCertificateRequest() {
        SSLCertificateRequest sSLCertificateRequest = new SSLCertificateRequest();
        TargetSystem targetSystem = new TargetSystem();
        targetSystem.setAddress("TargetSystemaddress");
        targetSystem.setDescription("TargetSystemDescription");
        targetSystem.setName("TargetName");

        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setHostname("TargetSystemServiceHostname");
        targetSystemServiceRequest.setName("Target System Service Name");
        targetSystemServiceRequest.setPort(443);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(false);
        targetSystemServiceRequest.setMonitoringEnabled(false);
        targetSystemServiceRequest.setDescription("TargetServiceDescription");
        targetSystemServiceRequest.setMonitoringEnabled(true);
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(true);

        sSLCertificateRequest.setCertificateName("certificatename.t-mobile.com");
        sSLCertificateRequest.setAppName("xyz");
        sSLCertificateRequest.setCertOwnerEmailId("testing@mail.com");
        sSLCertificateRequest.setCertOwnerNtid("testuser2");
        sSLCertificateRequest.setCertType("internal");
        sSLCertificateRequest.setTargetSystem(targetSystem);
        sSLCertificateRequest.setTargetSystemServiceRequest(targetSystemServiceRequest);
        return sSLCertificateRequest;
    }

    @Test
    public void get_sslCertificateMetadataDetails(){
        SSLCertificateMetadataDetails ssCertificateMetadataDetails = new SSLCertificateMetadataDetails();
        ssCertificateMetadataDetails.setContainerName("containername");
        ssCertificateMetadataDetails.setCertificateStatus("active");
        ssCertificateMetadataDetails.setCertType("internal");
        ssCertificateMetadataDetails.setApplicationName("abc");
        ssCertificateMetadataDetails.setCertOwnerEmailId("owneremail@test.com");
        ssCertificateMetadataDetails.setApplicationTag("tag");
        ssCertificateMetadataDetails.setProjectLeadEmailId("project@email.com");
        ssCertificateMetadataDetails.setAkmid("12345");
        ssCertificateMetadataDetails.setCertCreatedBy("rob");
        ssCertificateMetadataDetails.setAuthority("authority");
        ssCertificateMetadataDetails.setCreateDate("10-20-2020");
        ssCertificateMetadataDetails.setCertificateName("testcert.com");
        ssCertificateMetadataDetails.setCertificateId(111);
        ssCertificateMetadataDetails.setExpiryDate("10-20-2030");
        ssCertificateMetadataDetails.setApplicationOwnerEmailId("abcdef@mail.com");
        assertNotNull(ssCertificateMetadataDetails);
    }

    @Test
    public void getRevocationReasons_Success() throws Exception {
    	Integer certficateId = 123;
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);
        String jsonStr2 ="{\"time_enabled\":false,\"details_enabled\":false,\"reasons\":[{\"reason\":\"unspecified\",\"displayName\":\"Unspecified\"},{\"reason\":\"keyCompromise\",\"displayName\":\"Key compromise\"},{\"reason\":\"cACompromise\",\"displayName\":\"CA compromise\"},{\"reason\":\"affiliationChanged\",\"displayName\":\"Affiliation changed\"},{\"reason\":\"superseded\",\"displayName\":\"Superseded\"},{\"reason\":\"cessationOfOperation\",\"displayName\":\"Cessation of operation\"},{\"reason\":\"certificateHold\",\"displayName\":\"Certificate hold\"}]}";
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(jsonStr2);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates​/revocationreasons"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        ResponseEntity<?> enrollResponse =
                sSLCertificateService.getRevocationReasons(certficateId, token);

        //Assert
        assertNotNull(enrollResponse);
        assertEquals(HttpStatus.OK, enrollResponse.getStatusCode());
    }

    @Test
    public void getRevocationReasons_Failure() throws Exception {
    	Integer certficateId = 123;
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(response);
        String errorJson ="{\"errors\":[\"Forbidden\"]}";
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.FORBIDDEN);
        revocationResponse.setResponse(errorJson);
        revocationResponse.setSuccess(false);
        when(reqProcessor.processCert(eq("/certificates​/revocationreasons"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.getRevocationReasons(certficateId, token);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.FORBIDDEN, revocResponse.getStatusCode());
    }

    @Test
    public void issueRevocationRequest_Success() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(true);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);



        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.OK, revocResponse.getStatusCode());
    }

    @Test
    public void issueRevocationRequest_Non_Admin_Success() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);
         String[] policies = {"o_cert_testCert@t-mobile.com"};
         userDetails.setPolicies(policies);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerNtid\":\"normaluser\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);



        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.OK, revocResponse.getStatusCode());
    }

    @Test
    public void issueRevocationRequest_Admin_Failure() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";

    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJsonError = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.BAD_REQUEST);
        response.setResponse(metaDataJsonError);
        response.setSuccess(false);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);



        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);
        CertResponse revocationResponse = new CertResponse();
        revocationResponse.setHttpstatus(HttpStatus.OK);
        revocationResponse.setResponse(null);
        revocationResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/revocationrequest"), anyObject(), anyString(), anyString())).thenReturn(revocationResponse);

        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.BAD_REQUEST, revocResponse.getStatusCode());
    }

    @Test
    public void testAddUserToCertificateSuccessfully() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        try {
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("r_cert_certificatename.t-mobile.com");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, userDetail, false);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailureAllCerts() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certtest250630.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certtest250630.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response response_404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        try {
            when(ControllerUtil.getPoliciesAsStringFromJson(any(), any())).thenReturn("default,r_cert_certtest250630.t-mobile.com");
        } catch (IOException e) {
            e.printStackTrace();
        }

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return response_404;

                return response_404;
            }
        });

        when(certificateUtils.getCertificateMetaData(token, "certtest250630.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, userDetail, false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailure() throws IOException {
        CertificateUser certUser = new CertificateUser("testuser2","write", "certtest250630.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");

        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_cert_certtest250630.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("w_cert_certtest250630.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(responseNotFound);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(responseNotFound);

        when(certificateUtils.getCertificateMetaData(token, "certtest250630.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, userDetail, false);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailureBadrequest() {
        CertificateUser certUser = new CertificateUser("testuser1","write", "CertificateName");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        userDetails.setUsername("testuser1");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        when(certificateUtils.getCertificateMetaData(token, "CertificateName", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, null, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateForNonAdminFailed() throws IOException {
        CertificateUser certUser = new CertificateUser("testuser2","deny", "certificatename.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        certificateMetadata.setCertOwnerNtid("testuser2");
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate owner cannot be added as a user to the certificate owned by him\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("d_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, userDetail, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailedIfEmptyUserDetails() {
        CertificateUser certUser = new CertificateUser("testuser1", "read", "certificatename.t-mobile.com");
        userDetails = null;
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, null, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testAddUserToCertificateFailedForNotAuthorizedUser() {
        CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = null;
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(false);

        ResponseEntity<String> responseEntity = sSLCertificateService.addUserToCertificate(token, certUser, userDetail, false);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = Exception.class)
    public void testAddUserToCertificatePolicyDataFailed() {
        CertificateUser certUser = new CertificateUser("testuser2","deny", "certificatename.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"key\":[\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"]}");

        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        sSLCertificateService.addUserToCertificate(token, certUser, userDetail, false);
    }

    @Test
    public void test_getgetTargetSystemList_success()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetSystems\": [ {" +
                "  \"name\" : \"abc.com\"," +
                "  \"description\" : \"\"," +
                "  \"address\" : \"abc.com\"," +
                "  \"targetSystemID\" : \"234\"" +
                "}, {" +
                "  \"name\" : \"cde.com\"," +
                "  \"description\" : \"cde.com\"," +
                "  \"address\" : \"cde.com\"," +
                "  \"targetSystemID\" : \"123\"" +
                "}]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/findTargetSystem"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);

        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn(jsonStr);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+jsonStr+"}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true));

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getgetTargetSystemList_failed()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetSystems\": [ {" +
                "  \"name\" : \"abc.com\"," +
                "  \"description\" : \"\"," +
                "  \"address\" : \"abc.com\"," +
                "  \"targetSystemID\" : \"234\"" +
                "}, {" +
                "  \"name\" : \"cde.com\"," +
                "  \"description\" : \"cde.com\"," +
                "  \"address\" : \"cde.com\"," +
                "  \"targetSystemID\" : \"123\"" +
                "}]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("{\"errors\":[\"Failed to get Target system list from NCLM\"]}");
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/findTargetSystem"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system list from NCLM\"]}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getgetTargetSystemList_empty()throws Exception{
        String token = "12345";
        String jsonStr = "{\"data\": [ ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/findTargetSystem"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn("[]");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": []}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemList(token, getMockUser(true));

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getTargetSystemServiceList_success()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetsystemservices\": [ {\n" +
                "  \"name\" : \"testservice1\",\n" +
                "  \"description\" : \"\",\n" +
                "  \"targetSystemServiceId\" : \"1234\",\n" +
                "  \"hostname\" : \"testhostname\",\n" +
                "  \"monitoringEnabled\" : false,\n" +
                "  \"multiIpMonitoringEnabled\" : false,\n" +
                "  \"port\" : 22\n" +
                "} ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(true);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/targetsystemservicelist"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);

        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn(jsonStr);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+jsonStr+"}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemServiceList(token, getMockUser(true), "123");

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getTargetSystemServiceList_failed()throws Exception{
        String token = "12345";
        String jsonStr = "{\"targetsystemservices\": [ {\n" +
                "  \"name\" : \"testservice1\",\n" +
                "  \"description\" : \"\",\n" +
                "  \"targetSystemServiceId\" : \"1234\",\n" +
                "  \"hostname\" : \"testhostname\",\n" +
                "  \"monitoringEnabled\" : false,\n" +
                "  \"multiIpMonitoringEnabled\" : false,\n" +
                "  \"port\" : 22\n" +
                "} ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setResponse("{\"errors\":[\"Failed to get Target system service list from NCLM\"]}");
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/targetsystemservicelist"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);


        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system service list from NCLM\"]}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemServiceList(token, getMockUser(true), "123");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
    }

    @Test
    public void test_getTargetSystemServiceList_empty()throws Exception{
        String token = "12345";
        String jsonStr = "{\"data\": [ ]}";
        CertResponse response = new CertResponse();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(jsonStr);
        response.setSuccess(false);
        String jsonStrUser = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        CertResponse responseUser = new CertResponse();
        responseUser.setHttpstatus(HttpStatus.OK);
        responseUser.setResponse(jsonStrUser);
        responseUser.setSuccess(true);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStrUser)).thenReturn(requestMap);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(responseUser);

        when(reqProcessor.processCert(eq( "/certmanager/targetsystemservicelist"), Mockito.anyObject(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(response);
        when(JSONUtil.getJSONasDefaultPrettyPrint(Mockito.any())).thenReturn("[]");

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"data\": []}");
        ResponseEntity<String> responseEntityActual = sSLCertificateService.getTargetSystemServiceList(token, getMockUser(true), "123");

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.getBody(), responseEntityActual.getBody());
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

    SSLCertificateMetadataDetails getSSLCertificateMetadataDetails() {
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("testuser1");
        certDetails.setCertificateName("certificatename.t-mobile.com");
        certDetails.setCertOwnerNtid("testuser1");
        certDetails.setCertOwnerEmailId("owneremail@test.com");
        certDetails.setExpiryDate("10-20-2030");
        return certDetails;
    }

	@Test
    public void testAssociateAppRoleToCertificateSuccssfully() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertificateFailure400() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename", "role1", "read");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertFailureMasterApprole() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "selfservicesupportrole", "read");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertificateFailure() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertificateFailure403() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertMetadataFailure() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

    @Test
    public void testAssociateAppRoleToCertMetadataFailureRevokeFailure() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read","{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        Response configureAppRoleResponse404 = getMockResponse(HttpStatus.NOT_FOUND, true, "");

        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                if (count++ == 1)
                    return configureAppRoleResponse404;

                return configureAppRoleResponse;
            }
        });
        Response updateMetadataResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);

    }

	@Test
    public void testAssociateAppRoleToCertificateSuccssfullyForAdmin() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailedForEmptyUserDetails() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add approle to this certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = null;
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{\"data\": {\"policies\":\"r_cert_certificatename.t-mobile.com\"}}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailedIfNoRoleExists() {

		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure approle as first step\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.NOT_FOUND, true, "{}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test(expected = Exception.class)
    public void testAssociateAppRoleToCertificateFailedIfReadApprole() {

		ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(true);
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "read");

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        Response appRoleResponse = getMockResponse(HttpStatus.OK, true, "{}");
        when(reqProcessor.process("/auth/approle/role/read", "{\"role_name\":\"role1\"}",token)).thenReturn(appRoleResponse);
        Response configureAppRoleResponse = getMockResponse(HttpStatus.OK, true, "");
        when(appRoleService.configureApprole(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(configureAppRoleResponse);
        Response updateMetadataResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.updateMetadata(Mockito.anyMap(),Mockito.anyString())).thenReturn(updateMetadataResponse);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailureIfEmptyInput() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = null;
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

	@Test
    public void testAssociateAppRoleToCertificateFailureIfInvalidAccess() {

        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        userDetails = getMockUser(false);
        CertificateApprole certificateApprole = new CertificateApprole("certificatename.t-mobile.com", "role1", "revoke");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntityActual =  sSLCertificateService.associateApproletoCertificate(certificateApprole, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected, responseEntityActual);
    }

    void mockNclmLogin() throws Exception {
        String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), any(), any(), any())).thenReturn(certResponse);
    }

    @Test
    public void test_downloadCertificateWithPrivateKey_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};

        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        certDetails.setCertificateId(123123);
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pem\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_success_pkcs12pem() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pkcs12pem", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pfx\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_success_default() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "default", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pfx\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_failure() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(null);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithPrivateKey_post_failure() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pkcs12der", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }


    @Test
    public void test_downloadCertificateWithPrivateKey_failure_httpClient() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pkcs12der", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenThrow(new IOException());

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        InputStreamResource resource = null;
        ResponseEntity<InputStreamResource> responseEntityExpected =
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificates_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);


        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false);

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname.pem\"")
                .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8")).body(resource);

        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails userDetails = getMockUser(false);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificates_failed_403() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();
        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false);

        String policyList [] = {};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.FORBIDDEN, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificates_failed_invalid_token() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
        certResponse.setResponse(null);
        certResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), any(), any(), any())).thenReturn(certResponse);

        CertificateDownloadRequest certificateDownloadRequest = new CertificateDownloadRequest(
                "certname", "password", "pembundle", false);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_success() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname\"")
                .contentType(MediaType.parseMediaType("application/x-pem-file;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem");
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_success_der() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(mockHttpEntity);
        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.OK)
                .contentLength(10).header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certname\"")
                .contentType(MediaType.parseMediaType("application/pkix-cert;charset=utf-8")).body(resource);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "der");
        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_failed() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(400);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        String responseString = "teststreamdata";
        when(EntityUtils.toString(mockHttpEntity, "UTF-8")).thenReturn(responseString);

        byte[] decodedBytes = Base64.getDecoder().decode(responseString);
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }


    @Test
    public void test_downloadCertificateWithoutPrivateKey_failed_entity_null() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(null);

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificateWithoutPrivateKey_failed_httpClient() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        when(HttpClientBuilder.create()).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setSSLContext(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.setRedirectStrategy(Mockito.any())).thenReturn(httpClientBuilder);
        when(httpClientBuilder.build()).thenReturn(httpClient1);
        when(httpClient1.execute(Mockito.any())).thenThrow(new IOException());

        String policyList [] = {"r_cert_certname"};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificate_failed_403() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        mockNclmLogin();

        String policyList [] = {};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser1");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(false), "certname", "pem");
        assertEquals(HttpStatus.FORBIDDEN, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void test_downloadCertificate_failed_invalid_token() throws Exception {

        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setSuccess(true);
        response.setResponse(null);
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
        certResponse.setResponse(null);
        certResponse.setSuccess(true);

        when(reqProcessor.processCert(eq("/auth/certmanager/login"), any(), any(), any())).thenReturn(certResponse);

        String policyList [] = {};
        VaultTokenLookupDetails lookupDetails = null;
        lookupDetails = new VaultTokenLookupDetails();
        lookupDetails.setUsername("normaluser");
        lookupDetails.setPolicies(policyList);
        lookupDetails.setToken(token);
        lookupDetails.setValid(true);
        lookupDetails.setAdmin(true);

        when(tokenValidator.getVaultTokenLookupDetails(Mockito.any())).thenReturn(lookupDetails);
        SSLCertificateMetadataDetails certDetails = new SSLCertificateMetadataDetails();
        certDetails.setCertType("internal");
        certDetails.setCertCreatedBy("normaluser");
        certDetails.setCertificateName("CertificateName");
        certDetails.setCertOwnerNtid("normaluser1");
        certDetails.setCertOwnerEmailId("normaluser@test.com");
        certDetails.setExpiryDate("10-20-2030");
        when(certificateUtils.getCertificateMetaData(Mockito.any(), eq("certname"), anyString())).thenReturn(certDetails);

        ResponseEntity<InputStreamResource> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(null);

        ResponseEntity<InputStreamResource> responseEntityActual =
                sSLCertificateService.downloadCertificate(token, getMockUser(true), "certname", "pem");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntityActual.getStatusCode());
        assertEquals(responseEntityExpected.toString(),responseEntityActual.toString());

    }

    @Test
    public void renewCertificate_Success() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(true);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);


        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> renewCertResponse =
                sSLCertificateService.renewCertificate(certficateName, userDetails, token);

        //Assert
        assertNotNull(renewCertResponse);
    }

    @Test
    public void renewCertificate_Non_Admin_Success() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJson = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.OK);
        response.setResponse(metaDataJson);
        response.setSuccess(true);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);

        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);


        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> renewCertResponse =
                sSLCertificateService.renewCertificate(certficateName, userDetails, token);

        //Assert
        assertNotNull(renewCertResponse);
    }

    @Test
    public void renewCertificate_Admin_Failure() throws Exception {
    	String certficateName = "testCert@t-mobile.com";
    	String token = "FSR&&%S*";
    	String jsonStr = "{  \"username\": \"testusername1\",  \"password\": \"testpassword1\"}";
    	String jsonStr2 = "{\"certificates\":[{\"sortedSubjectName\": \"CN=CertificateName.t-mobile.com, C=US, " +
                "ST=Washington, " +
                "L=Bellevue, O=T-Mobile USA, Inc\"," +
                "\"certificateId\":57258,\"certificateStatus\":\"Active\"," +
                "\"containerName\":\"cont_12345\",\"NotAfter\":\"2021-06-15T04:35:58-07:00\"}]}";

    	RevocationRequest revocationRequest = new RevocationRequest();
    	revocationRequest.setReason("unspecified");

    	 UserDetails userDetails = new UserDetails();
         userDetails.setSelfSupportToken("tokentTest");
         userDetails.setUsername("normaluser");
         userDetails.setAdmin(false);
         userDetails.setClientToken(token);
         userDetails.setSelfSupportToken(token);

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("access_token", "12345");
        requestMap.put("token_type", "type");
        when(ControllerUtil.parseJson(jsonStr)).thenReturn(requestMap);

        CertManagerLogin certManagerLogin = new CertManagerLogin();
        certManagerLogin.setToken_type("token type");
        certManagerLogin.setAccess_token("1234");
        String metaDataJsonError = "{\"data\":{\"akmid\":\"102463\",\"applicationName\":\"tvs\",\"applicationOwnerEmailId\":\"SpectrumClearingTools@T-Mobile.com\",\"applicationTag\":\"TVS\",\"authority\":\"T-Mobile Issuing CA 01 - SHA2\",\"certCreatedBy\":\"nnazeer1\",\"certOwnerEmailId\":\"ltest@smail.com\",\"certType\":\"internal\",\"certificateId\":59880,\"certificateName\":\"certtest260630.t-mobile.com\",\"certificateStatus\":\"Revoked\",\"containerName\":\"VenafiBin_12345\",\"createDate\":\"2020-06-26T05:10:41-07:00\",\"expiryDate\":\"2021-06-26T05:10:41-07:00\",\"projectLeadEmailId\":\"Daniel.Urrutia@T-Mobile.Com\",\"users\":{\"normaluser\":\"write\",\"certuser\":\"read\",\"safeadmin\":\"deny\",\"testsafeuser\":\"write\",\"testuser1\":\"deny\",\"testuser2\":\"read\"}}}";
        Response response = new Response();
        response.setHttpstatus(HttpStatus.BAD_REQUEST);
        response.setResponse(metaDataJsonError);
        response.setSuccess(false);

        when(reqProcessor.process(eq("/read"), anyObject(), anyString())).thenReturn(response);



        CertResponse certResponse = new CertResponse();
        certResponse.setHttpstatus(HttpStatus.OK);
        certResponse.setResponse(jsonStr);
        certResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/auth/certmanager/login"), anyObject(), anyString(), anyString())).thenReturn(certResponse);

        CertResponse renewResponse = new CertResponse();
        renewResponse.setHttpstatus(HttpStatus.OK);
        renewResponse.setResponse(null);
        renewResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certificates/renew"), anyObject(), anyString(), anyString())).thenReturn(renewResponse);

        CertResponse findCertResponse = new CertResponse();
        findCertResponse.setHttpstatus(HttpStatus.OK);
        findCertResponse.setResponse(jsonStr2);
        findCertResponse.setSuccess(true);
        when(reqProcessor.processCert(eq("/certmanager/findCertificate"), anyObject(), anyString(), anyString())).thenReturn(findCertResponse);

        when(ControllerUtil.updateMetaData(anyString(), anyMap(), anyString())).thenReturn(Boolean.TRUE);

        ResponseEntity<?> revocResponse =
                sSLCertificateService.issueRevocationRequest(certficateName, userDetails, token, revocationRequest);

        //Assert
        assertNotNull(revocResponse);
        assertEquals(HttpStatus.BAD_REQUEST, revocResponse.getStatusCode());
    }

    @Test
    public void testRemoveUserFromCertificateForLdapAuthSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String expectedResponse = "{\"messages\":[\"Successfully removed user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateUserpassAuthSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	CertificateUser certUser = new CertificateUser("testuser2","write", "certificatename.t-mobile.com");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"w_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(reqProcessor.process("/auth/userpass/read","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("w_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(ControllerUtil.configureUserpassUser(eq("testuser2"),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNoContent);
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
    	String expectedResponse = "{\"messages\":[\"Successfully removed user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateFailureIfNotauthorized() {
    	SSLCertificateMetadataDetails certificateMetadata = null;
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
    	CertificateUser certUser = new CertificateUser("testuser1","write", "certificatename.t-mobile.com");
        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(false);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateFailure400() {
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	CertificateUser certUser = new CertificateUser("testuser1", "deny", "certificatename");
        String expectedResponse = "{\"errors\":[\"Invalid input values\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateFailureIfNotvalidUser() {
        UserDetails userDetail = null;
    	CertificateUser certUser = new CertificateUser("testuser1","write", "certificatename.t-mobile.com");
        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = Exception.class)
    public void testRemoveUserFromCertificatePolicyDataFailed() {
        CertificateUser certUser = new CertificateUser("testuser2", "deny", "certificatename.t-mobile.com");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"key\":[\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"]}");

        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}",token)).thenReturn(userResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com", "internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
    }

    @Test
    public void testRemoveUserFromCertificateConfigureLdapUserFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Failed to remvoe the user from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveUserFromCertificateUpdateMetadataFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateUser certUser = new CertificateUser("testuser2","read", "certificatename.t-mobile.com");
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
    	Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Failed to remove the user from the certificate. Metadata update failed\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/users","{\"username\":\"testuser2\"}", token)).thenReturn(userResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
        when(ControllerUtil.configureLDAPUser(eq("testuser2"),any(),any(),eq(token))).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeUserFromCertificate(certUser, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateSuccess() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read");

    	Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        String expectedResponse = "{\"messages\":[\"Group is successfully removed from certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body(expectedResponse);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);

        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNoContent);
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateFailureIfNotauthorized() {
    	SSLCertificateMetadataDetails certificateMetadata = null;
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read");

        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove groups from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(false);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateFailure400() {
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename", "testgroup","read");
        String expectedResponse = "{\"errors\":[\"Invalid input values\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateFailureIfNotvalidUser() {
        UserDetails userDetail = null;
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read");
        String expectedResponse = "{\"errors\":[\"Access denied: No permission to remove group from this certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(expectedResponse);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test(expected = Exception.class)
    public void testRemoveGroupFromCertificatePolicyDataFailed() {
    	ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","deny");
        SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(false);
        userDetail.setUsername("testuser1");

        Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"key\":[\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"d_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"]}");

        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
    }

    @Test
    public void testRemoveGroupFromCertificateConfigureLdapGroupFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read");
    	Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Failed to remove the group from the certificate\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    @Test
    public void testRemoveGroupFromCertificateUpdateMetadataFailed() throws IOException {
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
        UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        ReflectionTestUtils.setField(sSLCertificateService,"vaultAuthMethod", "ldap");
    	CertificateGroup certGroup = new CertificateGroup("certificatename.t-mobile.com", "testgroup","read");
    	Response groupResp = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response idapConfigureResponse = getMockResponse(HttpStatus.NO_CONTENT, true, "{\"policies\":null}");
    	Response responseNotFound = getMockResponse(HttpStatus.NOT_FOUND, false, "");
        String expectedResponse = "{\"errors\":[\"Group configuration failed. Please try again\"]}";
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(expectedResponse);

        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);

        List<String> resList = new ArrayList<>();
        resList.add("default");
        resList.add("r_cert_certificatename.t-mobile.com");
        when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);

        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"testgroup\"}", token)).thenReturn(groupResp);
        when(ControllerUtil.configureLDAPGroup(any(),any(),any())).thenReturn(idapConfigureResponse);
        when(ControllerUtil.updateMetadata(any(),any())).thenReturn(responseNotFound);

        ResponseEntity<String> responseEntity = sSLCertificateService.removeGroupFromCertificate(certGroup, userDetail);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertEquals(responseEntityExpected, responseEntity);
    }

    
    @Test
    public void test_addGroupToCertificate_success()  {
    	String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
    	String policies="r_cert_certmsivadasample.t-mobile.com";
    	CertificateGroup certificateGroup = new CertificateGroup("certmsivadasample.t-mobile.com","r_safe_w_vault_demo","read");
    	UserDetails userDetails = getMockUser(false);
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
    	Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
    	Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");
        when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"r_safe_w_vault_demo\"}",token)).thenReturn(userResponse);
        try {     
            List<String> resList = new ArrayList<>();
            String groupName="r_safe_w_vault_demo";
            when(ControllerUtil.getPoliciesAsListFromJson(obj,policies)).thenReturn(resList);
            when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
            when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
            when(ControllerUtil.configureLDAPGroup(groupName, policies, token)).thenReturn(responseNoContent);
            when(reqProcessor.process("/auth/ldap/groups/configure",policies, token)).thenReturn(userResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.updateSslCertificateMetadata(any(),eq(token))).thenReturn(responseNoContent);
    	ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate( token, certificateGroup);
    	
    	assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
    
    @Test
    public void test_addGroupToCertificate_Badrequest() {
    	CertificateGroup certGroup = new CertificateGroup("certmsivadasample.t-mobile.com","r_safe_w_vault_demo","read");
    	SSLCertificateMetadataDetails certificateMetadata = getSSLCertificateMetadataDetails();
    	UserDetails userDetail = getMockUser(true);
        userDetail.setUsername("testuser1");
        Response userResponse = getMockResponse(HttpStatus.OK, true, "{\"data\":{\"bound_cidrs\":[],\"max_ttl\":0,\"policies\":[\"default\",\"r_cert_certificatename.t-mobile.com\"],\"ttl\":0,\"groups\":\"admin\"}}");
        Response responseNoContent = getMockResponse(HttpStatus.NO_CONTENT, true, "");
        when(certificateUtils.getCertificateMetaData(token, "certificatename.t-mobile.com","internal")).thenReturn(certificateMetadata);
        
        
        
        when(ControllerUtil.arecertificateGroupInputsValid(certGroup)).thenReturn(true);
        when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        when(reqProcessor.process("/auth/ldap/groups","{\"groupname\":\"r_vault_demo\"}",token)).thenReturn(userResponse);
        try {           
            List<String> resList = new ArrayList<>();
            resList.add("default");
            resList.add("r_cert_certificatename.t-mobile.com");
            when(ControllerUtil.getPoliciesAsListFromJson(any(), any())).thenReturn(resList);
            when(ControllerUtil.arecertificateGroupInputsValid(certGroup)).thenReturn(true);
            when(ControllerUtil.canAddCertPermission(any(), any(), eq(token))).thenReturn(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        when(ControllerUtil.updateMetadata(any(),eq(token))).thenReturn(responseNoContent);
        when(certificateUtils.hasAddOrRemovePermission(userDetail, certificateMetadata)).thenReturn(true);
        
        ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate(token, certGroup);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        
    } 

    
    @Test
    public void test_addGroupToCertificate_isAdmin() {
        String token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        String certificateName = "certsample.t-mobile.com";
        CertificateGroup certificateGroup = new CertificateGroup();
        certificateGroup.setAccess("read");
        certificateGroup.setCertificateName("certsample.t-mobile.com");
        certificateGroup.setGroupname("group1");
        System.out.println("certgroup is :"+certificateGroup);
        UserDetails userDetails = getMockUser(false);
        ResponseEntity<String> responseEntityExpected = ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
        when(ControllerUtil.arecertificateGroupInputsValid(certificateGroup)).thenReturn(true);
        ReflectionTestUtils.setField(sSLCertificateService, "vaultAuthMethod", "ldap");

        ResponseEntity<String> responseEntity = sSLCertificateService.addingGroupToCertificate( token, certificateGroup);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
    
    @Test
    public void getListOfCertificates_Succes()throws Exception{
    	 String token = "12345";

         Response response =getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
          		+ "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
          		+ "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
          		+ " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
          		+ " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
          		+ "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
          		+ "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
          		+ "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
         Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

         token = "5PDrOhsy4ig8L3EpsJZSLAMg";
         UserDetails user1 = new UserDetails();
         user1.setUsername("normaluser");
         user1.setAdmin(true);
         user1.setClientToken(token);
         user1.setSelfSupportToken(token);
         String certificateType = "internal";

         when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

         when(reqProcessor.process("/sslcert", "{\"path\":\"/sslcerts/CertificateName.t-mobile.com\"}",token)).thenReturn(response);

         ResponseEntity<String> responseEntityActual = sSLCertificateService.getListOfCertificates(token, certificateType, user1);

         assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
    }

    @Test
    public void getAllCertificates_Success()throws Exception{
        String token = "12345";

        Response response =getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
                + "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
                + "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
                + " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
                + " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
                + "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
                + "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
                + "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
        Response certResponse =getMockResponse(HttpStatus.OK, true, "{  \"data\": {  \"keys\": [    \"CertificateName.t-mobile.com\"    ]  }}");

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails user1 = new UserDetails();
        user1.setUsername("normaluser");
        user1.setAdmin(true);
        user1.setClientToken(token);
        user1.setSelfSupportToken(token);
        String certificateType = "internal";

        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

        when(reqProcessor.process("/sslcert", "{\"path\":\"" + SSLCertificateConstants.SSL_CERT_PATH + "\"}", token)).thenReturn(response);

        ResponseEntity<String> responseEntityActual = sSLCertificateService.getAllCertificates(token, "", certificateType, 1, 0);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
    }

    @Test
    public void getAllCertificates_failed()throws Exception{
        String token = "12345";

        Response response =getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
                + "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
                + "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
                + " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
                + " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
                + "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
                + "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
                + "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
        Response certResponse =getMockResponse(HttpStatus.NOT_FOUND, true, "");

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails user1 = new UserDetails();
        user1.setUsername("normaluser");
        user1.setAdmin(true);
        user1.setClientToken(token);
        user1.setSelfSupportToken(token);
        String certificateType = "internal";

        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

        when(reqProcessor.process("/sslcert", "{\"path\":\"" + SSLCertificateConstants.SSL_CERT_PATH + "\"}", token)).thenReturn(response);

        ResponseEntity<String> responseEntityActual = sSLCertificateService.getAllCertificates(token, "", certificateType, 1, 0);

        assertEquals(HttpStatus.OK, responseEntityActual.getStatusCode());
    }

    @Test
    public void getAllCertificates_failed_500()throws Exception{
        String token = "12345";

        Response response =getMockResponse(HttpStatus.OK, true, "{  \"keys\": [    {      \"akamid\": \"102463\",      \"applicationName\": \"tvs\", "
                + "     \"applicationOwnerEmailId\": \"abcdef@mail.com\",      \"applicationTag\": \"TVS\",  "
                + "    \"authority\": \"T-Mobile Issuing CA 01 - SHA2\",      \"certCreatedBy\": \"rob\",     "
                + " \"certOwnerEmailId\": \"ntest@gmail.com\",      \"certType\": \"internal\",     "
                + " \"certificateId\": 59480,      \"certificateName\": \"CertificateName.t-mobile.com\",   "
                + "   \"certificateStatus\": \"Active\",      \"containerName\": \"VenafiBin_12345\",    "
                + "  \"createDate\": \"2020-06-24T03:16:29-07:00\",      \"expiryDate\": \"2021-06-24T03:16:29-07:00\",  "
                + "    \"projectLeadEmailId\": \"project@email.com\"    }  ]}");
        Response certResponse =getMockResponse(HttpStatus.INTERNAL_SERVER_ERROR, true, "");

        token = "5PDrOhsy4ig8L3EpsJZSLAMg";
        UserDetails user1 = new UserDetails();
        user1.setUsername("normaluser");
        user1.setAdmin(true);
        user1.setClientToken(token);
        user1.setSelfSupportToken(token);
        String certificateType = "internal";

        when(reqProcessor.process(Mockito.eq("/sslcert"),Mockito.anyString(),Mockito.eq(token))).thenReturn(certResponse);

        when(reqProcessor.process("/sslcert", "{\"path\":\"" + SSLCertificateConstants.SSL_CERT_PATH + "\"}", token)).thenReturn(response);

        ResponseEntity<String> responseEntityActual = sSLCertificateService.getAllCertificates(token, "", certificateType, 1, 0);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntityActual.getStatusCode());
    }

}
