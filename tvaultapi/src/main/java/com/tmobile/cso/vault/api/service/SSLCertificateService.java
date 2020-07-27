// =========================================================================
// Copyright 2020 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.tmobile.cso.vault.api.utils.PolicyUtils;
import com.tmobile.cso.vault.api.validator.TokenValidator;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.util.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tmobile.cso.vault.api.common.SSLCertificateConstants;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.AuthorizationUtils;
import com.tmobile.cso.vault.api.utils.CertificateUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SSLCertificateService {

    @Value("${vault.port}")
    private String vaultPort;

    @Autowired
    private RequestProcessor reqProcessor;

    @Autowired
    private WorkloadDetailsService workloadDetailsService;
    
    @Autowired
	private CertificateUtils certificateUtils;

    @Autowired
   	private PolicyUtils policyUtils;

    @Autowired
   	private AuthorizationUtils authorizationUtils;

    @Autowired
	private AppRoleService appRoleService;

    @Autowired
    private TokenValidator tokenValidator;

    @Autowired
	private AccessService accessService;


    @Value("${vault.auth.method}")
    private String vaultAuthMethod;

    @Value("${sslcertmanager.domain}")
    private String certManagerDomain;

    @Value("${sslcertmanager.endpoint.token_generator}")
    private String tokenGenerator;

    @Value("${sslcertmanager.endpoint.target_system_groups}")
    private String targetSystemGroups;

    @Value("${sslcertmanager.endpoint.certificate}")
    private String certificateEndpoint;

    @Value("${sslcertmanager.endpoint.targetsystems}")
    private String targetSystems;

    @Value("${sslcertmanager.endpoint.targetsystemservices}")
    private String targetSystemServies;

    @Value("${sslcertmanager.endpoint.enroll}")
    private String enrollUrl;

    @Value("${sslcertmanager.endpoint.enrollCA}")
    private String enrollCAUrl;

    @Value("${sslcertmanager.endpoint.enrollTemplateUrl}")
    private String enrollTemplateUrl;
    @Value("${sslcertmanager.endpoint.enrollKeysUrl}")
    private String enrollKeysUrl;

    @Value("${sslcertmanager.endpoint.enrollCSRUrl}")
    private String enrollCSRUrl;

    @Value("${sslcertmanager.endpoint.findTargetSystem}")
    private String findTargetSystem;

    @Value("${sslcertmanager.endpoint.findTargetSystemService}")
    private String findTargetSystemService;

    @Value("${sslcertmanager.endpoint.enrollUpdateCSRUrl}")
    private String enrollUpdateCSRUrl;

    @Value("${sslcertmanager.endpoint.findCertificate}")
    private String findCertificate;

    @Value("${sslcertmanager.username}")
    private String certManagerUsername;

    @Value("${sslcertmanager.password}")
    private String certManagerPassword;

    @Value("${sslcertmanager.targetsystemgroup.private_single_san.ts_gp_id}")
    private int private_single_san_ts_gp_id;

    @Value("${sslcertmanager.targetsystemgroup.private_multi_san.ts_gp_id}")
    private int private_multi_san_ts_gp_id;

    @Value("${sslcertmanager.targetsystemgroup.public_single_san.ts_gp_id}")
    private int public_single_san_ts_gp_id;

    @Value("${sslcertmanager.targetsystemgroup.public_multi_san.ts_gp_id}")
    private int public_multi_san_ts_gp_id;

	@Value("${sslcertmanager.endpoint.getCertifcateReasons}")
	private String getCertifcateReasons;

	@Value("${sslcertmanager.endpoint.issueRevocationRequest}")
	private String issueRevocationRequest;
    @Value("${workload.endpoint}")
    private String workloadEndpoint;

    @Value("${workload.endpoint.token}")
    private String cwmEndpointToken;

    @Value("${certificate.retry.count}")
    private int retrycount;

    @Value("${certificate.delay.time.millsec}")
    private int delayTime;    
    
    @Value("${SSLCertificateController.certificatename.text}")
    private String certificateNameTailText;
    
    @Value("${sslcertmanager.endpoint.renewCertificate}")
	private String renewCertificateEndpoint;
    
    @Value("${certificate.renew.delay.time.millsec}")
    private int renewDelayTime;

    private static Logger log = LogManager.getLogger(SSLCertificateService.class);

    private static final String[] PERMISSIONS = {"read", "write", "deny", "sudo"};

    /**
     * Login using CertManager
     *
     * @param certManagerLoginRequest
     * @return
     */
    public ResponseEntity<String> authenticate(CertManagerLoginRequest certManagerLoginRequest) throws Exception {
        String certManagerAPIEndpoint = "/auth/certmanager/login";
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "CertManager Login  with User name").
                put(LogMessage.MESSAGE, String.format("Trying to authenticate with CertManager with user name = [%s]"
                        ,certManagerLoginRequest.getUsername())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(certManagerAPIEndpoint, certManagerLoginRequest, "", getCertmanagerEndPoint(tokenGenerator));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication Successful").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body("{\"errors\":[\"CertManager Login Failed.\"]}" + "HTTP STATUSCODE  :" + response.getHttpstatus());
        }
    }

    /**
     * @param certManagerLoginRequest
     * @return
     */
    public CertManagerLogin login(CertManagerLoginRequest certManagerLoginRequest) throws Exception {
        CertManagerLogin certManagerLogin = null;
        String certManagerAPIEndpoint = "/auth/certmanager/login";
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                put(LogMessage.MESSAGE, "Trying to authenticate with CertManager").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(certManagerAPIEndpoint, certManagerLoginRequest, "", getCertmanagerEndPoint(tokenGenerator));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication Successful").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
            if (!MapUtils.isEmpty(responseMap)) {
                certManagerLogin = new CertManagerLogin();
                if (responseMap.get(SSLCertificateConstants.ACCESS_TOKEN) != null) {
                    certManagerLogin.setAccess_token((String) responseMap.get(SSLCertificateConstants.ACCESS_TOKEN));
                }
                if (responseMap.get(SSLCertificateConstants.TOKEN_TYPE) != null) {
                    certManagerLogin.setToken_type((String) responseMap.get(SSLCertificateConstants.TOKEN_TYPE));
                }
            }
            return certManagerLogin;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, SSLCertificateConstants.CUSTOMER_LOGIN).
                    put(LogMessage.MESSAGE, "CertManager Authentication failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return null;
        }
    }


    /**
     * @param sslCertificateRequest
     * @return
     */
    public ResponseEntity<String> generateSSLCertificate(SSLCertificateRequest sslCertificateRequest,
                                                               UserDetails userDetails ,String token) {
        CertResponse enrollResponse = new CertResponse();

        //Validate the input data
        boolean isValidData = validateInputData(sslCertificateRequest);
        if(!isValidData){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        }

        try {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("CERTIFICATE REQUEST [%s]",
                            sslCertificateRequest.toString())).
                    put(LogMessage.APIURL,
                            ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));

           String username = (Objects.nonNull(ControllerUtil.getNclmUsername())) ?
                    (new String(Base64.getDecoder().decode(ControllerUtil.getNclmUsername()))) :
                    (new String(Base64.getDecoder().decode(certManagerUsername)));

            String password = (Objects.nonNull(ControllerUtil.getNclmPassword())) ?
                    (new String(Base64.getDecoder().decode(ControllerUtil.getNclmPassword()))) :
                    (new String(Base64.getDecoder().decode(certManagerPassword)));

            //Step-1 : Authenticate
            CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest(username, password);
            CertManagerLogin certManagerLogin = login(certManagerLoginRequest);

            SSLCertTypeConfig sslCertTypeConfig = prepareSSLConfigObject(sslCertificateRequest);

            CertificateData certificateDetails = getCertificate(sslCertificateRequest, certManagerLogin);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Certificate name =[%s] = isCertificateExist [%s]",
                            sslCertificateRequest.getCertificateName(), certificateDetails)).
                    build()));
            if (Objects.isNull(certificateDetails)) {
                //Step-2 Validate targetSystem
                int targetSystemId = getTargetSystem(sslCertificateRequest, certManagerLogin);

                //Step-3:  CreateTargetSystem
                if (targetSystemId == 0) {
                    targetSystemId  = createTargetSystem(sslCertificateRequest.getTargetSystem(), certManagerLogin, sslCertTypeConfig);
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("createTargetSystem Completed Successfully [%s]", targetSystemId)).
                            build()));
                    if (targetSystemId == 0){
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                        enrollResponse.setSuccess(Boolean.FALSE);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
                    }
                }

                //Step-4 : Validate the Target System Service
                int targetSystemServiceId = getTargetSystemServiceId(sslCertificateRequest, targetSystemId, certManagerLogin);


                //Step-5: Create Target System  Service
                if (targetSystemServiceId == 0) {
                    TargetSystemServiceRequest targetSystemServiceRequest = prepareTargetSystemServiceRequest(sslCertificateRequest);
                    TargetSystemService targetSystemService = createTargetSystemService(targetSystemServiceRequest, targetSystemId, certManagerLogin);

                    if (Objects.nonNull(targetSystemService)) {
                        targetSystemServiceId = targetSystemService.getTargetSystemServiceId();
                    } else {
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                        enrollResponse.setSuccess(Boolean.FALSE);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
                    }
                    log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                            put(LogMessage.ACTION, String.format("createTargetSystem Service  Completed Successfully [%s]", targetSystemService)).
                            put(LogMessage.MESSAGE, String.format("Target System Service ID  [%s]", targetSystemService.getTargetSystemServiceId())).
                            build()));
                }

                //Step-7 - Enroll Configuration
                //getEnrollCA
                CertResponse response = getEnrollCA(certManagerLogin, targetSystemServiceId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("getEnrollCA Completed Successfully [%s]", response.getResponse())).
                        build()));

                //Step-8 PutEnrollCA
                int updatedSelectedId = putEnrollCA(certManagerLogin, targetSystemServiceId, response);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("PutEnroll CA Successfully Completed[%s]", updatedSelectedId)).
                        build()));

                //Step-9  GetEnrollTemplates
                CertResponse templateResponse = getEnrollTemplates(certManagerLogin, targetSystemServiceId, updatedSelectedId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("Get Enrollment template  Completed Successfully [%s]",
                                templateResponse.getResponse())).
                        build()));

                //Step-10  PutEnrollTemplates
                int enrollTemplateId = putEnrollTemplates(certManagerLogin, targetSystemServiceId, templateResponse, updatedSelectedId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("PutEnroll template  Successfully Completed = enrollTemplateId = [%s]", enrollTemplateId)).
                        build()));

                //GetTemplateParameters
                //PutTemplateParameters

                //Step-11  GetEnrollKeys
                CertResponse getEnrollKeyResponse = getEnrollKeys(certManagerLogin, targetSystemServiceId, enrollTemplateId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("getEnrollKeys Completed Successfully [%s]", getEnrollKeyResponse.getResponse())).
                        build()));

                //Step-12  PutEnrollKeys
                int enrollKeyId = putEnrollKeys(certManagerLogin, targetSystemServiceId, getEnrollKeyResponse, enrollTemplateId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("putEnrollKeys  Successfully Completed[%s]", enrollKeyId)).
                        build()));

                //Step-13 GetEnrollCSRs
                String updatedRequest = getEnrollCSR(certManagerLogin, targetSystemServiceId, enrollTemplateId, sslCertificateRequest);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("getEnrollCSRResponse Completed Successfully [%s]", updatedRequest)).
                        build()));

                //Step-14  PutEnrollCSRs
                CertResponse putEnrollCSRResponse = putEnrollCSR(certManagerLogin, targetSystemServiceId, updatedRequest);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("PutEnroll CSR  Successfully Completed = = [%s]", putEnrollCSRResponse)).
                        build()));

                //Step-15: Enroll Process
                enrollResponse = enrollCertificate(certManagerLogin, targetSystemServiceId);
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("Enroll Certificate response Completed Successfully [%s]", enrollResponse.getResponse())).
                        build()));

                //If Certificate creates successfully
				if (HttpStatus.NO_CONTENT.equals(enrollResponse.getHttpstatus())) {
					// Policy Creation
					boolean isPoliciesCreated;

					if (userDetails.isAdmin()) {
						isPoliciesCreated = createPolicies(sslCertificateRequest, token);
					} else {
						isPoliciesCreated = createPolicies(sslCertificateRequest, userDetails.getSelfSupportToken());
					}
                    if(isPoliciesCreated) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Policies are created for SSL certificate [%s]",
                                        sslCertificateRequest.getCertificateName())).
                                build()));
                    }

                    String metadataJson = populateSSLCertificateMetadata(sslCertificateRequest, userDetails,
                            certManagerLogin);

					boolean sslMetaDataCreationStatus;

					if (userDetails.isAdmin()) {
						sslMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson, token);
					} else {
						sslMetaDataCreationStatus = ControllerUtil.createMetadata(metadataJson,
								userDetails.getSelfSupportToken());
					}


                    if (sslMetaDataCreationStatus) {
                        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                                put(LogMessage.ACTION, String.format("Metadata  created for SSL certificate [%s]",
                                        sslCertificateRequest.getCertificateName())).
                                build()));
                    }


                    //Send failed certificate response in case of any issues in Policy/Meta data creation
                    if ((!isPoliciesCreated) || (!sslMetaDataCreationStatus)) {
                        enrollResponse.setResponse(SSLCertificateConstants.SSL_CREATE_EXCEPTION);
                        enrollResponse.setSuccess(Boolean.FALSE);
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION,
										String.format(
												"Metadata or Policies failed for SSL certificate [%s] - metaDataStatus[%s] - policyStatus[%s]",
												sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus,
												isPoliciesCreated))
								.build()));
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
                    } else {
                    	
                    	return addSudoPermissionToCertificateOwner(sslCertificateRequest, userDetails, token,
								enrollResponse, isPoliciesCreated, sslMetaDataCreationStatus);	

                    }
                }
            } else {
                enrollResponse.setSuccess(Boolean.FALSE);
                enrollResponse.setHttpstatus(HttpStatus.BAD_REQUEST);
                enrollResponse.setResponse("Certificate Already Available in  NCLM with Active Status");
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                        put(LogMessage.ACTION, String.format("Certificate Already Available in  NCLM with Active Status " +
                                "[%s]", enrollResponse.toString())).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+enrollResponse.getResponse()+
                        "\"]}");
            }
        } catch (TVaultValidationException tex) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Inside  TVaultValidationException " +
                                    "Exception = [%s] =  Message [%s]", Arrays.toString(tex.getStackTrace()),
                            tex.getMessage())).build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"" + tex.getMessage() + "\"]}");

        } catch (Exception e) {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, String.format("Inside  Exception " +
                                    "Exception = [%s] =  Message [%s]", Arrays.toString(e.getStackTrace()),
                            e.getMessage())).build()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body
                    ("{\"errors\":[\"" + SSLCertificateConstants.SSL_CREATE_EXCEPTION + "\"]}");
        }
        return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\""+SSLCertificateConstants.SSL_CERT_SUCCESS+"\"]}");
    }

	/**
	 * Method to provide sudo permission to certificate owner
	 * 
	 * @param sslCertificateRequest
	 * @param userDetails
	 * @param token
	 * @param enrollResponse
	 * @param isPoliciesCreated
	 * @param sslMetaDataCreationStatus
	 * @return
	 */
	private ResponseEntity<String> addSudoPermissionToCertificateOwner(SSLCertificateRequest sslCertificateRequest,
			UserDetails userDetails, String token, CertResponse enrollResponse, boolean isPoliciesCreated,
			boolean sslMetaDataCreationStatus) {
		CertificateUser certificateUser = new CertificateUser();
		certificateUser.setUsername(sslCertificateRequest.getCertOwnerNtid());
		certificateUser.setAccess(TVaultConstants.SUDO_POLICY);
		certificateUser.setCertificateName(sslCertificateRequest.getCertificateName());
		
		ResponseEntity<String> addUserresponse = addUserToCertificate(token, certificateUser, userDetails, true);
		
		if(HttpStatus.OK.equals(addUserresponse.getStatusCode())){
			certificateUser.setAccess(TVaultConstants.WRITE_POLICY);
			ResponseEntity<String> addReadPolicyResponse = addUserToCertificate(token, certificateUser, userDetails, true);
			if(HttpStatus.OK.equals(addReadPolicyResponse.getStatusCode())){
				enrollResponse.setResponse(SSLCertificateConstants.SSL_CERT_SUCCESS);
				enrollResponse.setSuccess(Boolean.TRUE);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, String.format("Metadata or Policies created for SSL certificate [%s] - metaDataStatus [%s] - policyStatus [%s]", sslCertificateRequest.getCertificateName(), sslMetaDataCreationStatus, isPoliciesCreated))
						.build()));
			    return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\""+enrollResponse.getResponse()+"\"]}");
			}else {
				enrollResponse.setResponse(SSLCertificateConstants.SSL_OWNER_PERMISSION_EXCEPTION);
	            enrollResponse.setSuccess(Boolean.FALSE);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
			            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
			            put(LogMessage.ACTION, "addUserToCertificate").
			            put(LogMessage.MESSAGE, "Adding sudo permission to certificate owner failed").
			            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
			            build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
			}
		}else {
			enrollResponse.setResponse(SSLCertificateConstants.SSL_OWNER_PERMISSION_EXCEPTION);
            enrollResponse.setSuccess(Boolean.FALSE);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		            put(LogMessage.ACTION, "addUserToCertificate").
		            put(LogMessage.MESSAGE, "Adding sudo permission to certificate owner failed").
		            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		            build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\""+enrollResponse.getResponse()+"\"]}");
		}
	}


	/**
	 * Method to populate certificate metadata details
	 * @param sslCertificateRequest
	 * @param userDetails
	 * @param certManagerLogin
	 * @return
	 * @throws Exception
	 */
	private String populateSSLCertificateMetadata(SSLCertificateRequest sslCertificateRequest, UserDetails userDetails,
            CertManagerLogin certManagerLogin) throws Exception {
		String certMetadataPath = null;
    	if(sslCertificateRequest.getCertType().equalsIgnoreCase("internal")) {
    		certMetadataPath = SSLCertificateConstants.SSL_CERT_PATH + '/' + sslCertificateRequest.getCertificateName();
    	}else {
    		certMetadataPath = SSLCertificateConstants.SSL_CERT_PATH_EXT + '/' + sslCertificateRequest.getCertificateName();
    	}
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = new SSLCertificateMetadataDetails();

        //Get Application details
        String applicationName = sslCertificateRequest.getAppName();
        ResponseEntity<String> appResponse = workloadDetailsService.getWorkloadDetailsByAppName(applicationName);
        if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
            JsonParser jsonParser = new JsonParser();
            JsonObject response = (JsonObject) jsonParser.parse(appResponse.getBody());
        JsonObject jsonElement = null;
        if (Objects.nonNull(response)) {
            jsonElement = response.get("spec").getAsJsonObject();
            if (Objects.nonNull(jsonElement)) {
                String applicationTag = validateString(jsonElement.get("tag"));
                String projectLeadEmail = validateString(jsonElement.get("projectLeadEmail"));
                String appOwnerEmail = validateString(jsonElement.get("brtContactEmail"));
                String akmid = validateString(jsonElement.get("akmid"));
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION,"Populate Application details in SSL Certificate Metadata").
                        put(LogMessage.MESSAGE, String.format("Application Details  for an " +
                                        "applicationName = [%s] , applicationTag = [%s], " +
                                        "projectLeadEmail =  [%s],appOwnerEmail =  [%s], akmid = [%s]", applicationName,
                                applicationTag, projectLeadEmail, appOwnerEmail, akmid)).build()));

                sslCertificateMetadataDetails.setAkmid(akmid);
                sslCertificateMetadataDetails.setProjectLeadEmailId(projectLeadEmail);
                sslCertificateMetadataDetails.setApplicationOwnerEmailId(appOwnerEmail);
                sslCertificateMetadataDetails.setApplicationTag(applicationTag);
                sslCertificateMetadataDetails.setApplicationName(applicationName);
            }
            }
        } else {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "Getting Application Details by app name during Meta data creation ").
                    put(LogMessage.MESSAGE, String.format("Application details will not insert/update in metadata  " +
                                    "for an application =  [%s] ",  applicationName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }

        CertificateData certDetails = null;
        //Get Certificate Details
        for (int i = 1; i <= retrycount; i++) {
            Thread.sleep(delayTime);
            certDetails = getCertificate(sslCertificateRequest, certManagerLogin);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "Populate Certificate Details in SSL Certificate MetaData").
                    put(LogMessage.MESSAGE, String.format("Fetching certificate details count = [%s] and status = [%s]"
                            , i, Objects.nonNull(certDetails))).build()));
            if (Objects.nonNull(certDetails)) {
                break;
            }
        }
        if (Objects.nonNull(certDetails)) {
            sslCertificateMetadataDetails.setCertificateId(certDetails.getCertificateId());
            sslCertificateMetadataDetails.setCertificateName(certDetails.getCertificateName());
            sslCertificateMetadataDetails.setCreateDate(certDetails.getCreateDate());
            sslCertificateMetadataDetails.setExpiryDate(certDetails.getExpiryDate());
            sslCertificateMetadataDetails.setAuthority(certDetails.getAuthority());
            sslCertificateMetadataDetails.setCertificateStatus(certDetails.getCertificateStatus());
            sslCertificateMetadataDetails.setContainerName(certDetails.getContainerName());

        } else {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, String.format("Certificate Details to  not available for given " +
                            "certificate = [%s]", sslCertificateRequest.getCertificateName())).
                    build()));
        }
        sslCertificateMetadataDetails.setCertCreatedBy(userDetails.getUsername());
        sslCertificateMetadataDetails.setCertOwnerEmailId(sslCertificateRequest.getCertOwnerEmailId());
        sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
        sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());

        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, String.format("MetaData info details = [%s]", sslCertificateMetadataDetails)).
                build()));


        SSLCertMetadata sslCertMetadata = new SSLCertMetadata(certMetadataPath, sslCertificateMetadataDetails);
        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
        rqstParams.put("path", certMetadataPath);
        return ControllerUtil.convetToJson(rqstParams);
	}

    /**
     * Validate input data
     * @param sslCertificateRequest
     * @return
     */
    private boolean validateInputData(SSLCertificateRequest sslCertificateRequest){
        boolean isValid=true;
        if(sslCertificateRequest.getCertificateName().contains(" ") || sslCertificateRequest.getAppName().contains(" ") ||
                sslCertificateRequest.getCertOwnerEmailId().contains(" ") ||  sslCertificateRequest.getCertType().contains(" ") ||
                (!sslCertificateRequest.getCertificateName().endsWith(certificateNameTailText)) ||
                sslCertificateRequest.getTargetSystem().getAddress().contains(" ") ||
                (sslCertificateRequest.getCertificateName().contains(".-")) ||
                (sslCertificateRequest.getCertificateName().contains("-.")) ||
                (!isValidHostName(sslCertificateRequest.getTargetSystemServiceRequest().getHostname())) || (!isValidAppName(sslCertificateRequest))){
            isValid= false;
        }

        return isValid;
    }
    private boolean isValidAppName(SSLCertificateRequest sslCertificateRequest){
        boolean isValidApp=false;
        ResponseEntity<String> appResponse =
                workloadDetailsService.getWorkloadDetailsByAppName(sslCertificateRequest.getAppName());
        if (HttpStatus.OK.equals(appResponse.getStatusCode())) {
            isValidApp=true;
        }
        return isValidApp;
    }


    /**
     * To Validate the hostname when it's not null/empty
     * @param hostname
     * @return
     */
    private boolean isValidHostName(String hostname){
        if(!StringUtils.isEmpty(hostname)){
            String regex = "^[a-zA-Z0-9.-]+$";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(hostname);
            return m.matches();
        }
        return true;
    }

	/**
     * To create r/w/o/d policies
     * @param sslCertificateRequest
     * @param token
     * @return
     */
    private boolean createPolicies(SSLCertificateRequest sslCertificateRequest, String token) {
        boolean policiesCreated = false;
        Map<String, Object> policyMap = new HashMap<>();
        Map<String, String> accessMap = new HashMap<>();
        String certificateName = sslCertificateRequest.getCertificateName();
        String certPath = null;
        String certMetadataPath = null;
    	if(sslCertificateRequest.getCertType().equalsIgnoreCase("internal")) {
    		certPath = SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateName;
    		certMetadataPath = SSLCertificateConstants.SSL_CERT_PATH + '/' + certificateName;
    	}else {
    		certPath = SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT + certificateName;
    		certMetadataPath = SSLCertificateConstants.SSL_CERT_PATH_EXT + '/' + certificateName;
    	}

    	boolean isCertDataUpdated = certificateMetadataForPoliciesCreation(sslCertificateRequest, token, certPath);

		if(isCertDataUpdated) {

	        //Read Policy
	        accessMap.put(certPath , TVaultConstants.READ_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.READ_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.READ_CERT_POLICY_PREFIX + certificateName);
	        policyMap.put(SSLCertificateConstants.ACCESS_STRING, accessMap);

	        String policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response readResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

	        //Write Policy
	        accessMap.put(certPath , TVaultConstants.WRITE_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+ certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response writeResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

	        //Deny Policy
	        accessMap.put(certPath , TVaultConstants.DENY_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.DENY_CERT_POLICY_PREFIX + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response denyResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

	        //Owner Policy
	        accessMap.put(certPath , TVaultConstants.SUDO_POLICY);
	        accessMap.put(certMetadataPath, TVaultConstants.WRITE_POLICY);
	        policyMap.put(SSLCertificateConstants.ACCESS_ID, SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX + certificateName);
	        policyRequestJson = ControllerUtil.convetToJson(policyMap);
	        Response sudoResponse = reqProcessor.process(SSLCertificateConstants.ACCESS_UPDATE_ENDPOINT, policyRequestJson, token);

	        if ((readResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
	        		writeResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) &&
	        		denyResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
	                &&  sudoResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)
	        ) ||
	                (readResponse.getHttpstatus().equals(HttpStatus.OK) &&
	                		writeResponse.getHttpstatus().equals(HttpStatus.OK) &&
	                		denyResponse.getHttpstatus().equals(HttpStatus.OK))
	              && sudoResponse.getHttpstatus().equals(HttpStatus.OK)
	        ) {
	            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL Certificate Policies Creation Success").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
	            policiesCreated = true;
	        } else {
	            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL Certificate policies creation failed").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
	        }
		}
        return policiesCreated;
    }

	/**
	 * Method to create certificate metadata in sslcerts/externalcerts mount
	 * @param sslCertificateRequest
	 * @param token
	 * @param certPath
	 * @return
	 */
    private boolean certificateMetadataForPoliciesCreation(SSLCertificateRequest sslCertificateRequest, String token,
			String certPath) {
		SSLCertificateMetadataDetails sslCertificateMetadataDetails = new SSLCertificateMetadataDetails();

    	sslCertificateMetadataDetails.setApplicationName(sslCertificateRequest.getAppName());
    	sslCertificateMetadataDetails.setCertificateName(sslCertificateRequest.getCertificateName());
    	sslCertificateMetadataDetails.setCertType(sslCertificateRequest.getCertType());
    	sslCertificateMetadataDetails.setCertOwnerNtid(sslCertificateRequest.getCertOwnerNtid());

    	SSLCertMetadata sslCertMetadata = new SSLCertMetadata(certPath, sslCertificateMetadataDetails);
        String jsonStr = JSONUtil.getJSON(sslCertMetadata);
        Map<String, Object> rqstParams = ControllerUtil.parseJson(jsonStr);
        rqstParams.put("path", certPath);
        String certDataJson = ControllerUtil.convetToJson(rqstParams);

		Response response = reqProcessor.process("/write", certDataJson, token);

		boolean isCertDataUpdated = false;

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			 log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
	                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
	                    put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
	                    put(LogMessage.MESSAGE, "SSL certificate metadata creation success for policy creation").
	                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
	                    build()));
			 isCertDataUpdated = true;
		}else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.POLICY_CREATION_TITLE).
					put(LogMessage.MESSAGE, "SSL certificate metadata creation failed for policy creation").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
		}
		return isCertDataUpdated;
	}

    /**
     * THis method will be responsible to check the whether given certificate exists or not
     * @param sslCertificateRequest
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private CertificateData getCertificate(SSLCertificateRequest sslCertificateRequest, CertManagerLogin certManagerLogin) throws Exception {
        CertificateData certificateData=null;
        String certName = sslCertificateRequest.getCertificateName();
        int containerId = getTargetSystemGroupId(SSLCertType.valueOf("PRIVATE_SINGLE_SAN"));
        String findCertificateEndpoint = "/certmanager/findCertificate";
        String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid", String.valueOf(containerId));
        CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetEndpoint));
        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.CERTIFICATES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    if ((Objects.equals(getCertficateName(jsonElement.get("sortedSubjectName").getAsString()), certName))
                            && jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS).getAsString().
                            equalsIgnoreCase(SSLCertificateConstants.ACTIVE)) {
                        certificateData= new CertificateData();
                        certificateData.setCertificateId(Integer.parseInt(jsonElement.get("certificateId").getAsString()));
                        certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
                        certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));
                        certificateData.setContainerName(validateString(jsonElement.get("containerName")));
                        certificateData.setCertificateStatus(validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
                        certificateData.setCertificateName(certName);
                        certificateData.setAuthority((!StringUtils.isEmpty(jsonElement.get("enrollServiceInfo")) ?
                                 validateString(jsonElement.get("enrollServiceInfo").getAsJsonObject().get("name")) :
                                 null));
                        break;
                    }

                }
            }
        }
        return certificateData;
    }

    private String validateString(JsonElement jsonElement){
        return (!StringUtils.isEmpty(jsonElement) ? (jsonElement.getAsString()):null);
    }

    /**
     * Get Certificate name
     * @param certData
     * @return
     */
    private String getCertficateName(String certData){
        String[] list = certData.split(",");
        for (String str : list) {
            String[] values = str.split("=");
            if (values[0].equalsIgnoreCase("CN"))
                return values[1];
        }
        return null;
    }
    /**
     * To check whether the given certificate already exists
     * @param sslCertificateRequest
     * @param targetSystemId
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private int getTargetSystemServiceId(SSLCertificateRequest sslCertificateRequest, int targetSystemId, CertManagerLogin certManagerLogin) throws Exception {
        int targetSystemServiceID = 0;
        String targetSystemName = sslCertificateRequest.getTargetSystemServiceRequest().getName();
        String getTargetSystemServiceEndpoint = "/certmanager/findTargetSystemService";
        String findTargetSystemServiceEndpoint = findTargetSystemService.replace("tsgid",
                String.valueOf(targetSystemId));
        CertResponse response = reqProcessor.processCert(getTargetSystemServiceEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(findTargetSystemServiceEndpoint));

        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.TARGETSYSTEM_SERVICES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEM_SERVICES);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    if (jsonElement.get(SSLCertificateConstants.NAME).getAsString().equalsIgnoreCase(targetSystemName)) {
                        targetSystemServiceID = jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_SERVICE_ID).getAsInt();
                        break;
                    }

                }
            }
        }
        return targetSystemServiceID;
    }


    /**
     * This method will be responsible for get the id of given Target System  if exists
     * @param sslCertificateRequest
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private int getTargetSystem(SSLCertificateRequest sslCertificateRequest, CertManagerLogin certManagerLogin) throws Exception {
        int targetSystemID = 0;
        String targetSystemName = sslCertificateRequest.getTargetSystem().getName();
        String getTargetSystemEndpoint = "/certmanager/findTargetSystem";
        String findTargetSystemEndpoint = findTargetSystem.replace("tsgid",
                String.valueOf(getTargetSystemGroupId(SSLCertType.valueOf("PRIVATE_SINGLE_SAN"))));
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(findTargetSystemEndpoint));
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
        JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEMS);
        if (Objects.nonNull(jsonArray)) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                if (jsonElement.get(SSLCertificateConstants.NAME).getAsString().equalsIgnoreCase(targetSystemName)) {
                    targetSystemID = jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_ID).getAsInt();
                }
            }
        }
        return targetSystemID;
    }


    //Get Enroll CSR

    /**
     * Get the enroll csr details
     * @param certManagerLogin
     * @param entityid
     * @param templateid
     * @param sslCertificateRequest
     * @return
     * @throws Exception
     */
    private String getEnrollCSR(CertManagerLogin certManagerLogin, int entityid, int templateid, SSLCertificateRequest sslCertificateRequest) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollCSR";
        String enrollTemplateCA = enrollCSRUrl.replace("templateId", String.valueOf(templateid)).replace("entityid", String.valueOf(entityid));
        CertResponse response = reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
        String updatedRequest = updatedRequestWithCN(response.getResponse(), sslCertificateRequest);
        return updatedRequest;
    }

    //Update request with certificate name

    /**
     * This method will be responsible for updating request with certificate name
     * @param jsonString
     * @param sslCertificateRequest
     * @return
     */
    private String updatedRequestWithCN(String jsonString, SSLCertificateRequest sslCertificateRequest) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonString);
        JsonObject jsonObject1 = jsonObject.getAsJsonObject(SSLCertificateConstants.SUBJECT);
        JsonArray jsonArray = jsonObject1.getAsJsonArray(SSLCertificateConstants.ITEMS);
        JsonObject jsonObject2 = null;
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement jsonElement = jsonArray.get(i);
            jsonObject2 = jsonElement.getAsJsonObject();
            if (jsonObject2.get(SSLCertificateConstants.TYPENAME).getAsString().toString().equals(SSLCertificateConstants.CN)) {
                JsonArray jsonArray2 = jsonElement.getAsJsonObject().getAsJsonArray(SSLCertificateConstants.VALUE);
                for (int j = 0; j < jsonArray2.size(); j++) {
                    JsonElement jsonElement1 = jsonArray2.get(j);
                    jsonObject2 = jsonElement1.getAsJsonObject();
                    jsonObject2.addProperty(SSLCertificateConstants.VALUE, sslCertificateRequest.getCertificateName());
                    break;
                }
            }
            break;
        }
        return jsonObject.toString();
    }


    //petEnrollCSR

    /**
     * Update the CSR details
     * @param certManagerLogin
     * @param entityid
     * @param updatedRequest
     * @return
     * @throws Exception
     */
    private CertResponse putEnrollCSR(CertManagerLogin certManagerLogin, int entityid, String updatedRequest) throws Exception {
        int enrollKeyId = 0;
        String enrollEndPoint = "/certmanager/putEnrollCSR";
        String enrollTemplateCA = enrollUpdateCSRUrl.replace("entityid", String.valueOf(entityid));
        return reqProcessor.processCert(enrollEndPoint, updatedRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
    }

    /**
     * Update the enroll keys
     * @param certManagerLogin
     * @param entityid
     * @param response
     * @param templateid
     * @return
     * @throws Exception
     */
    private int putEnrollKeys(CertManagerLogin certManagerLogin, int entityid, CertResponse response, int templateid) throws Exception {
        int enrollKeyId = 0;
        String enrollEndPoint = "/certmanager/putEnrollKeys";
        String enrollTemplateCA = enrollKeysUrl.replace("templateId", String.valueOf(templateid)).replace("entityid", String.valueOf(entityid));
        CertResponse certResponse = reqProcessor.processCert(enrollEndPoint, response.getResponse(), certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            enrollKeyId = (Integer) responseMap.get(SSLCertificateConstants.SELECTED_ID);
        }
        return enrollKeyId;
    }

    /**
     * Get the enroll keys
     * @param certManagerLogin
     * @param entityid
     * @param templateid
     * @return
     * @throws Exception
     */
    private CertResponse getEnrollKeys(CertManagerLogin certManagerLogin, int entityid, int templateid) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollkeys";
        String enrollTemplateCA = enrollKeysUrl.replace("templateId", String.valueOf(templateid)).replace("entityid", String.valueOf(entityid));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
    }


    //putEnrollTemplates

    /**
     * update the enroll templates
     * @param certManagerLogin
     * @param entityid
     * @param response
     * @param caId
     * @return
     * @throws Exception
     */
    private int putEnrollTemplates(CertManagerLogin certManagerLogin, int entityid, CertResponse response, int caId) throws Exception {
        int enrollTemlateId = 0;
        String enrollEndPoint = "/certmanager/putEnrollTemplates";
        String enrollTempletEndpoint = enrollTemplateUrl.replace("caid", String.valueOf(caId)).replace("entityid", String.valueOf(entityid));
        CertResponse certResponse = reqProcessor.processCert(enrollEndPoint, response.getResponse(), certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTempletEndpoint));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            enrollTemlateId = (Integer) responseMap.get(SSLCertificateConstants.SELECTED_ID);
        }
        return enrollTemlateId;
    }

    //getEnrollTemplate

    /**
     * Get the enroll templates
     * @param certManagerLogin
     * @param entityid
     * @param caId
     * @return
     * @throws Exception
     */
    private CertResponse getEnrollTemplates(CertManagerLogin certManagerLogin, int entityid, int caId) throws Exception {
        String enrollEndPoint = "/certmanager/getEnrollTemplates";
        String enrollTemplateCA = enrollTemplateUrl.replace("caid", String.valueOf(caId)).replace("entityid", String.valueOf(entityid));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollTemplateCA));
    }

    //Update the CA

    /**
     * Update the enroll CA
     * @param certManagerLogin
     * @param entityid
     * @param response
     * @return
     * @throws Exception
     */
    private int putEnrollCA(CertManagerLogin certManagerLogin, int entityid, CertResponse response) throws Exception {
        int selectedId = 0;
        String enrollEndPoint = "/certmanager/putEnrollCA";
        String enrollCA = enrollCAUrl.replace("entityid", String.valueOf(entityid));
        CertResponse certResponse = reqProcessor.processCert(enrollEndPoint, response.getResponse(), certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollCA));
        Map<String, Object> responseMap = ControllerUtil.parseJson(certResponse.getResponse());
        if (!MapUtils.isEmpty(responseMap)) {
            selectedId = (Integer) responseMap.get(SSLCertificateConstants.SELECTED_ID);
        }
        return selectedId;
    }


    //Get the Enroll CA

    /**
     * Update the enroll CA
     * @param certManagerLogin
     * @param entityid
     * @return
     * @throws Exception
     */
    private CertResponse getEnrollCA(CertManagerLogin certManagerLogin, int entityid) throws Exception {
        int selectedId = 0;
        String enrollEndPoint = "/certmanager/getEnrollCA";
        String enrollCA = enrollCAUrl.replace("entityid", String.valueOf(entityid));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(enrollCA));
    }


    //Enroll Certificate

    /**
     * This method will be responsible to create certificate
     * @param certManagerLogin
     * @param entityId
     * @return
     * @throws Exception
     */
    private CertResponse enrollCertificate(CertManagerLogin certManagerLogin, int entityId) throws Exception {
        String enrollEndPoint = "/certmanager/enroll";
        String targetSystemEndPoint = enrollUrl.replace("entityid", String.valueOf(entityId));
        return reqProcessor.processCert(enrollEndPoint, "", certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetSystemEndPoint));
    }

    //Create Target System Service

    /**
     * This method will be responsible to create target system service
     * @param targetSystemServiceRequest
     * @param targetSystemId
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private TargetSystemService createTargetSystemService(TargetSystemServiceRequest targetSystemServiceRequest, int targetSystemId,
                                                          CertManagerLogin certManagerLogin) throws Exception {
        TargetSystemService targetSystemService = null;
        String createTargetSystemEndPoint = "/certmanager/targetsystemservice/create";
        String targetSystemAPIEndpoint = new StringBuffer().append(targetSystems).append("/").
                append(targetSystemId).
                append("/").
                append(targetSystemServies).toString();
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "createTargetSystemService").
                put(LogMessage.MESSAGE, String.format("Trying to create target System Service [%s]", targetSystemServiceRequest)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(createTargetSystemEndPoint, targetSystemServiceRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetSystemAPIEndpoint));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystemService ").
                    put(LogMessage.MESSAGE, "Creation of TargetSystem Service Successful.").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
            if (!MapUtils.isEmpty(responseMap)) {
                String tss_hostname = (String) responseMap.get("hostname");
                String tss_name = (String) responseMap.get("name");
                int tss_port = (Integer) responseMap.get("port");
                int tss_groupId = (Integer) responseMap.get("targetSystemGroupId");
                int tss_systemId = (Integer) responseMap.get("targetSystemId");
                int tss_systemServiceId = (Integer) responseMap.get("targetSystemServiceId");
                targetSystemService = new TargetSystemService(tss_hostname, tss_name, tss_port, tss_groupId, tss_systemId, tss_systemServiceId);
            }
            return targetSystemService;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystemService").
                    put(LogMessage.MESSAGE, "Creation of TargetSystemService failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return targetSystemService;
        }

    }


    //Build TargetSystem Service

    /**
     * Prepare the request for target system service
     * @param sslCertificateRequest
     * @return
     */
    private TargetSystemServiceRequest prepareTargetSystemServiceRequest(SSLCertificateRequest sslCertificateRequest) {
        TargetSystemServiceRequest targetSysServiceRequest = sslCertificateRequest.getTargetSystemServiceRequest();
        TargetSystemServiceRequest targetSystemServiceRequest = new TargetSystemServiceRequest();
        targetSystemServiceRequest.setPort(targetSysServiceRequest.getPort());
        targetSystemServiceRequest.setDescription(targetSysServiceRequest.getDescription());
        targetSystemServiceRequest.setName(targetSysServiceRequest.getName());
        targetSystemServiceRequest.setHostname(targetSysServiceRequest.getHostname());
        
        targetSystemServiceRequest.setMonitoringEnabled(Boolean.TRUE); 
        targetSystemServiceRequest.setMultiIpMonitoringEnabled(Boolean.TRUE);

        return targetSystemServiceRequest;
    }

    //Prepare SSL config Object

    /**
     * Prepare the SSL config Object
     * @param sslCertificateRequest
     * @return
     */
    private SSLCertTypeConfig prepareSSLConfigObject(SSLCertificateRequest sslCertificateRequest) {
        SSLCertTypeConfig sslCertTypeConfig = new SSLCertTypeConfig();
        SSLCertType sslCertType = SSLCertType.valueOf("PRIVATE_SINGLE_SAN");
        sslCertTypeConfig.setSslCertType(sslCertType);
        sslCertTypeConfig.setTargetSystemGroupId(getTargetSystemGroupId(sslCertType));
        return sslCertTypeConfig;
    }


    /**
     * Creates a targetSystem
     *
     * @param targetSystemRequest
     * @param certManagerLogin
     * @return
     */
    private int createTargetSystem(TargetSystem targetSystemRequest, CertManagerLogin certManagerLogin,
                                 SSLCertTypeConfig sslCertTypeConfig) throws Exception {
        int  targetSystemId = 0;
        String createTargetSystemEndPoint = "/certmanager/targetsystem/create";
        String targetSystemAPIEndpoint = new StringBuffer().append(targetSystemGroups).
                append(sslCertTypeConfig.getTargetSystemGroupId()).
                append("/").
                append(targetSystems).toString();
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                put(LogMessage.ACTION, "createTargetSystem").
                put(LogMessage.MESSAGE, String.format("Trying to create target System [%s], [%s]", targetSystemRequest.getName(), targetSystemRequest.getAddress())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                build()));
        CertResponse response = reqProcessor.processCert(createTargetSystemEndPoint, targetSystemRequest, certManagerLogin.getAccess_token(), getCertmanagerEndPoint(targetSystemAPIEndpoint));
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystem").
                    put(LogMessage.MESSAGE, "Creation of TargetSystem Successful.").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
            if (!MapUtils.isEmpty(responseMap)) {
                 targetSystemId = (Integer) responseMap.get("targetSystemID");
            }
            return targetSystemId;
        } else {
            log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "createTargetSystem").
                    put(LogMessage.MESSAGE, "Creation of TargetSystem failed.").
                    put(LogMessage.RESPONSE, response.getResponse()).
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return targetSystemId;
        }
    }

    /**
     * @param certManagerAPIEndpoint
     * @return
     */
    private String getCertmanagerEndPoint(String certManagerAPIEndpoint) {
        if (!StringUtils.isEmpty(certManagerAPIEndpoint) && !StringUtils.isEmpty(certManagerDomain)) {
            StringBuffer endPoint = new StringBuffer(certManagerDomain);
            endPoint.append(certManagerAPIEndpoint);
            return endPoint.toString();
        }
        return "";
    }

    /**
     * Get the target System Group ID
     * @param sslCertType
     * @return
     */
    private int getTargetSystemGroupId(SSLCertType sslCertType) {
        int ts_gp_id = private_single_san_ts_gp_id;
        switch (sslCertType) {
            case PRIVATE_SINGLE_SAN:
                ts_gp_id = private_single_san_ts_gp_id;
                break;
        }
        return ts_gp_id;
    }

        /**
         * Get ssl certificate metadata list
         * @param token
         * @param userDetails
         * @param certName
         * @return
         * @throws Exception
         */

    public ResponseEntity<String> getServiceCertificates(String token, UserDetails userDetails, String certName, Integer limit, Integer offset) throws Exception {
       	log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                  put(LogMessage.ACTION, "getServiceCertificates").
   			      put(LogMessage.MESSAGE, String.format("Trying to get list of Ssl certificatests")).
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));
        String _path = SSLCertificateConstants.SSL_CERT_PATH  ;
       	Response response = new Response();
       	String certListStr = "";
       	String tokenValue= (userDetails.isAdmin())? token :userDetails.getSelfSupportToken();

        response = getMetadata(tokenValue, _path);
        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            certListStr = getsslmetadatalist(response.getResponse(),tokenValue,userDetails,certName,limit,offset);
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getServiceCertificates").
                    put(LogMessage.MESSAGE, "Certificates fetched from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
        }
        else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getServiceCertificates").
                    put(LogMessage.MESSAGE, "Reterived empty certificate list from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body(certListStr);
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                  put(LogMessage.ACTION, "getServiceCertificates").
                  put(LogMessage.MESSAGE, "Failed to get certificate list from metadata").
   			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));

   		return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
   	}


       /**
   	 * Get  for ssl certificate names
   	 * @param token
   	 * @param userDetails
   	 * @param path
   	 * @return
   	 */
   	private Response getMetadata(String token, String path) {

   		String pathStr = path+"?list=true";
   		return reqProcessor.process("/sslcert","{\"path\":\""+pathStr+"\"}",token);
   	}

    /**
   	 * Get metadata for each certificate
   	 * @param token
   	 * @param userDetails
   	 * @param path
   	 * @return
   	 */
	private String getsslmetadatalist(String certificateResponse, String token, UserDetails userDetails,
			String certName, Integer limit, Integer offset) {
		String path = SSLCertificateConstants.SSL_CERT_PATH  ;

   		String pathStr= "";
   		String endPoint = "";
   		Response response = new Response();
   		JsonParser jsonParser = new JsonParser();
   		JsonArray responseArray = new JsonArray();
   		JsonObject metadataJsonObj=new JsonObject();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(certificateResponse);
   		JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
   		List<String> certNames = geMatchCertificates(jsonArray,certName); 		
   		if(limit == null || offset ==null) {
   			limit = certNames.size();
   			offset = 0;
   		}
   		
		if (!userDetails.isAdmin()) {
			responseArray = getMetadataForUser(certNames, userDetails,path,limit,offset);
		} else {
			int maxVal = certNames.size()> (limit+offset)?limit+offset : certNames.size();
			for (int i = offset; i < maxVal; i++) {
				endPoint = certNames.get(i).replaceAll("^\"+|\"+$", "");
				pathStr = path + "/" + endPoint;
				response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
				if (HttpStatus.OK.equals(response.getHttpstatus())) {
					responseArray.add(((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data"));
				}
			}
		}

   		if(ObjectUtils.isEmpty(responseArray)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
 	   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
 	   				  put(LogMessage.ACTION, "get ssl metadata").
 	   			      put(LogMessage.MESSAGE, "Certificates metadata is not available").
 	   			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
 	   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
 	   			      build()));
   		}
   		metadataJsonObj.add("keys", responseArray);   		
   		metadataJsonObj.addProperty("offset", offset);
   		return metadataJsonObj.toString();
   	}

  	/**
   	 * Get the certificate names matches the search keyword
   	 * @param jsonArray
   	 * @param searchText
   	 * @return
   	 */
	private List<String> geMatchCertificates(JsonArray jsonArray, String searchText) {
		List<String> list = new ArrayList<>();
		if (!ObjectUtils.isEmpty(jsonArray)) {
		    if (!StringUtils.isEmpty(searchText)) {
				for (int i = 0; i < jsonArray.size(); i++) {
					if (jsonArray.get(i).toString().contains(searchText)) {
						list.add(jsonArray.get(i).toString());
					}
				}
			} else {
				for (int i = 0; i < jsonArray.size(); i++) {
					list.add(jsonArray.get(i).toString());
				}
			}
		}
		return list;
	}
   	
   	/**
   	 * To Get the metadata details for user
   	 * @param certNames
   	 * @param userDetails
   	 * @param limit
   	 * @param offset
   	 * @return
   	 */
	private JsonArray getMetadataForUser(List<String> certNames, UserDetails userDetails, String path, Integer limit,
			Integer offset) {
		Response response;
		String pathStr = "";
		String endPoint = "";
		int count = 0;
		JsonParser jsonParser = new JsonParser();
		JsonArray responseArray = new JsonArray();
		int maxVal = certNames.size() > (limit + offset) ? limit + offset : certNames.size();
		for (int i = 0; i < certNames.size(); i++) {
			endPoint = certNames.get(i).replaceAll("^\"+|\"+$", "");
			pathStr = path + "/" + endPoint;
			response = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}",
					userDetails.getSelfSupportToken());
			if (HttpStatus.OK.equals(response.getHttpstatus()) && !ObjectUtils.isEmpty(response.getResponse())) {
				JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
				if (userDetails.getUsername().equalsIgnoreCase(
						(object.get("certOwnerNtid") != null ? object.get("certOwnerNtid").getAsString() : ""))) {
					if (count >= offset && count < maxVal) {
						responseArray.add(object);
					}
					count++;
				}
			}
		}
		return responseArray;
	}

    /**
     * To get nclm token
     * @return
     */
    public String getNclmToken() {
        String username = (Objects.nonNull(ControllerUtil.getNclmUsername())) ?
                (new String(Base64.getDecoder().decode(ControllerUtil.getNclmUsername()))) :
                (new String(Base64.getDecoder().decode(certManagerUsername)));

        String password = (Objects.nonNull(ControllerUtil.getNclmPassword())) ?
                (new String(Base64.getDecoder().decode(ControllerUtil.getNclmPassword()))) :
                (new String(Base64.getDecoder().decode(certManagerPassword)));

        CertManagerLoginRequest certManagerLoginRequest = new CertManagerLoginRequest(username, password);
        try {
            CertManagerLogin certManagerLogin = login(certManagerLoginRequest);
            return certManagerLogin.getAccess_token();
        } catch (Exception e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getNclmToken").
                    put(LogMessage.MESSAGE, "Failed to get nclm token").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));

        }
        return null;
    }

    /**
     * To get the list of target systems in a target system group.
     * @param token
     * @param userDetails
     * @return
     * @throws Exception
     */
    public ResponseEntity<String> getTargetSystemList(String token, UserDetails userDetails) throws Exception {
        String getTargetSystemEndpoint = "/certmanager/findTargetSystem";
        String findTargetSystemEndpoint = findTargetSystem.replace("tsgid",
                String.valueOf(getTargetSystemGroupId(SSLCertType.valueOf("PRIVATE_SINGLE_SAN"))));

        List<TargetSystemDetails> targetSystemDetails = new ArrayList<>();
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", getNclmToken(),
                getCertmanagerEndPoint(findTargetSystemEndpoint));

        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null && jsonObject.get(SSLCertificateConstants.TARGETSYSTEMS) != null && !jsonObject.get(SSLCertificateConstants.TARGETSYSTEMS).toString().equalsIgnoreCase("null"))  {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEMS);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    targetSystemDetails.add(new TargetSystemDetails(jsonElement.get(SSLCertificateConstants.NAME).getAsString(),
                            jsonElement.get(SSLCertificateConstants.DESCRIPTION).getAsString(),
                            jsonElement.get(SSLCertificateConstants.ADDRESS).getAsString(),
                            jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_ID).getAsString()));
                }

                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getTargetSystemList").
                        put(LogMessage.MESSAGE, "Successfully retrieved target system list from NCLM").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemDetails)+"}");
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getTargetSystemList").
                    put(LogMessage.MESSAGE, "Retrieved empty target system list from NCLM").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemDetails)+"}");

        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getTargetSystemList").
                put(LogMessage.MESSAGE, "Failed to get Target system list from NCLM").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system list from NCLM\"]}");

    }

    /**
     * Get service list from a target system.
     * @param token
     * @param userDetails
     * @param targetSystemId
     * @return
     */
    public ResponseEntity<String> getTargetSystemServiceList(String token, UserDetails userDetails, String targetSystemId) throws Exception {
        String getTargetSystemEndpoint = "/certmanager/targetsystemservicelist";
        String findTargetSystemEndpoint = findTargetSystemService.replace("tsgid", targetSystemId);

        List<TargetSystemServiceDetails> targetSystemServiceDetails = new ArrayList<>();
        CertResponse response = reqProcessor.processCert(getTargetSystemEndpoint, "", getNclmToken(),
                getCertmanagerEndPoint(findTargetSystemEndpoint));

        if (HttpStatus.OK.equals(response.getHttpstatus())) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null && jsonObject.get(SSLCertificateConstants.TARGETSYSTEM_SERVICES) != null && !jsonObject.get(SSLCertificateConstants.TARGETSYSTEM_SERVICES).toString().equalsIgnoreCase("null")) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.TARGETSYSTEM_SERVICES);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    targetSystemServiceDetails.add(new TargetSystemServiceDetails(jsonElement.get(SSLCertificateConstants.NAME).getAsString(),
                            jsonElement.get(SSLCertificateConstants.DESCRIPTION).getAsString(),
                            jsonElement.get(SSLCertificateConstants.TARGETSYSTEM_SERVICE_ID).getAsString(),
                            jsonElement.get(SSLCertificateConstants.HOSTNAME).getAsString(),
                            jsonElement.get(SSLCertificateConstants.MONITORINGENABLED).getAsBoolean(),
                            jsonElement.get(SSLCertificateConstants.MULTIIPMONITORINGENABLED).getAsBoolean(),
                            jsonElement.get(SSLCertificateConstants.PORT).getAsInt()));
                }
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "getTargetSystemServiceList").
                        put(LogMessage.MESSAGE, "Successfully retrieved target system service list from NCLM").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemServiceDetails)+"}");
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getTargetSystemServiceList").
                    put(LogMessage.MESSAGE, "Retrieved empty target system service list from NCLM").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body("{\"data\": "+JSONUtil.getJSONasDefaultPrettyPrint(targetSystemServiceDetails)+"}");
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getTargetSystemServiceList").
                put(LogMessage.MESSAGE, String.format("Failed to get Target system service list from NCLM for the target system [%s]", targetSystemId)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to get Target system service list from NCLM\"]}");

    }
  	
	/**
	 * Get Revocation Reasons.
	 * 
	 * @param certificateId
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getRevocationReasons(Integer certificateId, String token) {
		CertResponse revocationReasons = new CertResponse();
		try {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Fetch Revocation Reasons")
					.put(LogMessage.MESSAGE,
							String.format("Trying to fetch Revocation Reasons for [%s]", certificateId))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			String nclmAccessToken = getNclmToken();

			String nclmGetCertificateReasonsEndpoint = getCertifcateReasons.replace("certID", certificateId.toString());
			revocationReasons = reqProcessor.processCert("/certificates​/revocationreasons", certificateId,
					nclmAccessToken, getCertmanagerEndPoint(nclmGetCertificateReasonsEndpoint));
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Fetch Revocation Reasons")
					.put(LogMessage.MESSAGE, "Fetch Revocation Reasons")
					.put(LogMessage.STATUS, revocationReasons.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(revocationReasons.getHttpstatus()).body(revocationReasons.getResponse());
		} catch (TVaultValidationException error) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION,
									String.format(
											"Inside  TVaultValidationException " + "Exception = [%s] =  Message [%s]",
											Arrays.toString(error.getStackTrace()), error.getMessage()))
							.build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"" + "Certificate unavailable in NCLM." + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]", 
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"" + SSLCertificateConstants.SSL_CERTFICATE_REASONS_FAILED + "\"]}");
		}

	}

    /**
    * Issue a revocation request for certificate
    *
    * @param certificateId
    * @param token
    * @param revocationRequest
    * @return
    * @throws IOException
    * @throws JsonMappingException
    * @throws JsonParseException
    */
	public ResponseEntity<String> issueRevocationRequest(String certificateName, UserDetails userDetails, String token,
			RevocationRequest revocationRequest) {

		revocationRequest.setTime(getCurrentLocalDateTimeStamp());

		Map<String, String> metaDataParams = new HashMap<String, String>();

		String endPoint = certificateName;
		String _path = SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint;
		Response response = null;
		try {
			if (userDetails.isAdmin()) {
				response = reqProcessor.process("/read", "{\"path\":\"" + _path + "\"}", token);
			} else {
				response = reqProcessor.process("/read", "{\"path\":\"" + _path + "\"}",
						userDetails.getSelfSupportToken());
			}
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Exception = [%s] =  Message [%s]", 
							Arrays.toString(e.getStackTrace()), response.getResponse()))
					.build()));
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certificate unavailable" + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certificate unavailable" + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);

		if (!userDetails.isAdmin()) {

			Boolean isPermission = validateOwnerPermissionForNonAdmin(userDetails, certificateName);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\"" + "Access denied: no permission to revoke certificate" + "\"]}");
			}
		}
		String certID = object.get("certificateId").getAsString();
		float value = Float.valueOf(certID);
		int certificateId = (int) value;
		CertResponse revocationResponse = new CertResponse();
		try {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Issue Revocation Request")
					.put(LogMessage.MESSAGE,
							String.format("Trying to issue Revocation Request for [%s]", certificateId))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			String nclmAccessToken = getNclmToken();

			String nclmApiIssueRevocationEndpoint = issueRevocationRequest.replace("certID",
					String.valueOf(certificateId));
			revocationResponse = reqProcessor.processCert("/certificates/revocationrequest", revocationRequest,
					nclmAccessToken, getCertmanagerEndPoint(nclmApiIssueRevocationEndpoint));
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Issue Revocation Request")
					.put(LogMessage.MESSAGE, "Issue Revocation Request for CertificateID")
					.put(LogMessage.STATUS, revocationResponse.getHttpstatus().toString())
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			boolean sslMetaDataUpdationStatus;
			metaDataParams.put("certificateStatus", "Revoked");
			if (userDetails.isAdmin()) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(_path, metaDataParams, token);
			} else {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(_path, metaDataParams,
						userDetails.getSelfSupportToken());
			}
			if (sslMetaDataUpdationStatus) {
				return ResponseEntity.status(revocationResponse.getHttpstatus())
						.body("{\"messages\":[\"" + "Revocation done successfully" + "\"]}");
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "Revocation Request Failed")
						.put(LogMessage.MESSAGE, "Revocation Request failed for CertificateID")
						.put(LogMessage.STATUS, revocationResponse.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"" + "Revocation failed" + "\"]}");
			}

		} catch (TVaultValidationException error) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  TVaultValidationException  = [%s] =  Message [%s]", 
							Arrays.toString(error.getStackTrace()), error.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + error.getMessage() + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"" + e.getMessage() + "\"]}");
		}
	}
	
	/**		
	 * Get Current Date and Time.
	 * 
	 * @return
	 */
	public String getCurrentLocalDateTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
	}
	
	/**
	 * Validate Permission for Non-admin User.
	 * 
	 * @param userDetails
	 * @param certificateName
	 * @return
	 */
	public Boolean validateOwnerPermissionForNonAdmin(UserDetails userDetails, String certificateName) {
		String ownerPermissionCertName = SSLCertificateConstants.OWNER_PERMISSION_CERTIFICATE + certificateName;
		Boolean isPermission = false;
		if (ArrayUtils.isNotEmpty(userDetails.getPolicies())) {
			isPermission = Arrays.stream(userDetails.getPolicies()).anyMatch(ownerPermissionCertName::equals);
			if (isPermission) {
				log.debug(
						JSONUtil.getJSON(ImmutableMap.<String, String> builder()
								.put(LogMessage.USER,
										ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
								.put(LogMessage.ACTION, "Certificate permission for user " + userDetails.getUsername())
								.put(LogMessage.MESSAGE,
										"User has permission to access the certificate " + certificateName)
								.put(LogMessage.APIURL,
										ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
								.build()));
				return isPermission;
			}
		}

		return isPermission;
	}
	
    /**
   	 * Adds permission to user for a certificate
   	 * @param token
   	 * @param safeUser
   	 * @return
   	 */
   	public ResponseEntity<String> addUserToCertificate(String token, CertificateUser certificateUser, UserDetails userDetails, boolean addSudoPermission) {
   		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   				put(LogMessage.MESSAGE, "Trying to add user to Certificate folder ").
   				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   				build()));
   		
   		if(!areCertificateUserInputsValid(certificateUser)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Invalid user inputs").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
   		}
   		
   		String userName = certificateUser.getUsername().toLowerCase();
   		String certificateName = certificateUser.getCertificateName().toLowerCase();
   		String access = certificateUser.getAccess().toLowerCase();   
   		
   		String authToken = null;
   		
   		boolean isAuthorized = true;
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();   	            
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, "internal");
   			
   			if(!addSudoPermission){
   				isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
   			}
   			
   			if((!addSudoPermission) && (isAuthorized) && (userName.equalsIgnoreCase(certificateMetaData.getCertOwnerNtid()))) {
   				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   	   					put(LogMessage.MESSAGE, "Certificate owner cannot be added as a user to the certificate owned by him").
   	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   	   					build()));
   				
   				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Certificate owner cannot be added as a user to the certificate owned by him\"]}");
   			}
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
   		}
   		
   		if(isAuthorized){   			
   			return checkUserDetailsAndAddCertificateToUser(authToken, userName, certificateName, access);	
   		}else{
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to add users to this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add users to this certificate\"]}");
   		}
   	}

	/**
	 * Method to check the user details and add access policy to certificate
	 * @param token
	 * @param userName
	 * @param certificateName
	 * @param access
	 * @return
	 */
	private ResponseEntity<String> checkUserDetailsAndAddCertificateToUser(String token, String userName,
			String certificateName, String access) {
		
		String policyPrefix = getCertificatePolicyPrefix(access);
		
		if(TVaultConstants.EMPTY.equals(policyPrefix)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Incorrect access requested. Valid values are read, write, deny").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,deny \"]}");
		}

		String policy = policyPrefix + certificateName;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certificateName;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s]", readPolicy, writePolicy, denyPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response userResponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}",token);	
		}
		else {
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}",token);
		}
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				ObjectMapper objMapper = new ObjectMapper();					
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				if (!(TVaultConstants.USERPASS.equals(vaultAuthMethod))) {
					groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while getting the currentpolicies or groups").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
			
			policies.add(policy);
		}else{
			// New user to be configured
			policies.add(policy);
		}
		
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureUserpassUser/configureLDAPUser", policies)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		return configureUserpassOrLDAPUserToUpdateMetadata(token, userName, certificateName, access, groups,
				policiesString, currentpoliciesString);
		
	}

	private String getCertificatePolicyPrefix(String access) {
		String policyPrefix ="";

		switch (access){
			case TVaultConstants.READ_POLICY: policyPrefix = SSLCertificateConstants.READ_CERT_POLICY_PREFIX; break ;
			case TVaultConstants.WRITE_POLICY: policyPrefix = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX; break;
			case TVaultConstants.DENY_POLICY: policyPrefix = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX; break;
			case TVaultConstants.SUDO_POLICY: policyPrefix = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX; break;
			default: log.error(SSLCertificateConstants.ERROR_INVALID_ACCESS_POLICY_MSG); break;
		}
		return policyPrefix;
	}


	/**
	 * Method to configure the Userpass or ldap users and update metadata for add user to certificate
	 * @param token
	 * @param userName
	 * @param certificateName
	 * @param access
	 * @param groups
	 * @param policiesString
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> configureUserpassOrLDAPUserToUpdateMetadata(String token, String userName,
			String certificateName, String access, String groups, String policiesString, String currentpoliciesString) {
		Response ldapConfigresponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,policiesString,token);
		}
		else {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,policiesString,groups,token);
		}

		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return updateMetadataForAddUserToCertificate(token, userName, certificateName, access, groups,
					currentpoliciesString);		
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Trying to configureUserpassUser/configureLDAPUser failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Try Again\"]}");
		}
	}

	/**
	 * Method to update the metadata for user to add the certificate access policies
	 * @param token
	 * @param userName
	 * @param certificateName
	 * @param access
	 * @param groups
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForAddUserToCertificate(String token, String userName,
			String certificateName, String access, String groups, String currentpoliciesString) {
		Response ldapConfigresponse;
		String certificatePath = SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateName;
		Map<String,String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name",userName);
		params.put("path",certificatePath);
		params.put("access",access);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Trying to update metadata [%s]", params.toString())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		Response metadataResponse = ControllerUtil.updateMetadata(params,token);
		if(metadataResponse != null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("User is successfully associated with Certificate [%s] - User [%s]", certificateName, userName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"User is successfully associated \"]}");		
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName,currentpoliciesString,token);
			}
			else {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName,currentpoliciesString,groups,token);
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				log.debug("Reverting user policy update");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...Passed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Please try again\"]}");
			}else{
				log.debug("Reverting user policy update failed");
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_USER_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "User configuration failed. Trying to revert...failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"messages\":[\"User configuration failed.Contact Admin \"]}");
			}
		}
	} 
	
	/**
	 * Validates Certificate User inputs
	 * @param certificateUser
	 * @return boolean
	 */
	private boolean areCertificateUserInputsValid(CertificateUser certificateUser) {
		
		if (ObjectUtils.isEmpty(certificateUser)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateUser.getUsername())
				|| ObjectUtils.isEmpty(certificateUser.getAccess())
				|| ObjectUtils.isEmpty(certificateUser.getCertificateName())
				|| certificateUser.getCertificateName().contains(" ")
	            || (!certificateUser.getCertificateName().endsWith(certificateNameTailText))
	            || (certificateUser.getCertificateName().contains(".-"))
	            || (certificateUser.getCertificateName().contains("-."))
				) {
			return false;
		}
		boolean isValid = true;
		String access = certificateUser.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}

	/**
	 * Adds a group to a certificate
	 * @param userDetails
	 * @param userToken
	 * @param certificateGroup
	 * @return
	 */
	public ResponseEntity<String> addGroupToCertificate(UserDetails userDetails, String userToken, CertificateGroup certificateGroup) {
   		String authToken = null;
   		boolean isAuthorized = true;
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			isAuthorized=isAuthorized(userDetails, certificateGroup.getCertificateName());
   			if(isAuthorized){
   	   			return addingGroupToCertificate(authToken, certificateGroup);
   	   		}else{
   	   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   	   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   	   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_GROUP_TO_CERT_MSG).
   	   					put(LogMessage.MESSAGE, "Access denied: No permission to add groups to this certificate").
   	   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   	   					build()));

   	   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this certificate\"]}");
   	   		}

   		}
   		return ResponseEntity.status(HttpStatus.OK).body("{\"messages\\\":[\"Group is successfully associated with Certificate\"]}");
	}

	/**
	 * isAuthorizedh
	 * @param token
	 * @param certName
	 * @return
	 */
	public boolean isAuthorized(UserDetails userDetails, String certificatename) {
		String certName = certificatename;

		String powerToken = null;
		if (userDetails.isAdmin()) {
			powerToken = userDetails.getClientToken();
		} else {
			powerToken = userDetails.getSelfSupportToken();
		}

		SSLCertificateMetadataDetails sslMetaData = certificateUtils.getCertificateMetaData(powerToken, certName, "internal");

		return certificateUtils.hasAddOrRemovePermission(userDetails, sslMetaData);

	}

	/**
	 * Adds group to a certificate
	 * @param token
	 * @param certificateGroup
	 * @return
	 */
	public ResponseEntity<String> addingGroupToCertificate(String token, CertificateGroup certificateGroup) {
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Add Group to certificate").
				put(LogMessage.MESSAGE, String.format ("Trying to add Group to certificate")).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		if(!ControllerUtil.arecertificateGroupInputsValid(certificateGroup)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}

		//checking whether auth method is userpass or ldap//
		//we should set vaultAuthMethod=ldap//
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"This operation is not supported for Userpass authentication. \"]}");
		}
 		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String> requestMap = null;

		TypeReference<Map<String,String>> typeRef=new TypeReference<Map<String,String>>() {
		};
		try {
			String jsonstr = objMapper.writeValueAsString(certificateGroup);
			requestMap = objMapper.readValue(jsonstr, typeRef);
		} catch (IOException e) {
			log.error(e);
		}

		String groupName = requestMap.get("groupname");
		String certificateName = requestMap.get("certificateName");
		String access = requestMap.get("access");
		groupName = (groupName !=null) ? groupName.toLowerCase() : groupName;
		certificateName = (certificateName != null) ? certificateName.toLowerCase() : certificateName;
		access = (access != null) ? access.toLowerCase(): access;

		String path= SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateName;
		boolean canAddGroup = ControllerUtil.canAddCertPermission(path,certificateName,token);
		String policyPrefix = getCertificatePolicyPrefix(access);
		if(canAddGroup){

			if(TVaultConstants.EMPTY.equals(policyPrefix)){
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,write,deny \"]}");
			}
			String policy = policyPrefix + certificateName;
			String readPolicy = "r_cert_"+certificateName;
			String writePolicy = "w_cert_"+certificateName;
			String denyPolicy = "d_cert_"+certificateName;
			Response getGrpResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}",token);
			String responseJson="";

			List<String> policies = new ArrayList<>();
			List<String> currentpolicies = new ArrayList<>();

			if(HttpStatus.OK.equals(getGrpResp.getHttpstatus())){
				responseJson = getGrpResp.getResponse();
				try {
					currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				} catch (IOException e) {
					log.error(e);
				}
				policies.addAll(currentpolicies);
				policies.remove(readPolicy);
				policies.remove(writePolicy);
				policies.remove(denyPolicy);

				policies.add(policy);
			}else{
				// New group to be configured
				policies.add(policy);
			}

			String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
			String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
			Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,policiesString,token);

			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				Map<String,String> params = new HashMap<String,String>();
				params.put("type", "groups");
				params.put("name",groupName);
				params.put("certificateName",certificateName);
				params.put("access",access);
				params.put("path", path);
				Response metadataResponse = ControllerUtil.updateSslCertificateMetadata(params,token);
				if(metadataResponse !=null && HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus())){
					log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Add Group to certificate").
							put(LogMessage.MESSAGE, "Group configuration Success.").
							put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
				}else{
					ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName,currentpoliciesString,token);
					if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
						log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "Add Group to certificate").
								put(LogMessage.MESSAGE, "group configuration success").
								put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
								put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully associated with Certificate\"]}");
					}else{
						log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
								put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
								put(LogMessage.ACTION, "Add Group to certificate").
								put(LogMessage.MESSAGE, "Group configuration failed").
								put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
								put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
								put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
								build()));
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Contact Admin \"]}");
					}
				}
			}
			else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Add Group to certificate").
						put(LogMessage.MESSAGE, "Group configuration failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed.Contact Admin \"]}");
			}
		}else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add groups to this certificate\"]}");
		}
	}

	/**
     * Associate Approle to Certificate
     * @param userDetails
     * @param certificateApprole
     * @return
     */
    public ResponseEntity<String> associateApproletoCertificate(CertificateApprole certificateApprole, UserDetails userDetails) {        
        String authToken = null;
        boolean isAuthorized = true;
        if(!areCertificateApproleInputsValid(certificateApprole)) {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Invalid input values").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
        }
        
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
                put(LogMessage.MESSAGE, String.format("Trying to add Approle to Certificate - Request [%s]", certificateApprole.toString())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        String approleName = certificateApprole.getApproleName().toLowerCase();
        String certificateName = certificateApprole.getCertificateName().toLowerCase();
        String access = certificateApprole.getAccess().toLowerCase();

        if (approleName.equals(TVaultConstants.SELF_SERVICE_APPROLE_NAME)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to associate this AppRole to any Certificate\"]}");
        }

        if (!ObjectUtils.isEmpty(userDetails)) {

	        if (userDetails.isAdmin()) {
	        	authToken = userDetails.getClientToken();
	        }else {
	        	authToken = userDetails.getSelfSupportToken();
	        }

	        SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, "internal");

			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);

        }else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to add approle to this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));

   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add approle to this certificate\"]}");
   		}

        if(isAuthorized){
        	return createPoliciesAndConfigureApproleToCertificate(authToken, approleName, certificateName, access);
        } else{
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Access denied: No permission to add Approle [%s] to the Certificate [%s]", approleName, certificateName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to add Approle to this Certificate\"]}");
        }
    }

	/**
	 * @param authToken
	 * @param approleName
	 * @param certificateName
	 * @param access
	 * @return
	 */
	private ResponseEntity<String> createPoliciesAndConfigureApproleToCertificate(String authToken, String approleName,
			String certificateName, String access) {
		String policyPrefix = getCertificatePolicyPrefix(access);

		if(TVaultConstants.EMPTY.equals(policyPrefix)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Incorrect access requested. Valid values are read, write, deny").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Incorrect access requested. Valid values are read,deny \"]}");
		}

		String policy = policyPrefix + certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policy is [%s]", policy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certificateName;

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		Response roleResponse = reqProcessor.process("/auth/approle/role/read","{\"role_name\":\""+approleName+"\"}",authToken);

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
		        put(LogMessage.MESSAGE, String.format("roleResponse status is [%s]", roleResponse.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();

		if(HttpStatus.OK.equals(roleResponse.getHttpstatus())) {
			responseJson = roleResponse.getResponse();
			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode policiesArry = objMapper.readTree(responseJson).get("data").get("policies");
				if (null != policiesArry) {
					for (JsonNode policyNode : policiesArry) {
						currentpolicies.add(policyNode.asText());
					}
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
			policies.add(policy);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Non existing role name. Please configure approle as first step - Approle = [%s]", approleName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

		    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("{\"errors\":[\"Non existing role name. Please configure approle as first step\"]}");
		}

		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("policies [%s] before calling configureApprole", policies)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		return configureApproleAndMetadataToCertificate(authToken, approleName, certificateName, access,
				policiesString, currentpoliciesString);
	}

	/**
	 * @param authToken
	 * @param approleName
	 * @param certificateName
	 * @param access
	 * @param policiesString
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> configureApproleAndMetadataToCertificate(String authToken, String approleName,
			String certificateName, String access, String policiesString, String currentpoliciesString) {
		Response approleControllerResp = appRoleService.configureApprole(approleName, policiesString, authToken);

		if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT) || approleControllerResp.getHttpstatus().equals(HttpStatus.OK)){
			return updateApproleMetadataForCertificate(authToken, approleName, certificateName, access,
					currentpoliciesString);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Failed to add Approle [%s] to the Certificate [%s]", approleName, certificateName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Failed to add Approle to the Certificate\"]}");
		}
	}

	/**
	 * @param authToken
	 * @param approleName
	 * @param certificateName
	 * @param access
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateApproleMetadataForCertificate(String authToken, String approleName,
			String certificateName, String access, String currentpoliciesString) {
		String certificatePath = SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateName;
		Map<String,String> params = new HashMap<>();
		params.put("type", "app-roles");
		params.put("name",approleName);
		params.put("path",certificatePath);
		params.put("access",access);
		Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, String.format("Approle [%s] successfully associated with Certificate [%s]", approleName, certificateName)).
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Approle successfully associated with Certificate\"]}");
		} else {
			return rollBackApprolePolicyForCertificate(authToken, approleName, currentpoliciesString, metadataResponse);
		}
	}

	/**
	 * @param authToken
	 * @param approleName
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> rollBackApprolePolicyForCertificate(String authToken, String approleName,
			String currentpoliciesString, Response metadataResponse) {
		Response approleControllerResp;
		approleControllerResp = appRoleService.configureApprole(approleName, currentpoliciesString, authToken);
		if(approleControllerResp.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting, Approle policy update success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Please try again\"]}");
		}else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.ADD_APPROLE_TO_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting Approle policy update failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Approle configuration failed. Contact Admin\"]}");
		}
	}

	/**
	 * Validates Certificate approle inputs
	 * @param certificateApprole
	 * @return boolean
	 */
	private boolean areCertificateApproleInputsValid(CertificateApprole certificateApprole) {

		if (ObjectUtils.isEmpty(certificateApprole)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateApprole.getApproleName())
				|| ObjectUtils.isEmpty(certificateApprole.getAccess())
				|| ObjectUtils.isEmpty(certificateApprole.getCertificateName())
				|| certificateApprole.getCertificateName().contains(" ")
	            || (!certificateApprole.getCertificateName().endsWith(certificateNameTailText))
	            || (certificateApprole.getCertificateName().contains(".-"))
	            || (certificateApprole.getCertificateName().contains("-."))
				) {
			return false;
		}
		boolean isValid = true;
		String access = certificateApprole.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}

    /**
     * Check if user has download permission.
     * @param certificateName
     * @param userDetails
     * @return
     */
	public boolean hasDownloadPermission(String certificateName, UserDetails userDetails) {
        String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX + certificateName;
        String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX + certificateName;
        String renewRevokePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX + certificateName;
        if (userDetails.isAdmin()) {
            return true;
        }
        VaultTokenLookupDetails  vaultTokenLookupDetails = null;
        try {
            vaultTokenLookupDetails = tokenValidator.getVaultTokenLookupDetails(userDetails.getClientToken());
        } catch (TVaultValidationException e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "hasDownloadPermission").
                    put(LogMessage.MESSAGE, String.format ("Failed to get lookup details for user  [%s]", userDetails.getUsername())).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return false;
        }
        String[] policies = vaultTokenLookupDetails.getPolicies();
        if (ArrayUtils.isNotEmpty(policies) && (Arrays.asList(policies).contains(readPolicy) || Arrays.asList(policies).contains(sudoPolicy) || Arrays.asList(policies).contains(renewRevokePolicy))) {
            return true;
        }
        return false;
    }

    public SSLCertificateMetadataDetails getCertificateMetadata(String token, String certificateName) {
        return certificateUtils.getCertificateMetaData(token, certificateName, "internal");
    }

    /**
     * Download certificate.
     * @param token
     * @param certificateDownloadRequest
     * @param userDetails
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadCertificateWithPrivateKey(String token, CertificateDownloadRequest certificateDownloadRequest, UserDetails userDetails) {

        String certName = certificateDownloadRequest.getCertificateName();
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certName, "internal");
        if (hasDownloadPermission(certificateDownloadRequest.getCertificateName(), userDetails) && sslCertificateMetadataDetails!= null) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                    put(LogMessage.MESSAGE, String.format ("Trying to download certificate [%s]", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return downloadCertificateWithPrivateKey(certificateDownloadRequest, sslCertificateMetadataDetails);
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                put(LogMessage.MESSAGE, String.format ("Access denied: [%s] has no permission to download certificate [%s] or certificate is not onboarded in T-Vault", userDetails.getUsername(), certName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }

    /**
     * Download certificate.
     * @param certificateDownloadRequest
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadCertificateWithPrivateKey(CertificateDownloadRequest certificateDownloadRequest, SSLCertificateMetadataDetails sslCertificateMetadataDetails) {
        InputStreamResource resource = null;
        int certId = sslCertificateMetadataDetails.getCertificateId();
        String certName = certificateDownloadRequest.getCertificateName();

        String nclmToken = getNclmToken();
        if (StringUtils.isEmpty(nclmToken)) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Invalid nclm token", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }

        String fileType;
        switch (certificateDownloadRequest.getFormat()) {
            case SSLCertificateConstants.CERT_DOWNLOAD_TYPE_PKCS12DERR: fileType=".p12"; break;
            case SSLCertificateConstants.CERT_DOWNLOAD_TYPE_PEMBUNDLE: fileType=".pem"; break;
            case SSLCertificateConstants.CERT_DOWNLOAD_TYPE_PKCS12PEM:
            default: fileType=".pfx"; break;
        }
        String downloadFileName = certificateDownloadRequest.getCertificateName()+fileType;
        HttpClient httpClient;
        String api = certManagerDomain + "certificates/"+certId+"/privatekeyexport";
        try {
            httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
                    NoopHostnameVerifier.INSTANCE).
                    setSSLContext(
                            new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
                                @Override
                                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                    return true;
                                }
                            }).build()
                    ).setRedirectStrategy(new LaxRedirectStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getApiResponse").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to create hhtpClient", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }

        HttpPost postRequest = new HttpPost(api);
        postRequest.addHeader("Authorization", "Bearer "+ nclmToken);
        postRequest.addHeader("Content-type", "application/json");
        postRequest.addHeader("Accept","application/octet-stream");
        StringEntity stringEntity;
        try {
            stringEntity = new StringEntity("{\"format\":\""+certificateDownloadRequest.getFormat()+"\",\"password\":\""+certificateDownloadRequest.getCertificateCred()+"\", \"issuerChain\": "+certificateDownloadRequest.isIssuerChain()+"}");
        } catch (UnsupportedEncodingException e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to encode request", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }
        postRequest.setEntity(stringEntity);

        try {
            HttpResponse apiResponse = httpClient.execute(postRequest);

            if (apiResponse.getStatusLine().getStatusCode() != 200) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
            }
            HttpEntity entity = apiResponse.getEntity();
            if (entity != null) {
                String responseString = EntityUtils.toString(entity, "UTF-8");
                // nclm api will give certificate in base64 encoded format
                byte[] decodedBytes = Base64.getDecoder().decode(responseString);
                resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
                return ResponseEntity.status(HttpStatus.OK).contentLength(decodedBytes.length)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+downloadFileName+"\"")
                        .contentType(MediaType.parseMediaType("application/x-pkcs12;charset=utf-8"))
                        .body(resource);
            }
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to get api response from NCLM", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

        } catch (IOException e) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "downloadCertificate").
                    put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to get api response from NCLM", certName)).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
    }

    /**
     * Download certificate.
     * @param token
     * @param userDetails
     * @param certificateName
     * @param certificateType
     * @return
     */
    public ResponseEntity<InputStreamResource> downloadCertificate(String token, UserDetails userDetails, String certificateName, String certificateType) {

        InputStreamResource resource = null;
        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certificateName, "internal");
        if (hasDownloadPermission(certificateName, userDetails) && sslCertificateMetadataDetails != null) {

            String nclmToken = getNclmToken();
            if (StringUtils.isEmpty(nclmToken)) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Invalid nclm token", certificateName)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
            }

            String contentType;
            switch (certificateType) {
                case "der": contentType = "application/pkix-cert"; break;
                case "pem":
                default: contentType = "application/x-pem-file"; break;
            }

            HttpClient httpClient;

            String api = certManagerDomain + "certificates/"+sslCertificateMetadataDetails.getCertificateId()+"/"+certificateType;

            try {
                httpClient = HttpClientBuilder.create().setSSLHostnameVerifier(
                        NoopHostnameVerifier.INSTANCE).
                        setSSLContext(
                                new SSLContextBuilder().loadTrustMaterial(null,new TrustStrategy() {
                                    @Override
                                    public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                                        return true;
                                    }
                                }).build()
                        ).setRedirectStrategy(new LaxRedirectStrategy()).build();


            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate [%s]. Failed to create hhtpClient", certificateName)).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
            }

            HttpGet getRequest = new HttpGet(api);
            getRequest.addHeader("accept", "application/json");
            getRequest.addHeader("Authorization", "Bearer "+ nclmToken);

            try {
                HttpResponse apiResponse = httpClient.execute(getRequest);
                if (apiResponse.getStatusLine().getStatusCode() != 200) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
                }

                HttpEntity entity = apiResponse.getEntity();
                if (entity != null) {
                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    // nclm api will give certificate in base64 encoded format
                    byte[] decodedBytes = Base64.getDecoder().decode(responseString);
                    resource = new InputStreamResource(new ByteArrayInputStream(decodedBytes));
                    return ResponseEntity.status(HttpStatus.OK).contentLength(decodedBytes.length)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+certificateName+"\"")
                            .contentType(MediaType.parseMediaType(contentType+";charset=utf-8"))
                            .body(resource);
                }
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, "Failed to download certificate").
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);

            } catch (IOException e) {
                log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "downloadCertificate").
                        put(LogMessage.MESSAGE, String.format ("Failed to download certificate")).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resource);
        }

        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "downloadCertificateWithPrivateKey").
                put(LogMessage.MESSAGE, String.format ("Access denied: [%s] has no permission to download certificate [%s] or certificate is not onboarded in T-Vault", userDetails.getUsername(), certificateName)).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resource);
    }


    /**
     * Get certificate details.
     * @param token
     * @param userDetails
     * @param certificateName
     * @return
     */
    public ResponseEntity<String> getCertificateDetails(String token, UserDetails userDetails, String certificateName,
    		String certificateType) {

        SSLCertificateMetadataDetails sslCertificateMetadataDetails = certificateUtils.getCertificateMetaData(token, certificateName, certificateType);
        if (sslCertificateMetadataDetails !=null) {
            return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(sslCertificateMetadataDetails));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: Unable to read certificate details.\"]}");
    }
    
    /**
	 * Renew SSL Certificate and update metadata
	 * 
	 * @param certificateId
	 * @param token
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public ResponseEntity<String> renewCertificate(String certificateName, UserDetails userDetails, String token) {			

		Map<String, String> metaDataParams = new HashMap<String, String>();

		String endPoint = certificateName;
		String _path = SSLCertificateConstants.SSL_CERT_PATH + "/" + endPoint;
		Response response = new Response();
		if (!userDetails.isAdmin()) {
			Boolean isPermission = validateOwnerPermissionForNonAdmin(userDetails, certificateName);

			if (!isPermission) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("{\"errors\":[\""
								+ "Access denied: No permission to renew certificate"
								+ "\"]}");
			}
		}
		try {
			if (userDetails.isAdmin()) {
				response = reqProcessor.process("/read", "{\"path\":\"" + _path + "\"}", token);
			} else {
				response = reqProcessor.process("/read", "{\"path\":\"" + _path + "\"}",
						userDetails.getSelfSupportToken());
			}
		} catch (Exception e) {
			log.error(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION,
											String.format("Exception = [%s] =  Message [%s]",
													Arrays.toString(e.getStackTrace()), response.getResponse()))
									.build()));
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"messages\":[\"" + "Certficate unavailable" + "\"]}");
		}
		if (!HttpStatus.OK.equals(response.getHttpstatus())) {
			return ResponseEntity.status(response.getHttpstatus())
					.body("{\"errors\":[\"" + "Certficate unavailable" + "\"]}");
		}
		JsonParser jsonParser = new JsonParser();
		JsonObject object = ((JsonObject) jsonParser.parse(response.getResponse())).getAsJsonObject("data");
		metaDataParams = new Gson().fromJson(object.toString(), Map.class);		
		
		String certID = object.get("certificateId").getAsString();
        float value = Float.valueOf(certID);
		int certificateId = (int) value;
		
		CertResponse renewResponse = new CertResponse();
		try {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "Renew certificate")
					.put(LogMessage.MESSAGE,
							String.format("Trying to renew certificate for [%s]", certificateName))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			String nclmAccessToken = getNclmToken();

			String nclmApiRenewEndpoint = renewCertificateEndpoint.replace("certID", String.valueOf(certificateId));
			renewResponse = reqProcessor.processCert("/certificates/renew", "",
					nclmAccessToken, getCertmanagerEndPoint(nclmApiRenewEndpoint));
			Thread.sleep(renewDelayTime);
			log.debug(
					JSONUtil.getJSON(
							ImmutableMap.<String, String> builder()
									.put(LogMessage.USER,
											ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
									.put(LogMessage.ACTION, "Renew certificate")
									.put(LogMessage.MESSAGE, "Renew certificate for CertificateID")
									.put(LogMessage.STATUS, renewResponse.getHttpstatus().toString())
									.put(LogMessage.APIURL,
											ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
									.build()));
			
			//if renewed get new certificate details and update metadata
			if (renewResponse!=null && HttpStatus.OK.equals(renewResponse.getHttpstatus())) {
			CertificateData certData = getLatestCertificate(certificateName,nclmAccessToken);			
			boolean sslMetaDataUpdationStatus=true;		
			if(!ObjectUtils.isEmpty(certData)) {
			metaDataParams.put("certificateId",((Integer)certData.getCertificateId()).toString()!=null?
					((Integer)certData.getCertificateId()).toString():String.valueOf(certificateId));
			metaDataParams.put("createDate", certData.getCreateDate()!=null?certData.getCreateDate():object.get("createDate").getAsString());
			metaDataParams.put("expiryDate", certData.getExpiryDate()!=null?certData.getExpiryDate():object.get("expiryDate").getAsString());			
			metaDataParams.put("certificateStatus", certData.getCertificateStatus()!=null?certData.getCertificateStatus():
				object.get("certificateStatus").getAsString());
						
			if (userDetails.isAdmin()) {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(_path, metaDataParams, token);
			} else {
				sslMetaDataUpdationStatus = ControllerUtil.updateMetaData(_path, metaDataParams,
						userDetails.getSelfSupportToken());
			}
			}
			if (sslMetaDataUpdationStatus) {
				return ResponseEntity.status(renewResponse.getHttpstatus())
						.body("{\"messages\":[\"" + "Certificate Renewed Successfully" + "\"]}");
			} else {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "Renew certificate Failed")
										.put(LogMessage.MESSAGE, "Metadata updation failed for CertificateID")
										.put(LogMessage.STATUS, renewResponse.getHttpstatus().toString())
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"" + "Metadata updation Failed." + "\"]}");
			}
			}else {
				log.error(
						JSONUtil.getJSON(
								ImmutableMap.<String, String> builder()
										.put(LogMessage.USER,
												ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
										.put(LogMessage.ACTION, "Renew certificate Failed")
										.put(LogMessage.MESSAGE, "Renew Request failed for CertificateID")
										.put(LogMessage.STATUS, renewResponse.getHttpstatus().toString())
										.put(LogMessage.APIURL,
												ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
										.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"" + "Certificate Renewal Failed" + "\"]}");
			}

		} catch (TVaultValidationException error) {
			log.error(
					JSONUtil.getJSON(ImmutableMap.<String, String> builder()
							.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
							.put(LogMessage.ACTION, String.format("Inside  TVaultValidationException  = [%s] =  Message [%s]",
									Arrays.toString(error.getStackTrace()), error.getMessage()))
							.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"" + error.getMessage() + "\"]}");
		} catch (Exception e) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String> builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, String.format("Inside  Exception = [%s] =  Message [%s]",
							Arrays.toString(e.getStackTrace()), e.getMessage()))
					.build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("{\"errors\":[\"" + e.getMessage() + "\"]}");
		}
	}	
	
	
	/**
     * To Get the latest certificate details in nclm for a given renewed certificate name
     * @param sslCertificateRequest
     * @param certManagerLogin
     * @return
     * @throws Exception
     */
    private CertificateData getLatestCertificate(String certName, String accessToken) throws Exception {
        CertificateData certificateData=new CertificateData(); 
        int containerId = getTargetSystemGroupId(SSLCertType.valueOf("PRIVATE_SINGLE_SAN"));
        String findCertificateEndpoint = "/certmanager/findCertificate";
        String targetEndpoint = findCertificate.replace("certname", String.valueOf(certName)).replace("cid", String.valueOf(containerId));
        CertResponse response = reqProcessor.processCert(findCertificateEndpoint, "", accessToken, getCertmanagerEndPoint(targetEndpoint));        
        Map<String, Object> responseMap = ControllerUtil.parseJson(response.getResponse());
        if (!MapUtils.isEmpty(responseMap) && (ControllerUtil.parseJson(response.getResponse()).get(SSLCertificateConstants.CERTIFICATES) != null)) {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            if (jsonObject != null) {
                JsonArray jsonArray = jsonObject.getAsJsonArray(SSLCertificateConstants.CERTIFICATES);
                LocalDateTime  createdDate = null ;
                LocalDateTime  certCreatedDate;
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonElement = jsonArray.get(i).getAsJsonObject();
                    if(i==0) {
                    createdDate = LocalDateTime.parse(validateString(jsonElement.get("NotBefore")).substring(0, 19));
                    }else if (i>0) {
                    	createdDate = LocalDateTime.parse(validateString(jsonArray.get(i-1).getAsJsonObject().get("NotBefore")).substring(0, 19));
                    }
                    if ((Objects.equals(getCertficateName(jsonElement.get("sortedSubjectName").getAsString()), certName))) {
                    	certCreatedDate = LocalDateTime.parse(validateString(jsonElement.get("NotBefore")).substring(0, 19));
                    	if(!ObjectUtils.isEmpty(createdDate) && (createdDate.isBefore(certCreatedDate) || createdDate.isEqual(certCreatedDate))) {
                        certificateData= new CertificateData();
                        certificateData.setCertificateId(Integer.parseInt(jsonElement.get("certificateId").getAsString()));
                        certificateData.setExpiryDate(validateString(jsonElement.get("NotAfter")));
                        certificateData.setCreateDate(validateString(jsonElement.get("NotBefore")));                       
                        certificateData.setCertificateStatus(validateString(jsonElement.get(SSLCertificateConstants.CERTIFICATE_STATUS)));
                        certificateData.setCertificateName(certName);
                                               
                    	}
                    }
                }                
            }
        }
        return certificateData;
    }    

	/**
	 * Removes user from certificate
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromCertificate(CertificateUser certificateUser, UserDetails userDetails) {
		
		if(!areCertificateUserInputsValid(certificateUser)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Invalid user inputs").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
   		}
		
		String userName = certificateUser.getUsername().toLowerCase();
   		String certificateName = certificateUser.getCertificateName().toLowerCase();   		
   		String authToken = null;   		
   		boolean isAuthorized = true;
   		
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();   	            
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, "internal");
   			
   			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData); 
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to remove user from this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}");
   		}
		
		if(isAuthorized){
			return checkUserPolicyAndRemoveFromCertificate(userName, certificateName, authToken);	
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Access denied: No permission to remove user from this certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove user from this certificate\"]}");
		}
	}

	/**
	 * @param userName
	 * @param certificateName
	 * @param authToken
	 * @return
	 */
	private ResponseEntity<String> checkUserPolicyAndRemoveFromCertificate(String userName, String certificateName,
			String authToken) {
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certificateName;
		
		log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		Response userResponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			userResponse = reqProcessor.process("/auth/userpass/read","{\"username\":\""+userName+"\"}", authToken);	
		}
		else {
			userResponse = reqProcessor.process("/auth/ldap/users","{\"username\":\""+userName+"\"}", authToken);
		}

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("userResponse status is [%s]", userResponse.getHttpstatus())).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));

		String responseJson="";
		String groups="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		
		if(HttpStatus.OK.equals(userResponse.getHttpstatus())){
			responseJson = userResponse.getResponse();	
			try {
				ObjectMapper objMapper = new ObjectMapper();
				currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
				if (!(TVaultConstants.USERPASS.equals(vaultAuthMethod))) {
					groups =objMapper.readTree(responseJson).get("data").get("groups").asText();
				}
			} catch (IOException e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
						put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
			}
			
			policies.addAll(currentpolicies);				
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		Response ldapConfigresponse;
		if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
			ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, policiesString, authToken);
		}
		else {
			ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, policiesString, groups, authToken);
		}
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){
			return updateMetadataForRemoveUserFromCertificate(userName, certificateName, authToken, groups,
					currentpoliciesString);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Failed to remvoe the user from the certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remvoe the user from the certificate\"]}");
		}
	}

	/**
	 * @param userName
	 * @param certificateName
	 * @param authToken
	 * @param groups
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForRemoveUserFromCertificate(String userName, String certificateName,
			String authToken, String groups, String currentpoliciesString) {
		Response ldapConfigresponse;
		// User has been associated with certificate. Now metadata has to be deleted
		String certificatePath = SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateName;
		Map<String,String> params = new HashMap<>();
		params.put("type", "users");
		params.put("name",userName);
		params.put("path",certificatePath);
		params.put("access","delete");
		
		Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
		if(metadataResponse != null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "User is successfully Removed from Certificate").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Successfully removed user from the certificate\"]}");
		} else {
			if (TVaultConstants.USERPASS.equals(vaultAuthMethod)) {
				ldapConfigresponse = ControllerUtil.configureUserpassUser(userName, currentpoliciesString, authToken);
			}
			else {
				ldapConfigresponse = ControllerUtil.configureLDAPUser(userName, currentpoliciesString, groups, authToken);
			}
			if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Failed to remove the user from the certificate. Metadata update failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the user from the certificate. Metadata update failed\"]}");
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
						put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
						put(LogMessage.MESSAGE, "Failed to revert user association on certificate").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
						build()));
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to revert user association on certificate\"]}");
			}
		}
	}
	
	/**
     * Remove Group from certificate
     * @param certificateGroup
     * @param userDetails
     * @return
     */
    public ResponseEntity<String> removeGroupFromCertificate(CertificateGroup certificateGroup, UserDetails userDetails) {
    	
    	if(!areCertificateGroupInputsValid(certificateGroup)) {
   			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Invalid user inputs").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
   		}
    	
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
                put(LogMessage.MESSAGE, String.format("Trying to remove Group from certificate - [%s]", certificateGroup.toString())).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        
        String groupName = certificateGroup.getGroupname().toLowerCase();
   		String certificateName = certificateGroup.getCertificateName().toLowerCase();
   		
   		String authToken = null;
   		
   		boolean isAuthorized = true;
   		
   		if (!ObjectUtils.isEmpty(userDetails)) {
   			if (userDetails.isAdmin()) {
   				authToken = userDetails.getClientToken();   	            
   	        }else {
   	        	authToken = userDetails.getSelfSupportToken();
   	        }
   			SSLCertificateMetadataDetails certificateMetaData = certificateUtils.getCertificateMetaData(authToken, certificateName, "internal");
   			
   			isAuthorized = certificateUtils.hasAddOrRemovePermission(userDetails, certificateMetaData);
   			
   		}else {
   			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
   					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_GROUP_FROM_CERT_MSG).
   					put(LogMessage.MESSAGE, "Access denied: No permission to remove group from this certificate").
   					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
   					build()));
   			
   			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove group from this certificate\"]}");
   		} 
   		
        if(isAuthorized){        	
        	return checkPolicyDetailsAndRemoveGroupFromCertificate(groupName, certificateName, authToken);
        } else {
        	log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
                    put(LogMessage.MESSAGE, "Access denied: No permission to remove groups from this certificate").
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: No permission to remove groups from this certificate\"]}");
        }

    }

	/**
	 * Method to check the group policy and remove the group from certificate
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @return
	 */
	private ResponseEntity<String> checkPolicyDetailsAndRemoveGroupFromCertificate(String groupName,
			String certificateName, String authToken) {
		String readPolicy = SSLCertificateConstants.READ_CERT_POLICY_PREFIX+certificateName;
		String writePolicy = SSLCertificateConstants.WRITE_CERT_POLICY_PREFIX+certificateName;
		String denyPolicy = SSLCertificateConstants.DENY_CERT_POLICY_PREFIX+certificateName;
		String sudoPolicy = SSLCertificateConstants.SUDO_CERT_POLICY_PREFIX+certificateName;
		
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
				put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
				put(LogMessage.MESSAGE, String.format ("Policies are, read - [%s], write - [%s], deny -[%s], owner - [%s]", readPolicy, writePolicy, denyPolicy, sudoPolicy)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
				build()));
		
		Response groupResp = reqProcessor.process("/auth/ldap/groups","{\"groupname\":\""+groupName+"\"}", authToken);

		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		        put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		        put(LogMessage.MESSAGE, String.format ("Group Response status is [%s]", groupResp.getHttpstatus())).
		        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		        build()));

		String responseJson="";
		List<String> policies = new ArrayList<>();
		List<String> currentpolicies = new ArrayList<>();
		
		if(HttpStatus.OK.equals(groupResp.getHttpstatus())){
		    responseJson = groupResp.getResponse();
		    try {
		        ObjectMapper objMapper = new ObjectMapper();
		        currentpolicies = ControllerUtil.getPoliciesAsListFromJson(objMapper, responseJson);
		    } catch (IOException e) {
		        log.error(e);
		        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		                put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		                put(LogMessage.MESSAGE, "Exception while creating currentpolicies or groups").
		                put(LogMessage.STACKTRACE, Arrays.toString(e.getStackTrace())).
		                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		                build()));
		    }

		    policies.addAll(currentpolicies);
			policies.remove(readPolicy);
			policies.remove(writePolicy);
			policies.remove(denyPolicy);
		}
		String policiesString = org.apache.commons.lang3.StringUtils.join(policies, ",");
		String currentpoliciesString = org.apache.commons.lang3.StringUtils.join(currentpolicies, ",");
		
		Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, policiesString, authToken);

		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT) || ldapConfigresponse.getHttpstatus().equals(HttpStatus.OK)){

			return updateMetadataForRemoveGroupFromCertificate(groupName, certificateName, authToken,
					currentpoliciesString);
		} else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
		            put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
		            put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
		            put(LogMessage.MESSAGE, "Failed to remove the group from the certificate").
		            put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
		            build()));
		    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Failed to remove the group from the certificate\"]}");
		}
	}

	/**
	 * Method to update the metadata after removed the group policy for a certificate
	 * @param groupName
	 * @param certificateName
	 * @param authToken
	 * @param currentpoliciesString
	 * @return
	 */
	private ResponseEntity<String> updateMetadataForRemoveGroupFromCertificate(String groupName, String certificateName,
			String authToken, String currentpoliciesString) {		
		String certificatePath = SSLCertificateConstants.SSL_CERT_PATH_VALUE + certificateName;
		Map<String,String> params = new HashMap<>();
		params.put("type", "groups");
		params.put("name", groupName);
		params.put("path",certificatePath);
		params.put("access","delete");
		
		Response metadataResponse = ControllerUtil.updateMetadata(params, authToken);
		
		if(metadataResponse !=null && (HttpStatus.NO_CONTENT.equals(metadataResponse.getHttpstatus()) || HttpStatus.OK.equals(metadataResponse.getHttpstatus()))){
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, String.format ("Group - [%s] is successfully removed from the certificate - [%s]", groupName, certificateName)).
					put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Group is successfully removed from certificate\"]}");
		}else {				
			return revertGroupPolicyIfMetadataUpdateFailed(groupName, authToken, currentpoliciesString,
					metadataResponse);
		}
	}

	/**
	 * Method to revert group policy if metadata update failed for certificate
	 * @param groupName
	 * @param authToken
	 * @param currentpoliciesString
	 * @param metadataResponse
	 * @return
	 */
	private ResponseEntity<String> revertGroupPolicyIfMetadataUpdateFailed(String groupName, String authToken,
			String currentpoliciesString, Response metadataResponse) {
		Response ldapConfigresponse = ControllerUtil.configureLDAPGroup(groupName, currentpoliciesString, authToken);
		
		if(ldapConfigresponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting, group policy update success").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Please try again\"]}");
		} else{
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
					put(LogMessage.ACTION, SSLCertificateConstants.REMOVE_USER_FROM_CERT_MSG).
					put(LogMessage.MESSAGE, "Reverting group policy update failed").
					put(LogMessage.RESPONSE, (null!=metadataResponse)?metadataResponse.getResponse():TVaultConstants.EMPTY).
					put(LogMessage.STATUS, (null!=metadataResponse)?metadataResponse.getHttpstatus().toString():TVaultConstants.EMPTY).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
					build()));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Group configuration failed. Contact Admin \"]}");
		}
	}

    
	/**
	 * Validates Certificate group inputs
	 * @param certificateUser
	 * @return boolean
	 */
	private boolean areCertificateGroupInputsValid(CertificateGroup certificateGroup) {
		
		if (ObjectUtils.isEmpty(certificateGroup)) {
			return false;
		}
		if (ObjectUtils.isEmpty(certificateGroup.getGroupname())
				|| ObjectUtils.isEmpty(certificateGroup.getAccess())
				|| ObjectUtils.isEmpty(certificateGroup.getCertificateName())
				|| certificateGroup.getCertificateName().contains(" ")
	            || (!certificateGroup.getCertificateName().endsWith(certificateNameTailText))
	            || (certificateGroup.getCertificateName().contains(".-"))
	            || (certificateGroup.getCertificateName().contains("-."))
				) {
			return false;
		}
		boolean isValid = true;
		String access = certificateGroup.getAccess();
		if (!ArrayUtils.contains(PERMISSIONS, access)) {
			isValid = false;
		}
		return isValid;
	}


	/**
	 * Get List Of internal or external certificates from the path sslcerts or externalcerts
	 * @param token
	 * @param certificateType
	 * @param userDetails
	 * @return
	 * @throws Exception
	 */
	public ResponseEntity<String> getListOfCertificates(String token, String certificateType, UserDetails userDetails)
			throws Exception {
		Response response = new Response();
		String _path = "";
		if (certificateType.equalsIgnoreCase("internal")) {
			_path = SSLCertificateConstants.SSL_CERT_PATH_VALUE;
		} else {
			_path = SSLCertificateConstants.SSL_CERT_PATH_VALUE_EXT;
		}
		String tokenValue = (userDetails.isAdmin()) ? token : userDetails.getSelfSupportToken();

		response = getMetadata(tokenValue, _path);


		if (HttpStatus.OK.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getListOfCertificates").
                    put(LogMessage.MESSAGE, "Certificates fetched from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
        }
        else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                    put(LogMessage.ACTION, "getListOfCertificates").
                    put(LogMessage.MESSAGE, "Reterived empty certificate list from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body(response.getResponse());
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
   			      put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
                  put(LogMessage.ACTION, "getListOfCertificates").
                  put(LogMessage.MESSAGE, "Failed to get certificate list from metadata").
   			      put(LogMessage.STATUS, response.getHttpstatus().toString()).
   			      put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
   			      build()));

   		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

    /**
     * To get all certificate metadata details.
     * @param token
     * @param certName
     * @param limit
     * @param offset
     * @return
     */
    public ResponseEntity<String> getAllCertificates(String token, String certName, String certType, Integer limit, Integer offset) {
        log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getAllCertificates").
                put(LogMessage.MESSAGE, "Trying to get all certificates").
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));
        String path = SSLCertificateConstants.SSL_CERT_PATH ;

        if (certType.equalsIgnoreCase("external")) {
            path = SSLCertificateConstants.SSL_CERT_PATH_EXT ;
        }

        Response response;
        String certListStr = "";

        response = getMetadata(token, path);
        if (HttpStatus.OK.equals(response.getHttpstatus())) {

            String pathStr= "";
            String endPoint = "";
            Response metadataResponse = new Response();
            JsonParser jsonParser = new JsonParser();
            JsonArray responseArray = new JsonArray();
            JsonObject metadataJsonObj=new JsonObject();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(response.getResponse());
            JsonArray jsonArray = jsonObject.getAsJsonObject("data").getAsJsonArray("keys");
            List<String> certNames = geMatchCertificates(jsonArray,certName);
            if(limit == null) {
                limit = certNames.size();
            }
            if (offset ==null) {
                offset = 0;
            }

            int maxVal = certNames.size()> (limit+offset)?limit+offset : certNames.size();
            for (int i = offset; i < maxVal; i++) {
                endPoint = certNames.get(i).replaceAll("^\"+|\"+$", "");
                pathStr = path + TVaultConstants.PATH_DELIMITER + endPoint;
                metadataResponse = reqProcessor.process("/sslcert", "{\"path\":\"" + pathStr + "\"}", token);
                if (HttpStatus.OK.equals(metadataResponse.getHttpstatus())) {
                    JsonObject certObj = ((JsonObject) jsonParser.parse(metadataResponse.getResponse())).getAsJsonObject("data");
                    certObj.remove("users");
                    certObj.remove("groups");
                    responseArray.add(certObj);
                }
            }

            if(ObjectUtils.isEmpty(responseArray)) {
                log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                        put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                        put(LogMessage.ACTION, "get ssl metadata").
                        put(LogMessage.MESSAGE, "Certificates metadata is not available").
                        put(LogMessage.STATUS, metadataResponse.getHttpstatus().toString()).
                        put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                        build()));
            }
            metadataJsonObj.add("keys", responseArray);
            metadataJsonObj.addProperty("offset", offset);
            certListStr = metadataJsonObj.toString();

            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getAllCertificates").
                    put(LogMessage.MESSAGE, "All Certificates fetched from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
        }
        else if (HttpStatus.NOT_FOUND.equals(response.getHttpstatus())) {
            log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                    put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                    put(LogMessage.ACTION, "getAllCertificates").
                    put(LogMessage.MESSAGE, "Retrieved empty certificate list from metadata").
                    put(LogMessage.STATUS, response.getHttpstatus().toString()).
                    put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                    build()));
            return ResponseEntity.status(HttpStatus.OK).body(certListStr);
        }
        log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
                put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER)).
                put(LogMessage.ACTION, "getAllCertificates").
                put(LogMessage.MESSAGE, "Failed to get certificate list from metadata").
                put(LogMessage.STATUS, response.getHttpstatus().toString()).
                put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).
                build()));

        return ResponseEntity.status(response.getHttpstatus()).body(certListStr);
    }
}
