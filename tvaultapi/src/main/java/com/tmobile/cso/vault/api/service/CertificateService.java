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

package com.tmobile.cso.vault.api.service;

import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.UserDetails;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
public class CertificateService {

	@Value("${nclm.endpoint}")
	private String nclmEndpoint;
	
	@Value("${nclm.endpoint.token}")
	private String nclmEndpointToken;

	@Value("${nclm.container.id}")
	private String nclmContainerId;

	private static Logger log = LogManager.getLogger(CertificateService.class);

	/**
	 * To get list of certificates in a container
	 * @param token
	 * @param userDetails
	 * @return
	 */
	public ResponseEntity<String> getCertificates(String token, UserDetails userDetails, String freeText, String limit, String offset) {
		String api = nclmEndpoint + "certificates?containerId="+nclmContainerId;
		if (freeText!=null) {
			api = api + "&freeText="+freeText;
		}
		if (limit!=null) {
			api = api + "&limit="+limit;
		}
		if (offset!=null) {
			api = api + "&offset="+offset;
		}
		return ResponseEntity.status(HttpStatus.OK).body(getApiResponse(api));
	}


	public String addParameter(String api, String type, String name, String value) {
		if (type.equals("path")) {
			api = api + "/" + name + "/" + value;
		}
		else if (type.equals("query")) {
			api = api.contains("?")?api+"&":api+"?";
			api = api + name + "=" + value;
		}
		return api;
	}
	/**
	 * To get response from nclm endpoint
	 * @param api
	 * @return
	 */
	private String getApiResponse(String api)  {
		HttpClient httpClient =null;
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
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getApiResponse").
					put(LogMessage.MESSAGE, "Failed to create hhtpClient").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		HttpGet getRequest = new HttpGet(api);
		getRequest.addHeader("accept", "application/json");
		getRequest.addHeader("Authorization", nclmEndpointToken);
		String output = "";
		StringBuffer jsonResponse = new StringBuffer();

		try {
			HttpResponse apiResponse = apiResponse = httpClient.execute(getRequest);
			if (apiResponse.getStatusLine().getStatusCode() != 200) {
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((apiResponse.getEntity().getContent())));
			while ((output = br.readLine()) != null) {
				jsonResponse.append(output);
			}
			return jsonResponse.toString();
		} catch (IOException e) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder().
					put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString()).
					put(LogMessage.ACTION, "getApiResponse").
					put(LogMessage.MESSAGE, "Failed to get api response").
					put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString()).
					build()));
		}
		return null;
	}

	/**
	 * To get list of target systems in a container
	 * @param token
	 * @param userDetails
	 * @param freeText
	 * @return
	 */
	public ResponseEntity<String> getTargetSystems(String token, UserDetails userDetails, String freeText) {
		String api = nclmEndpoint + "targetsystemgroups/"+nclmContainerId+"/targetsystems";
		if (freeText!=null) {
			api = api + "?freeText="+freeText;
		}
		return ResponseEntity.status(HttpStatus.OK).body(getApiResponse(api));
	}

	/**
	 * To get list of services in a target system
	 * @param token
	 * @param userDetails
	 * @param text
	 * @param freeText
	 * @return
	 */
	public ResponseEntity<String> getServices(String token, UserDetails userDetails, String targetSystemId, String freeText) {
		String api = nclmEndpoint + "targetsystems/"+targetSystemId+"/targetsystemservices";
		if (freeText!=null) {
			api = api + "?freeText="+freeText;
		}
		return ResponseEntity.status(HttpStatus.OK).body(getApiResponse(api));
	}

	/**
	 * To get list of certificates in a target system service
	 * @param token
	 * @param userDetails
	 * @param targetSystemId
	 * @param targetSystemServiceId
	 * @param freeText
	 * @return
	 */
	public ResponseEntity<String> getServiceCertificates(String token, UserDetails userDetails, String targetSystemId, String targetSystemServiceId, String freeText) {
		String api = nclmEndpoint + "targetsystems/"+targetSystemId+"/targetsystemservices/"+targetSystemServiceId;
		if (freeText!=null) {
			api = api + "?freeText="+freeText;
		}
		return ResponseEntity.status(HttpStatus.OK).body(getApiResponse(api));
	}
}
