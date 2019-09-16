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

import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.*;
import com.tmobile.cso.vault.api.utils.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.TVaultValidationException;


@Component
public class  SelfSupportService {
	
	@Autowired
	private SafesService safesService;
	
	@Autowired
	private PolicyUtils policyUtils;
	
	@Autowired
	private AuthorizationUtils authorizationUtils;
	
	@Autowired
	private SafeUtils safeUtils;

	@Autowired
	private AWSAuthService awsAuthService;

	@Autowired
	private AWSIAMAuthService awsiamAuthService;

	@Autowired
	private AppRoleService appRoleService;

	@Value("${vault.auth.method}")
	private String vaultAuthMethod;

	@Value("${safe.quota:20}")
	private String safeQuota;

	private static Logger log = LogManager.getLogger(SelfSupportService.class);

	/**
	 * Creates a safe by the user with least privileges, Requires an AppRole which can perform Safe Creation 
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param token
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> createSafe(UserDetails userDetails, String userToken, Safe safe) {
		
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.createSafe(token, safe);
		}
		else {
			// Assign the owner (Infer from logged in user?)
			// Create proper policies
			// Assign the policies
			// Modify should work the same
			// Delete safe - clean up of all items, paths, permissions, policies
			token = userDetails.getSelfSupportToken();
			if (!isSafeValid(safe)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
			}
			// check the user safe limit
			if (isSafeQuotaReached(token, userDetails.getUsername(), ControllerUtil.getSafeType(safe.getPath()))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"You have reached the limit of number of allowed safes that can be created\"]}");
			}
			if (safe != null && safe.getSafeBasicDetails() != null) {
				safe.getSafeBasicDetails().setOwnerid(userDetails.getUsername());
			}
			ResponseEntity<String> safe_creation_response = safesService.createSafe(token, safe);
			if (HttpStatus.OK.equals(safe_creation_response.getStatusCode() )) {
				// Associate admin user to the safe...
				SafeUser safeUser = new SafeUser();
				safeUser.setAccess(TVaultConstants.SUDO_POLICY);
				safeUser.setPath(safe.getPath());
				safeUser.setUsername(userDetails.getUsername());
				safesService.addUserToSafe(token, safeUser, null);
			}
			return safe_creation_response;
		}
	}

	/**
	 * Check whether the safe info in request is valid
	 * @param safe
	 * @return
	 */
	private boolean isSafeValid(Safe safe) {
		return safe != null && safe.getPath() != null && safe.getPath() != "";
	}

	/**
	 * Check whether the user safe limit reached
	 * @param token
	 * @param username
	 * @param path
	 * @return
	 */
	private boolean isSafeQuotaReached(String token, String username, String path) {
		String[] policies = policyUtils.getCurrentPolicies(token, username);
		String[] safes = safeUtils.getManagedSafes(policies, path);
		if (safes.length >= Integer.parseInt(safeQuota)) {
			return true;
		}
		return false;
	}


	/**
	 * Adds a user to a safe
	 * @param userDetails
	 * @param userToken
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> addUserToSafe(UserDetails userDetails, String userToken, SafeUser safeUser) {
		boolean canAddUser = safeUtils.canAddOrRemoveUser(userDetails, safeUser, TVaultConstants.ADD_USER);
		if (canAddUser) {
			if (userDetails.isAdmin()) {
				return safesService.addUserToSafe(userDetails.getClientToken(), safeUser, userDetails);
			}
			else {
				return safesService.addUserToSafe(userDetails.getSelfSupportToken(), safeUser, userDetails);
			}
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to add users to this safe\"]}");
		}
	}
	/**
	 * Get SDB Info
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getInfo(UserDetails userDetails, String userToken, String path){
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.getInfo(token, path);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to get this safe info\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.getInfo(token, path);
		}
	}
	/**
	 * Gets safe information as power user
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getSafe(UserDetails userDetails, String userToken, String path) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.getSafe(token, path);
		}
		else {
			String safeType = ControllerUtil.getSafeType(path);
			String safeName = ControllerUtil.getSafeName(path);
			if (StringUtils.isEmpty(safeType) || StringUtils.isEmpty(safeName)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path specified\"]}");
			}
			String powerToken = userDetails.getSelfSupportToken();
			String username = userDetails.getUsername();
			Safe safeMetaData = safeUtils.getSafeMetaData(powerToken, safeType, safeName);
			String[] latestPolicies = policyUtils.getCurrentPolicies(powerToken, username);
			ArrayList<String> policiesTobeChecked =  policyUtils.getPoliciesTobeCheked(safeType, safeName);
			boolean isAuthorized = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, false);
			if (isAuthorized) {
				token = userDetails.getSelfSupportToken();
				return safesService.getSafe(token, path);
			}
			else {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"errors\":[\"Access denied: no permission to read this safe information\"]}");
			}
		}
	}
	/**
	 * Removes user from safe as PowerUser
	 * @param token
	 * @param safeUser
	 * @return
	 */
	public ResponseEntity<String> removeUserFromSafe(UserDetails userDetails, String userToken, SafeUser safeUser) {
		String token = userDetails.getClientToken();
		boolean isAuthorized = safeUtils.canAddOrRemoveUser(userDetails, safeUser, TVaultConstants.REMOVE_USER);
		if (!isAuthorized) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"errors\":[\"Access denied: no permission to remove users from this safe\"]}");
		}
		if (userDetails.isAdmin()) {
			return safesService.removeUserFromSafe(token, safeUser);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return safesService.removeUserFromSafe(token, safeUser);
		}
	}
	/**
	 * Read from safe Recursively
	 * @param token
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> getFoldersRecursively(UserDetails userDetails, String userToken, String path) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.getFoldersRecursively(token, path);
		}
		else {
			// List of safes based on current user
			String[] policies = policyUtils.getCurrentPolicies(userDetails.getSelfSupportToken(), userDetails.getUsername());
			String[] safes = safeUtils.getManagedSafes(policies, path);
			Map<String, String[]> safesMap = new HashMap<String, String[]>();
			safesMap.put("keys", safes);
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safesMap));
		}
	}
	/**
	 * isAuthorized
	 * @param token
	 * @param safeName
	 * @return
	 */
	public ResponseEntity<String> isAuthorized (UserDetails userDetails, String path) {
		if (!ControllerUtil.isPathValid(path)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path specified\"]}");
		}
		String safeType = ControllerUtil.getSafeType(path);
		String safeName = ControllerUtil.getSafeName(path);
		if (StringUtils.isEmpty(safeType) || StringUtils.isEmpty(safeName)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path specified\"]}");
		}
		String powerToken = userDetails.getSelfSupportToken();
		String username = userDetails.getUsername();
		Safe safeMetaData = safeUtils.getSafeMetaData(powerToken, safeType, safeName);
		if (safeMetaData == null) {
			return ResponseEntity.status(HttpStatus.OK).body("false");
		}
		String[] latestPolicies = policyUtils.getCurrentPolicies(powerToken, username);
		ArrayList<String> policiesTobeChecked =  policyUtils.getPoliciesTobeCheked(safeType, safeName);
		boolean isAuthorized = authorizationUtils.isAuthorized(userDetails, safeMetaData, latestPolicies, policiesTobeChecked, false);
		return ResponseEntity.status(HttpStatus.OK).body(String.valueOf(isAuthorized));
	}

	/**
	 * Update a safe by the user with least privileges, Requires an AppRole which can perform Safe updation
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param userToken
	 * @param safe
	 * @return
	 */
	public ResponseEntity<String> updateSafe(UserDetails userDetails, String userToken, Safe safe) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.updateSafe(token, safe);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, safe.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to update this safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			ResponseEntity<String> safe_creation_response = safesService.updateSafe(token, safe);
			return safe_creation_response;
		}
	}
	/**
	 * Delete a safe by the user with least privileges, Requires an AppRole which can perform Safe Deletion
	 * (Sufficient access to the paths such as shared or metadata/shared, etc)
	 * @param userDetails
	 * @param userToken
	 * @param path
	 * @return
	 */
	public ResponseEntity<String> deletefolder(UserDetails userDetails, String userToken, String path) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.deletefolder(token, path);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to delete this safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			ResponseEntity<String> safe_creation_response = safesService.deletefolder(token, path);
			return safe_creation_response;
		}
	}
	/**
	 * Adds a group to a safe
	 * @param userDetails
	 * @param userToken
	 * @param safeGroup
	 * @return
	 */
	public ResponseEntity<String> addGroupToSafe(UserDetails userDetails, String userToken, SafeGroup safeGroup) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.addGroupToSafe(token, safeGroup);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, safeGroup.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add group to the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.addGroupToSafe(token, safeGroup);
		}
	}
	/**
	 * Removes a group from safe
	 * @param userDetails
	 * @param userToken
	 * @param safeGroup
	 * @return
	 */
	public ResponseEntity<String> removeGroupFromSafe(UserDetails userDetails, String userToken, SafeGroup safeGroup) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.removeGroupFromSafe(token, safeGroup);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, safeGroup.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove group from the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.removeGroupFromSafe(token, safeGroup);
		}
	}

	/**
	 * Add AWS role to safe
	 * @param userDetails
	 * @param userToken
	 * @param awsRole
	 * @return
	 */
	public ResponseEntity<String> addAwsRoleToSafe(UserDetails userDetails, String userToken, AWSRole awsRole) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.addAwsRoleToSafe(token, awsRole);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, awsRole.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add AWS role to the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.addAwsRoleToSafe(token, awsRole);
		}
	}

	/**
	 * Remove or detach AWS role from safe
	 * @param userDetails
	 * @param userToken
	 * @param awsRole
	 * @param detachOnly
	 * @return
	 */
	public ResponseEntity<String> removeAWSRoleFromSafe(UserDetails userDetails, String userToken, AWSRole awsRole, boolean detachOnly) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.removeAWSRoleFromSafe(token, awsRole, detachOnly, userDetails);
		}
		else {
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, awsRole.getPath());
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove AWS role from the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.removeAWSRoleFromSafe(token, awsRole, detachOnly, userDetails);
		}
	}

	/**
	 * Associate approle to safe
	 * @param userDetails
	 * @param token
	 * @param jsonstr
	 * @return
	 */
	public ResponseEntity<String> associateApproletoSDB(UserDetails userDetails, String userToken, SafeAppRoleAccess safeAppRoleAccess) {
		String jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.associateApproletoSDB(token, safeAppRoleAccess);
		}
		else {
			Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
			if(!ControllerUtil.areSafeAppRoleInputsValid(requestMap)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
			}
			String path = requestMap.get("path").toString();
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to add Approle to the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.associateApproletoSDB(token, safeAppRoleAccess);
		}
	}

	/**
	 * Delete app role from safe
	 * @param userDetails
	 * @param userToken
	 * @param jsonstr
	 * @return
	 */
	public ResponseEntity<String> deleteApproleFromSDB(UserDetails userDetails, String userToken, SafeAppRoleAccess safeAppRoleAccess) {
		if (TVaultConstants.SELF_SERVICE_APPROLE_NAME.equals(safeAppRoleAccess.getRole_name())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Access denied: no permission to delete this approle\"]}");
		}
		String jsonstr = JSONUtil.getJSON(safeAppRoleAccess);
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return safesService.removeApproleFromSafe(token, jsonstr);
		}
		else {
			Map<String,Object> requestMap = ControllerUtil.parseJson(jsonstr);
			if (ObjectUtils.isEmpty(requestMap.get("role_name")) || ObjectUtils.isEmpty(requestMap.get("path"))) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid role name or path\"]}");
			}
			String path = requestMap.get("path").toString();
			ResponseEntity<String> isAuthorized = isAuthorized(userDetails, path);
			if (!isAuthorized.getStatusCode().equals(HttpStatus.OK)) {
				return isAuthorized.getStatusCode().equals(HttpStatus.BAD_REQUEST)?isAuthorized:ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"errors\":[\"Error checking user permission\"]}");
			}
			if (isAuthorized.getBody().equals(TVaultConstants.FALSE)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{\"errors\":[\"Access denied: no permission to remove approle from the safe\"]}");
			}
			token = userDetails.getSelfSupportToken();
			return safesService.removeApproleFromSafe(token, jsonstr);
		}
	}

	/**
	 * Create aws role
	 * @param userDetails
	 * @param userToken
	 * @param awsLoginRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createRole(UserDetails userDetails, String userToken, AWSLoginRole awsLoginRole) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsAuthService.createRole(token, awsLoginRole, userDetails);
		}
		else {
			return awsAuthService.createRole(userDetails.getSelfSupportToken(), awsLoginRole, userDetails);
		}
	}

	/**
	 * Update aws role
	 * @param userDetails
	 * @param userToken
	 * @param awsLoginRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> updateRole(UserDetails userDetails, String userToken, AWSLoginRole awsLoginRole) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsAuthService.updateRole(token, awsLoginRole, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return awsAuthService.updateRole(token, awsLoginRole, userDetails);
		}
	}

	/**
	 * Create aws iam role
	 * @param userDetails
	 * @param userToken
	 * @param awsiamRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> createIAMRole(UserDetails userDetails, String userToken, AWSIAMRole awsiamRole) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsiamAuthService.createIAMRole(awsiamRole, token, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return awsiamAuthService.createIAMRole(awsiamRole, token, userDetails);
		}
	}

	/**
	 * Update aws iam role
	 * @param userDetails
	 * @param userToken
	 * @param awsiamRole
	 * @param path
	 * @return
	 * @throws TVaultValidationException
	 */
	public ResponseEntity<String> updateIAMRole(UserDetails userDetails, String userToken, AWSIAMRole awsiamRole) throws TVaultValidationException {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return awsiamAuthService.updateIAMRole(token, awsiamRole, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return awsiamAuthService.updateIAMRole(token, awsiamRole, userDetails);
		}
	}


	/**
	 * To get list of AWS Roles
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> listRoles(String token, UserDetails userDetails) {
		return awsAuthService.listRoles(token, userDetails);
	}

	/**
	 * To fetch information for an awsrole.
	 * @param token
	 * @param role
	 * @return
	 */
	public ResponseEntity<String> fetchRole(String token, String role, UserDetails userDetails){
		if (userDetails.isAdmin()) {
			return awsAuthService.fetchRole(token, role, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return awsAuthService.fetchRole(token, role, userDetails);
		}
	}

	/**
	 * Gets the map of all existing safe names
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getAllSafeNames(UserDetails userDetails) {
		HashMap<String, List<String>> safeNames = ControllerUtil.getAllExistingSafeNames(userDetails.getSelfSupportToken());
		if (MapUtils.isEmpty(safeNames)) {
			return ResponseEntity.status(HttpStatus.OK).body("No safes are available");
		}
		else {
			return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeNames));
		}
	}

	/**
	 * Create AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> createAppRole(String userToken, AppRole appRole, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.createAppRole(token, appRole, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.createAppRole(token, appRole, userDetails);
		}
	}

	/**
	 * Delete AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> deleteAppRole(String userToken, AppRole appRole, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.deleteAppRole(token, appRole, userDetails);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.deleteAppRole(token, appRole, userDetails);
		}
	}
	
	/**
	 * Get safes having read/write permission
	 * @param userDetails
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getSafes(UserDetails userDetails, String userToken) {
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		String[] policies = policyUtils.getCurrentPolicies(token, userDetails.getUsername());

		List<Map<String, String>> safeListUsers = new ArrayList<>();
		List<Map<String, String>> safeListShared = new ArrayList<>();
		List<Map<String, String>> safeListApps = new ArrayList<>();
		Map<String, List<Map<String, String>>> safeList = new HashMap<>();
		if (policies != null) {
			for (String policy: policies) {
				Map<String, String> safePolicy = new HashMap<>();
				String[] _policies = policy.split("_", -1);
				if (_policies.length >= 3) {
					String[] policyName = Arrays.copyOfRange(_policies, 2, _policies.length);
					String safeName = String.join("_", policyName);
					String safeType = _policies[1];

					if (policy.startsWith("r_")) {
						safePolicy.put(safeName, "read");
					} else if (policy.startsWith("w_")) {
						safePolicy.put(safeName, "write");
					}
					else if (policy.startsWith("d_")) {
						safePolicy.put(safeName, "deny");
					}
					if (!safePolicy.isEmpty()) {
						if (safeType.equals(TVaultConstants.USERS)) {
							safeListUsers.add(safePolicy);
						} else if (safeType.equals(TVaultConstants.SHARED)) {
							safeListShared.add(safePolicy);
						} else if (safeType.equals(TVaultConstants.APPS)) {
							safeListApps.add(safePolicy);
						}
					}
				}
			}
			safeList.put(TVaultConstants.USERS, safeListUsers);
			safeList.put(TVaultConstants.SHARED, safeListShared);
			safeList.put(TVaultConstants.APPS, safeListApps);
		}
		return ResponseEntity.status(HttpStatus.OK).body(JSONUtil.getJSON(safeList));
	}

	/**
	 * Read all approle names
	 * @param userToken
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoles(String userToken, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.readAppRoles(token);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.readAppRoles(token);
		}
	}
	
	/**
	 * List all approle names
	 * @param userToken
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> listAppRoles(String userToken, UserDetails userDetails) {
		return appRoleService.listAppRoles(userToken, userDetails);
	}

	/**
	 * Read approle info
	 * @param userToken
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRole(String userToken, String rolename, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (userDetails.isAdmin()) {
			return appRoleService.readAppRole(token, rolename);
		}
		else {
			token = userDetails.getSelfSupportToken();
			return appRoleService.readAppRole(token, rolename);
		}
	}
	/**
	 * Read role_id for a given AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoleRoleId(String userToken, String rolename, UserDetails userDetails) {
		return appRoleService.readAppRoleRoleId(userToken, rolename, userDetails);
	}
	/**
	 * Read secret_id for a given AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoleSecretId(String userToken, String rolename, UserDetails userDetails) {
		String token = userDetails.getClientToken();
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		return appRoleService.readAppRoleSecretId(token, rolename, userDetails);
	}
	/**
	 * Read approle details
	 * @param userToken
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readAppRoleDetails(String userToken, String rolename, UserDetails userDetails) {
		return appRoleService.readAppRoleDetails(userToken, rolename, userDetails);
	}
	/**
	 * To get/read Accessors of all SecretIds issued against an AppRole
	 * @param userToken
	 * @param rolename
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> readSecretIdAccessors(String userToken, String rolename, UserDetails userDetails) {
		return appRoleService.readSecretIdAccessors(userToken, rolename, userDetails);
	}
	/**
	 * Delete SecretIds for a given AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> deleteSecretIds(String userToken, AppRoleAccessorIds appRoleAccessorIds, UserDetails userDetails) {
		return appRoleService.deleteSecretIds(userToken, appRoleAccessorIds, userDetails);
	}
	/**
	 * Update AppRole
	 * @param userToken
	 * @param appRole
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> updateAppRole(String userToken, AppRole appRole, UserDetails userDetails) {
		return appRoleService.updateAppRole(userToken, appRole, userDetails);
	}

	public ResponseEntity<String> deleteRole(String token, String role, UserDetails userDetails) {
		if (!userDetails.isAdmin()) {
			token = userDetails.getSelfSupportToken();
		}
		return awsAuthService.deleteRole(token, role, userDetails);
	}
}
