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
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.DatabaseRole;
import com.tmobile.cso.vault.api.model.DatabaseStaticRole;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSecretService {

	@Autowired
	private RequestProcessor reqProcessor;

	private static Logger log = LogManager.getLogger(DatabaseSecretService.class);

	/**
	 * Read MySql database temporary credentials.
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getTemporaryCredentials(String role_name, String token) {
		Response response = reqProcessor.process("/database/creds/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Create database dynamic role
	 * @param token
	 * @param databaseRole
	 * @return
	 */
	public ResponseEntity<String> createRole(String token, DatabaseRole databaseRole) {
		String jsonStr = JSONUtil.getJSON(databaseRole);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Create database role").
				put(LogMessage.MESSAGE, String.format("Trying to create database role [%s]", jsonStr)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));

		Response response = reqProcessor.process("/database/roles/create/", jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT) || response.getHttpstatus().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Database role created successfully\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Create database role").
					put(LogMessage.MESSAGE, "Creation of database role failed").
					put(LogMessage.RESPONSE, response.getResponse()).
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}


	/**
	 * Create static database role
	 * @param token
	 * @param databaseRole
	 * @return
	 */
	public ResponseEntity<String> createStaticRole(String token, DatabaseStaticRole databaseRole) {
		String jsonStr = JSONUtil.getJSON(databaseRole);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
				put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
				put(LogMessage.ACTION, "Create static database role").
				put(LogMessage.MESSAGE, String.format("Trying to create static database role [%s]", jsonStr)).
				put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
				build()));

		Response response = reqProcessor.process("/database/static-roles/create/", jsonStr,token);
		if(response.getHttpstatus().equals(HttpStatus.NO_CONTENT) || response.getHttpstatus().equals(HttpStatus.OK)) {
			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"Static database role created successfully\"]}");
		}
		else {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "Create static database role").
					put(LogMessage.MESSAGE, "Creation of static database role failed").
					put(LogMessage.RESPONSE, response.getResponse()).
					put(LogMessage.STATUS, response.getHttpstatus().toString()).
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
			return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
		}
	}

	/**
	 * Get static credentials
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> getStaticCredentials(String role_name, String token) {
		Response response = reqProcessor.process("/database/static-creds/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Read database role
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> readRole(String role_name, String token) {
		Response response = reqProcessor.process("/database/roles/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * List database roles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listRoles(String token) {
		Response response = reqProcessor.process("/database/roles/list/","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Delete database role
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> deleteRole(String role_name, String token) {
		Response response = reqProcessor.process("/database/roles/delete/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Read static role
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> readStaticRole(String role_name, String token) {
		Response response = reqProcessor.process("/database/static-roles/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * List static database roles
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> listStaticRoles(String token) {
		Response response = reqProcessor.process("/database/static-roles/list/","{}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}

	/**
	 * Delete static database role
	 * @param role_name
	 * @param token
	 * @return
	 */
	public ResponseEntity<String> deleteStaticRole(String role_name, String token) {
		Response response = reqProcessor.process("/database/static-roles/delete/","{\"role_name\":\""+role_name+"\"}",token);
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());
	}
}
