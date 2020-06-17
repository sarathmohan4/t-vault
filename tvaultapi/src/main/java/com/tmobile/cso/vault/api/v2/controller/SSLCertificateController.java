//=========================================================================
//Copyright 2020 T-Mobile, US
//
//Licensed under the Apache License, Version 2.0 (the "License")
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//See the readme.txt file for additional language around disclaimer of warranties.
//=========================================================================
package com.tmobile.cso.vault.api.v2.controller;


import com.tmobile.cso.vault.api.model.CertManagerLoginRequest;
import com.tmobile.cso.vault.api.model.CertificateDownloadRequest;
import com.tmobile.cso.vault.api.model.SSLCertificateRequest;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.process.CertResponse;
import com.tmobile.cso.vault.api.service.SSLCertificateService;
import com.tmobile.cso.vault.api.utils.TVaultSSLCertificateException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@CrossOrigin
@Api( description = "SSL Certificate  Management Controller", position = 15)
public class SSLCertificateController {

	@Autowired
	private SSLCertificateService sslCertificateService;

	/**
	 * To authenticate with Certificate Lifecycle Manager
	 * @param certManagetLoginRequest
	 * @return
	 */
	@ApiOperation(value = "${SSLCertificateController.login.value}", notes = "${SSLCertificateController.login.notes}")
	@PostMapping(value="/v2/auth/sslcert/login",produces="application/json")
	public ResponseEntity<String> authenticate(@RequestBody CertManagerLoginRequest certManagetLoginRequest) throws Exception, TVaultSSLCertificateException {
		return sslCertificateService.authenticate(certManagetLoginRequest);
	}
	/**
	 * To Create SSL Certificate
	 * @param sslCertificateRequest
	 * @return
	 */
	@ApiOperation(value = "${SSLCertificateController.sslcreate.value}", notes = "${SSLCertificateController.sslcreate.notes}")
	@PostMapping(value="/v2/sslcert",consumes="application/json",produces="application/json")
	public ResponseEntity<CertResponse> generateSSLCertificate(HttpServletRequest request, @RequestHeader(value=
			"vault-token") String token,@RequestBody SSLCertificateRequest sslCertificateRequest)  {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return sslCertificateService.generateSSLCertificate(sslCertificateRequest,userDetails,token);
	}

	/**
	 * Download certificate with private key.
	 * @param request
	 * @param token
	 * @param certificateDownloadRequest
	 * @return
	 */
	@ApiOperation(value = "${CertificateController.downloadCertificateWithPrivateKey.value}", notes = "${CertificateController.downloadCertificateWithPrivateKey.notes}")
	@PostMapping(value="/v2/nclm/certificates/download", consumes="application/json")
	public ResponseEntity<InputStreamResource> downloadCertificateWithPrivateKey(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @Valid @RequestBody CertificateDownloadRequest certificateDownloadRequest) {
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return sslCertificateService.downloadCertificateWithPrivateKey(token, certificateDownloadRequest, userDetails);
	}

	/**
	 * Download certificate.
	 * @param request
	 * @param token
	 * @param certificateId
	 * @param certificateType
	 * @return
	 */
	@ApiOperation(value = "${CertificateController.downloadCertificate.value}", notes = "${CertificateController.downloadCertificate.notes}")
	@GetMapping(value="/v2/nclm/certificates/{certificate_id}/{certificate_type}", produces="application/json")
	public ResponseEntity<InputStreamResource> downloadCertificate(HttpServletRequest request, @RequestHeader(value="vault-token") String token, @PathVariable("certificate_id") String certificateId, @PathVariable("certificate_type") String certificateType){
		UserDetails userDetails = (UserDetails) ((HttpServletRequest) request).getAttribute("UserDetails");
		return sslCertificateService.downloadCertificate(token, userDetails, certificateId, certificateType);
	}

}