// =========================================================================
// Copyright 2020 T-Mobile, US
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

import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.service.CertificateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@Api( description = "Get Workload details", position = 14)
public class CertificateController {

	@Autowired
	private CertificateService certificateService;

	/**
	 * To get list of certificates in a container
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${WorkloadDetailsController.getApprolesFromCwm.value}", notes = "${WorkloadDetailsController.getApprolesFromCwm.notes}")
	@GetMapping(value="/v2/nclm/certificates", produces="application/json")
	public ResponseEntity<String> getCertificates(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam(name="freeText", required = false) String freeText, @RequestParam(name = "limit", required = false) String limit, @RequestParam(name = "offset", required = false) String offset){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return certificateService.getCertificates(token, userDetails, freeText, limit, offset);
	}

	/**
	 * To get list of target systems in a container
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${WorkloadDetailsController.getApprolesFromCwm.value}", notes = "${WorkloadDetailsController.getApprolesFromCwm.notes}")
	@GetMapping(value="/v2/nclm/targetsystems", produces="application/json")
	public ResponseEntity<String> getTargetSystems(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam(name="freeText", required = false) String freeText){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return certificateService.getTargetSystems(token, userDetails, freeText);
	}

	/**
	 * To get list of services in a target system
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${WorkloadDetailsController.getApprolesFromCwm.value}", notes = "${WorkloadDetailsController.getApprolesFromCwm.notes}")
	@GetMapping(value="/v2/nclm/services", produces="application/json")
	public ResponseEntity<String> getServices(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam(name="freeText", required = false) String freeText, @RequestParam("targetSystemId") String targetSystemId){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return certificateService.getServices(token, userDetails, targetSystemId, freeText);
	}

	/**
	 * To get list of certificates in a target system service
	 * @param request
	 * @param token
	 * @return
	 */
	@ApiOperation(value = "${WorkloadDetailsController.getApprolesFromCwm.value}", notes = "${WorkloadDetailsController.getApprolesFromCwm.notes}")
	@GetMapping(value="/v2/nclm/services/certificates", produces="application/json")
	public ResponseEntity<String> getServiceCertificates(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @RequestParam(name="freeText", required = false) String freeText, @RequestParam("targetSystemId") String targetSystemId, @RequestParam("targetSystemServiceId") String targetSystemServiceId){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return certificateService.getServiceCertificates(token, userDetails, targetSystemId, targetSystemServiceId, freeText);
	}
}
