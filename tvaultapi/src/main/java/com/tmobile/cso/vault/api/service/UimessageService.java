package com.tmobile.cso.vault.api.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.tmobile.cso.vault.api.common.TVaultConstants;
import com.tmobile.cso.vault.api.controller.ControllerUtil;
import com.tmobile.cso.vault.api.exception.LogMessage;
import com.tmobile.cso.vault.api.model.Message;
import com.tmobile.cso.vault.api.process.RequestProcessor;
import com.tmobile.cso.vault.api.process.Response;
import com.tmobile.cso.vault.api.utils.CommonUtils;
import com.tmobile.cso.vault.api.utils.JSONUtil;
import com.tmobile.cso.vault.api.utils.ThreadLocalContext;
import com.tmobile.cso.vault.api.utils.TokenUtils;

@Component
public class UimessageService {

	@Autowired
	private RequestProcessor reqProcessor;
	@Autowired
	private TokenUtils tokenUtils;
	@Autowired
	private CommonUtils commonUtils;

	private static Logger log = LogManager.getLogger(UimessageService.class);

	/**
	 * Save messages
	 * 
	 * @param token
	 * @param message
	 * @return
	 */
	public ResponseEntity<String> writeMessage(String token, Message message) {

		if (!commonUtils.isAuthorizedToken(token)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
					.put(LogMessage.ACTION, "isAuthorizedToken")
					.put(LogMessage.MESSAGE, "Access Denied: No enough permission to access this API")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("{\"errors\":[\"Access Denied: No enough permission to access this API\"]}");
		}

		ObjectMapper objMapper = new ObjectMapper();

		HashMap<String, String> metadataMap = message.getDetails();
		String metadataJson = "";
		try {
			metadataJson = objMapper.writeValueAsString(metadataMap);
		} catch (JsonProcessingException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("{\"errors\":[\"Invalid request.Check json data\"]}");
		}

		String path = TVaultConstants.UIMES_SAFES_METADATA;

		String writeJson = "{\"path\":\"" + path + "\",\"data\":" + metadataJson + "}";

		try {
			JsonNode dataNode = objMapper.readTree(writeJson).get("data");

			if (dataNode.toString() == "null") {

				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body("{\"errors\":[\"Invalid request.Check json data\"]}");
			}
		} catch (Exception e) {
			log.error(e);
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "JsonNode")
					.put(LogMessage.MESSAGE, String.format("dataNode failed  [%s]", e.getMessage()))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
		}

		if (isdataNullorEmpty(metadataMap)) {
			log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "isdataNullorEmpty")
					.put(LogMessage.MESSAGE, String.format("This field is required cannot be null value.", path))
					.put(LogMessage.RESPONSE, "Invalid input values")
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid input values\"]}");
		}

		if (ControllerUtil.isFolderExisting(path, token)) {
			log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
					.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
					.put(LogMessage.ACTION, "isFolderExisting")
					.put(LogMessage.MESSAGE, String.format("path is existing [%s]", path))
					.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
					.build()));

			if (keyexistingcheck(path, token, metadataJson)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "keyexistingcheck")
						.put(LogMessage.MESSAGE, String.format("sucessfully updated message ", path))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
			}

			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"message saved to vault\"]}");
		}

		else {
			Response response1 = reqProcessor.process("/sdb/createfolder", writeJson, token);
			if (response1.getHttpstatus().equals(HttpStatus.NO_CONTENT)) {
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "CreateFolder").put(LogMessage.MESSAGE, "Create Folder completed")
						.put(LogMessage.STATUS, response1.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));

				Response response = reqProcessor.process("/write", writeJson, token);
				log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "Writemessage")
						.put(LogMessage.MESSAGE, " Writing message [%s] completed successfully")
						.put(LogMessage.STATUS, response.getHttpstatus().toString())
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
			} else {
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "WriteMessage")
						.put(LogMessage.MESSAGE, String.format("Writing message [%s] failed", path))
						.put(LogMessage.RESPONSE, "Invalid path")
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"errors\":[\"Invalid path\"]}");
			}

			return ResponseEntity.status(HttpStatus.OK).body("{\"messages\":[\"message saved to vault\"]}");
		}

	}

	/**
	 * check for null value
	 * 
	 * @return
	 */

	public boolean isdataNullorEmpty(HashMap<String, String> metadataMap) {
		if (metadataMap.containsValue(null) || metadataMap.isEmpty()) {
			return true;
		}

		return false;
	}

	/**
	 * check whether message is exist or not
	 * 
	 * @param token
	 * @param path
	 * @return
	 */

	public boolean keyexistingcheck(String path, String token, String metadataJson) {
		Response response = reqProcessor.process("/read", "{\"path\":\"" + path + "\"}", token);
		if (HttpStatus.OK.equals(response.getHttpstatus())) {
			String responseJson = response.getResponse();

			ObjectMapper objMapper = new ObjectMapper();
			try {
				JsonNode dataNode = objMapper.readTree(responseJson).get("data");
				Map<String, String> responseMap = objMapper.convertValue(dataNode,
						new TypeReference<Map<String, String>>() {
						});

				Map<String, String> metadataMap = objMapper.readValue(metadataJson, Map.class);

				for (Map.Entry<String, String> entry : responseMap.entrySet()) {
					for (Map.Entry<String, String> entry1 : metadataMap.entrySet()) {
						if (entry.getKey() == entry1.getKey()) {
							responseMap.replace(entry1.getKey(), entry1.getValue());
							metadataJson = objMapper.writeValueAsString(responseMap);

							String writeJson = "{\"path\":\"" + path + "\",\"data\":" + metadataJson + "}";

							Response response1 = reqProcessor.process("/write", writeJson, token);

						} else {
							responseMap.put(entry1.getKey(), entry1.getValue());

							metadataJson = objMapper.writeValueAsString(responseMap);
							String writeJson = "{\"path\":\"" + path + "\",\"data\":" + metadataJson + "}";

							Response response1 = reqProcessor.process("/write", writeJson, token);

						}
					}
				}

			} catch (Exception e) {
				log.error(e);
				log.error(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
						.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER).toString())
						.put(LogMessage.ACTION, "keyexistingcheck")
						.put(LogMessage.MESSAGE, String.format("keyexistingcheck failed", e.getMessage()))
						.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL).toString())
						.build()));

			}

			return true;
		}
		return false;

	}

	/**
	 * Get message
	 * 
	 * @return
	 */

	public ResponseEntity<String> readMessage() {
		String token = tokenUtils.getSelfServiceToken();
		String path = TVaultConstants.UIMES_SAFES_METADATA;
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "readMessage")
				.put(LogMessage.MESSAGE, String.format("Trying to get read Message in vault [%s]", path))
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		Response response = reqProcessor.process("/sdb", "{\"path\":\"" + path + "\"}", token);
		log.debug(JSONUtil.getJSON(ImmutableMap.<String, String>builder()
				.put(LogMessage.USER, ThreadLocalContext.getCurrentMap().get(LogMessage.USER))
				.put(LogMessage.ACTION, "readMessage").put(LogMessage.MESSAGE, "Getting message completed")
				.put(LogMessage.STATUS, response.getHttpstatus().toString())
				.put(LogMessage.APIURL, ThreadLocalContext.getCurrentMap().get(LogMessage.APIURL)).build()));
		return ResponseEntity.status(response.getHttpstatus()).body(response.getResponse());

	}

}