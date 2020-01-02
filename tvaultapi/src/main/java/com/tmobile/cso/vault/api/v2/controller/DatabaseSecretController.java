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

import com.tmobile.cso.vault.api.model.DatabaseRole;
import com.tmobile.cso.vault.api.model.DatabaseStaticRole;
import com.tmobile.cso.vault.api.service.DatabaseSecretService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;


@RestController
@CrossOrigin
@Api(description = "Manage AWS Secrets", position = 13)
public class DatabaseSecretController {
	
	@Autowired
	DatabaseSecretService databaseSecretService;

	/**
	 * Read MySql database temporary credentials.
	 * @param token
	 * @param role_name
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.getTemporaryCredentials.value}", notes = "${DatabaseSecretController.getTemporaryCredentials.notes}")
	@GetMapping(value="/v2/database/role/credentials/{role_name}",produces="application/json")
	public ResponseEntity<String> getTemporaryCredentials(@RequestHeader(value="vault-token") String token, @PathVariable String role_name){
		return databaseSecretService.getTemporaryCredentials(role_name, token);
	}

	/**
	 * Create database role
	 * @param token
	 * @param databaseRole
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.createRole.value}", notes = "${DatabaseSecretController.createRole.notes}")
	@PostMapping(value="/v2/database/roles", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody DatabaseRole databaseRole){
		return databaseSecretService.createRole(token, databaseRole);
	}

	/**
	 * Create static database role
	 * @param request
	 * @param token
	 * @param databaseRole
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.createStaticRole.value}", notes = "${DatabaseSecretController.createStaticRole.notes}")
	@PostMapping(value="/v2/database/static-roles", consumes="application/json", produces="application/json")
	public ResponseEntity<String> createStaticRole(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody DatabaseStaticRole databaseRole){
		return databaseSecretService.createStaticRole(token, databaseRole);
	}

	/**
	 * Get static credentials
	 * @param token
	 * @param role_name
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.getStaticCredentials.value}", notes = "${DatabaseSecretController.getStaticCredentials.notes}")
	@GetMapping(value="/v2/database/static-creds/{role_name}",produces="application/json")
	public ResponseEntity<String> getStaticCredentials(@RequestHeader(value="vault-token") String token, @PathVariable String role_name){
		return databaseSecretService.getStaticCredentials(role_name, token);
	}

	/**
	 * Read database role
	 * @param token
	 * @param role_name
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.readRole.value}", notes = "${DatabaseSecretController.readRole.notes}")
	@GetMapping(value="/v2/database/roles/{role_name}",produces="application/json")
	public ResponseEntity<String> readRole(@RequestHeader(value="vault-token") String token, @PathVariable String role_name){
		return databaseSecretService.readRole(role_name, token);
	}

	/**
	 * List database roles
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.listRoles.value}", notes = "${DatabaseSecretController.listRoles.notes}")
	@GetMapping(value="/v2/database/roles/",produces="application/json")
	public ResponseEntity<String> listRoles(@RequestHeader(value="vault-token") String token){
		return databaseSecretService.listRoles(token);
	}

	/**
	 * Delete database role
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.deleteRole.value}", notes = "${DatabaseSecretController.deleteRole.notes}")
	@DeleteMapping(value="/v2/database/roles/{role_name}",produces="application/json")
	public ResponseEntity<String> deleteRole(@RequestHeader(value="vault-token") String token, @PathVariable String role_name){
		return databaseSecretService.deleteRole(role_name, token);
	}

	/**
	 * Read static role
	 * @param token
	 * @param role_name
	 * @return
	 */
	@ApiOperation(value = "${DatabaseSecretController.readStaticRole.value}", notes = "${DatabaseSecretController.readStaticRole.notes}")
	@GetMapping(value="/v2/database/static-roles/{role_name}",produces="application/json")
	public ResponseEntity<String> readStaticRole(@RequestHeader(value="vault-token") String token, @PathVariable String role_name){
		return databaseSecretService.readStaticRole(role_name, token);
	}
}
