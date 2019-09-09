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

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.catalina.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;

@Component
public class  AWSAuthService {

	@Value("${vault.port}")
	private String vaultPort;

	@Autowired
	private RequestProcessor reqProcessor;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	private static Logger logger = LogManager.getLogger(AWSAuthService.class);
	/**
	 * To authenticate using aws ec2 pkcs7 document and app role
	 * @param login
	 * @return
	 */
	public ResponseEntity<String> authenticateEC2(AWSLogin login){
		String jsonStr = JSONUtil.getJSON(login);
		if(jsonStr.toLowerCase().contains("nonce")){
			return ResponseEntity.badRequest().body("{\"errors\":[\"Not a valid request. Parameter 'nonce' is not expected \"]}");
		}

		String nonce= "";
		try {
			nonce = new ObjectMapper().readTree(jsonStr).at("/pkcs7").toString().substring(1,50);
		} catch (IOException e) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Authenticate EC2").
					put(LogMessage.MESSAGE, String.format("Failed to extract pkcs7 from json [%s]", jsonStr)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.badRequest().body("{\"errors\":[\"Not valid request. Check params \"]}");
		}
		String noncejson = "{\"nonce\":\""+nonce+"\",";
		jsonStr = noncejson + jsonStr.substring(1);

		Response response = reqProcessor.process("/auth/aws/login",jsonStr,"");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * To create an aws app role
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	public ResponseEntity<String> createRole(String token, AWSLoginRole awsLoginRole, UserDetails userDetails) throws TVaultValidationException{
		if (!ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)) {
			//return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String jsonStr = JSONUtil.getJSON(awsLoginRole);

		Response response = reqProcessor.process("/auth/aws/roles/create",jsonStr,token);

		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ // Role created with policies. Need to update SDB metadata too.
			String metadataJson = ControllerUtil.populateAWSMetaJson(awsLoginRole.getRole(), userDetails.getUsername());
			String awsRoleUsermetadataJson = ControllerUtil.populateUserMetaJson(awsLoginRole.getRole(), userDetails.getUsername(), TVaultConstants.AWSROLE_USERS_METADATA_MOUNT_PATH);
			if(ControllerUtil.createMetadata(metadataJson, token)) {
				if (ControllerUtil.createMetadata(awsRoleUsermetadataJson, token)) {
					return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role created \"]}");
				}
				// revert metadata creation
				Response resp = reqProcessor.process("/delete",metadataJson,token);
				if (resp !=null && resp.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
					logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
							put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
							put(LogMessage.ACTION, "Create AWS role").
							put(LogMessage.MESSAGE, "Failed to Create AWS role. metadata update reverted").
							put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
							build()));
				}
			}
			// revert role creation
			Response deleteResponse = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+awsLoginRole.getRole()+"\"}",token);
			if (deleteResponse.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"AWS role creation failed.\"]}");
			}
			else {
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS role created however metadata update failed. Please try with AWS role/update \"]}");
			}

		} else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * Method to update an aws app role.
	 * @param token
	 * @param awsLoginRole
	 * @return
	 */
	public ResponseEntity<String> updateRole(String token, AWSLoginRole awsLoginRole, UserDetails userDetails) throws TVaultValidationException{
		boolean isAllowed = isAllowed(awsLoginRole.getRole(), userDetails, TVaultConstants.UPDATE_OPERATION);
		if (!isAllowed) {
			logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "update AWSRole").
					put(LogMessage.MESSAGE, "Update AWSRole failed").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to update this AWSRole\"]}");
		}
		if (!ControllerUtil.areAWSEC2RoleInputsValid(awsLoginRole)) {
			//return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
			throw new TVaultValidationException("Invalid inputs for the given aws login type");
		}
		String roleName = awsLoginRole.getRole() ;

		AWSLoginRole existingAWSRole = readAWSRoleBasicDetails(roleName, token);
		if (existingAWSRole == null) {
			logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "updateRole").
					put(LogMessage.MESSAGE, String.format("Unable to read AWS role information. AWS role [%s] doesn't exist", roleName)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(HttpStatus.OK).body("{\"errors\":[\"Update failed . AWS Role does not exist\"]}");

		}
		awsLoginRole.setPolicies(existingAWSRole.getPolicies());
		String jsonStr = JSONUtil.getJSON(awsLoginRole);

		Response response = reqProcessor.process("/auth/aws/roles/delete",JSONUtil.getJSON(existingAWSRole),token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			response = reqProcessor.process("/auth/aws/roles/update",jsonStr,token);
			if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role updated \"]}");
			}else{
				return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
			}
		}
		else{
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"messages\":[\"Failed to update the AWS role.\"]}");
		}
	}

	private AWSLoginRole readAWSRoleBasicDetails(String roleName, String token) {
		AWSLoginRole awsLoginRole = null;
		Response awsResponse = reqProcessor.process("/auth/aws/roles","{\"role\":\""+roleName+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(awsResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(awsResponse.getResponse());
			if(responseMap.isEmpty()) {
				logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getAppRole").
						put(LogMessage.MESSAGE, "Reading AWSrole failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return awsLoginRole;
			}
			awsLoginRole = createAWSLgoinRole(roleName, responseMap);
			return awsLoginRole;
		}
		logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "getAWSrole").
				put(LogMessage.MESSAGE, "Reading AWS role failed").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return awsLoginRole;
	}

	private AWSLoginRole createAWSLgoinRole(String roleName, Map<String, Object> responseMap) {
		ArrayList<String> bound_ami_id = (ArrayList<String>)responseMap.get("bound_ami_id");
		ArrayList<String> bound_account_id = (ArrayList<String>)responseMap.get("bound_account_id");
		ArrayList<String> bound_region = (ArrayList<String>)responseMap.get("bound_region");
		ArrayList<String> bound_vpc_id = (ArrayList<String>)responseMap.get("bound_vpc_id");
		ArrayList<String> bound_subnet_id = (ArrayList<String>)responseMap.get("bound_subnet_id");
		ArrayList<String> bound_iam_role_arn = (ArrayList<String>)responseMap.get("bound_iam_role_arn");
		ArrayList<String> bound_iam_instance_profile_arn = (ArrayList<String>)responseMap.get("bound_iam_instance_profile_arn");

		String[] policies = null;
		if (responseMap.get("policies") != null && ((ArrayList<String>)responseMap.get("policies")) != null) {
			ArrayList<String> policiesList = ((ArrayList<String>)responseMap.get("policies"));
			policies = policiesList.toArray(new String[policiesList.size()]);
		}

		AWSLoginRole awsLoginRole = new AWSLoginRole(
				((String)responseMap.get("auth_type")),
				roleName,
				bound_ami_id.toArray(new String[bound_ami_id.size()]),
				bound_account_id.toArray(new String[bound_account_id.size()]),
				bound_region.toArray(new String[bound_region.size()]),
				bound_vpc_id.toArray(new String[bound_vpc_id.size()]),
				bound_subnet_id.toArray(new String[bound_subnet_id.size()]),
				bound_iam_role_arn.toArray(new String[bound_iam_role_arn.size()]),
				bound_iam_instance_profile_arn.toArray(new String[bound_iam_instance_profile_arn.size()]),
				(policies!=null)?policies: new String[0]
		);
		return awsLoginRole;
	}

	/**
	 * Method to delete an existing role.
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> deleteRole(String token, String role, UserDetails userDetails){
		Response permissionResponse = ControllerUtil.canDeleteRole(role, token, userDetails, TVaultConstants.AWSROLE_METADATA_MOUNT_PATH);
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(permissionResponse.getHttpstatus()) || HttpStatus.UNAUTHORIZED.equals(permissionResponse.getHttpstatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\""+permissionResponse.getResponse()+"\"]}");
		}
		AWSRoleMetadata awsRoleMetadata = readAWSRoleMetadata(token, role);
		String awsroleCreatedBy = userDetails.getUsername();
		if ( awsRoleMetadata.getAwsRoleMetadataDetails() != null) {
			awsroleCreatedBy = awsRoleMetadata.getAwsRoleMetadataDetails().getCreatedBy();
		}
		Response response = reqProcessor.process("/auth/aws/roles/delete","{\"role\":\""+role+"\"}",token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			// delete metadata
			String metaJson = ControllerUtil.populateAWSMetaJson(role, userDetails.getUsername());
			Response resp = reqProcessor.process("/delete",metaJson,token);
			String appRoleUsermetadataJson = ControllerUtil.populateUserMetaJson(role,awsroleCreatedBy, TVaultConstants.AWSROLE_USERS_METADATA_MOUNT_PATH);
			Response appRoleUserMetaDataDeletionResponse = reqProcessor.process("/delete",appRoleUsermetadataJson,token);

			if (HttpStatus.NO_CONTENT.equals(resp.getHttpstatus()) && HttpStatus.NO_CONTENT.equals(appRoleUserMetaDataDeletionResponse.getHttpstatus())) {
				logger.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "Delete AWS Role").
						put(LogMessage.MESSAGE, "Metadata deleted").
						put(LogMessage.STATUS, response.getHttpstatus().toString()).
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role deleted \"]}");
			}
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Role deleted, metadata delete failed\"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * Method to fetch information for an aws approle.
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> fetchRole(String token, String role, UserDetails userDetails){
		boolean isAllowed = isAllowed(role, userDetails, TVaultConstants.READ_OPERATION);
		if (isAllowed) {
			String jsoninput= "{\"role\":\""+role+"\"}";
			Response response = reqProcessor.process("/auth/aws/roles",jsoninput,token);
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
		logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "read AWSRole").
				put(LogMessage.MESSAGE, "Reading AWSRole details failed").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: You don't have enough permission to read this AWSRole details\"]}");
	}
	/**
	 * To get list of AWS Roles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listRoles(String token, UserDetails userDetails){
		Response response = null;
		String _path = TVaultConstants.AWSROLE_USERS_METADATA_MOUNT_PATH + "/" + userDetails.getUsername();
		if (userDetails.isAdmin()) {
			response = reqProcessor.process("/auth/aws/roles/list","{}",token);
		}
		else {
			response = reqProcessor.process("/auth/awsroles/rolesbyuser/list","{\"path\":\""+_path+"\"}",userDetails.getSelfSupportToken());
		}
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());	
	}
	
	
	/**
	 * Configures the credentials required to perform API calls to AWS as well as custom endpoints to talk to AWS APIs.
	 * @param awsClientConfiguration
	 * @return
	 */
	public ResponseEntity<String> configureClient(AWSClientConfiguration awsClientConfiguration, String token){
		String jsonStr = JSONUtil.getJSON(awsClientConfiguration);
		Response response = reqProcessor.process("/auth/aws/config/configureclient",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"AWS Client successfully configured \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	/**
	 * Returns the configured AWS client access credentials.
	 * @param awsClientConfiguration
	 * @return
	 */
	public ResponseEntity<String> readClientConfiguration(String token){
		Response response = reqProcessor.process("/auth/aws/config/readclientconfig","{}", token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Allows the explicit association of STS roles to satellite AWS accounts
	 * @param awsStsRole
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> createSTSRole(AWSStsRole awsStsRole, String token){
		String jsonStr = JSONUtil.getJSON(awsStsRole);
		Response response = reqProcessor.process("/auth/aws/config/sts/create",jsonStr, token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT)){ 
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"STS Role created successfully \"]}");
		}else{
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}
	
	/**
	 * Logs in using IAM credentials
	 * @param awsiamLogin
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> authenticateIAM(AWSIAMLogin awsiamLogin){
		String jsonStr = JSONUtil.getJSON(awsiamLogin);
		Response response = reqProcessor.process("/auth/aws/iam/login",jsonStr,"");
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
	/**
	 * 
	 * @param authType
	 * @param awsAuthLogin
	 * @return
	 */
	public ResponseEntity<String> authenticate(AWSAuthType authType, AWSAuthLogin awsAuthLogin){
		if (!ControllerUtil.areAwsLoginInputsValid(authType, awsAuthLogin)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid inputs for the given aws login type");
		}
		if (AWSAuthType.EC2.equals(authType) ) {
			AWSLogin login = generateAWSEC2Login(awsAuthLogin);
			return authenticateEC2(login);
		}
		else if (AWSAuthType.IAM.equals(authType)) {
			AWSIAMLogin awsiamLogin = generateIAMLogin(awsAuthLogin);
			return authenticateIAM(awsiamLogin);
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid Authentication type. Authentication type has to be either ec2 or iam");
		}
	}
	/**
	 * 
	 * @param awsAuthLogin
	 * @return
	 */
	private AWSLogin generateAWSEC2Login(AWSAuthLogin awsAuthLogin) {
		AWSLogin login = new AWSLogin();
		login.setPkcs7(awsAuthLogin.getPkcs7());
		login.setRole(awsAuthLogin.getRole());
		return login;
	}
	/**
	 * 
	 * @param awsAuthLogin
	 * @return
	 */
	private AWSIAMLogin generateIAMLogin(AWSAuthLogin awsAuthLogin) {
		AWSIAMLogin awsiamLogin  = new AWSIAMLogin();
		awsiamLogin.setIam_http_request_method(awsAuthLogin.getIam_http_request_method());
		awsiamLogin.setIam_request_body(awsAuthLogin.getIam_request_body());
		awsiamLogin.setIam_request_headers(awsAuthLogin.getIam_request_headers());
		awsiamLogin.setIam_request_url(awsAuthLogin.getIam_request_url());
		return awsiamLogin;
	}

	/**
	 * To configure AWS role
	 * @param roleName
	 * @param policies
	 * @param token
	 * @return
	 */
	public Response configureAWSRole(String roleName,String policies,String token ){
		ObjectMapper objMapper = new ObjectMapper();
		Map<String,String>configureRoleMap = new HashMap<String,String>();
		configureRoleMap.put("role", roleName);
		configureRoleMap.put("policies", policies);
		String awsConfigJson ="";
		try {
			awsConfigJson = objMapper.writeValueAsString(configureRoleMap);
		} catch (JsonProcessingException e) {
			logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "configureAWSRole").
					put(LogMessage.MESSAGE, String.format ("Unable to create awsConfigJson [%s] with roleName [%s] policies [%s] ", e.getMessage(), roleName, policies)).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return reqProcessor.process("/auth/aws/roles/update",awsConfigJson,token);
	}

	/**
	 * Checks whether given AWSRole operation is allowed for a given user based on the ownership of the AWSRole
	 * @param userDetails
	 * @param rolename
	 * @return
	 */
	public boolean isAllowed(String rolename, UserDetails userDetails, String operation) {
		boolean isAllowed = false;
		if (userDetails.isAdmin()) {
			// As an admin, I can read, delete, update anybody's AWSRole
			isAllowed = true;
		}
		else {
			AWSRoleMetadata awsRoleMetadata = readAWSRoleMetadata(userDetails.getSelfSupportToken(), rolename);
			String awsRoleOwner = null;
			if (awsRoleMetadata != null && awsRoleMetadata.getAwsRoleMetadataDetails() != null) {
				awsRoleOwner = awsRoleMetadata.getAwsRoleMetadataDetails().getCreatedBy();
			}
			if (Objects.equals(userDetails.getUsername(), awsRoleOwner)) {
				if (TVaultConstants.READ_OPERATION.equals(operation)
						|| TVaultConstants.DELETE_OPERATION.equals(operation)
						|| TVaultConstants.UPDATE_OPERATION.equals(operation)
						) {
					// As a owner of the AWSRole, I can read, delete, update his AWSRole
					isAllowed = true;
				}

			}
		}
		return isAllowed;
	}

	/**
	 * Reads the metadata associated with an AWS role
	 * @param token
	 * @param rolename
	 * @return AWSRoleMetadata
	 */
	public AWSRoleMetadata readAWSRoleMetadata(String token, String rolename) {
		AWSRoleMetadata awsRoleMetadata = null;
		String _path = TVaultConstants.AWSROLE_METADATA_MOUNT_PATH + "/" + rolename;
		Response readResponse = reqProcessor.process("/read","{\"path\":\""+_path+"\"}",token);
		Map<String, Object> responseMap = null;
		if(HttpStatus.OK.equals(readResponse.getHttpstatus())) {
			responseMap = ControllerUtil.parseJson(readResponse.getResponse());
			if(responseMap.isEmpty()) {
				logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
						put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
						put(LogMessage.ACTION, "getMetaDataForAWSRole").
						put(LogMessage.MESSAGE, "Reading Metadata for AWSRole failed").
						put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
						build()));
				return awsRoleMetadata;
			}

			Map<String,Object> appRoleMetadataMap = (Map<String,Object>) responseMap.get("data");
			if (appRoleMetadataMap != null) {
				awsRoleMetadata = new AWSRoleMetadata();
				AWSRoleMetadataDetails awsRoleMetadataDetails = new AWSRoleMetadataDetails();
				awsRoleMetadataDetails.setCreatedBy((String)appRoleMetadataMap.get("createdBy"));
				awsRoleMetadataDetails.setName(rolename);
				awsRoleMetadata.setAwsRoleMetadataDetails(awsRoleMetadataDetails);
				awsRoleMetadata.setPath(_path);
			}
			return awsRoleMetadata;
		}
		else if (HttpStatus.NOT_FOUND.equals(readResponse.getHttpstatus())) {
			logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getMetaDataForAWSRole").
					put(LogMessage.MESSAGE, "Reading Metadata for AWSRole failed. AWSRole Not found.").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return awsRoleMetadata;
		}
		logger.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "getMetaDataForAWSRole").
				put(LogMessage.MESSAGE, "Reading Metadata for AWSRole failed").
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));
		return awsRoleMetadata;
	}
}
