package com.tmobile.cso.vault.api.v2.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tmobile.cso.vault.api.model.Message;
import com.tmobile.cso.vault.api.service.UimessageService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@CrossOrigin
@Api(description = "Manage UI Message", position = 20)
public class UimessageController {

	@Autowired
	private UimessageService uimessageService;

	@ApiOperation(value = "${UimessageController.writeMessage.value}", notes = "${UimessageController.writeMessage.notes}")
	@PostMapping(value = { "v2/safes/message" }, consumes = "application/json", produces = "application/json")
	public ResponseEntity<String> writeMessage(HttpServletRequest request,
			@RequestHeader(value = "vault-token") String token, @RequestBody Message message) {

		return uimessageService.writeMessage(token, message); 

	}

	@ApiOperation(value = "${UimessageController.readMessage.value}", notes = "${UimessageController.readMessage.notes}")
	@GetMapping(value = "v2/safes/message", produces = "application/json")
	public ResponseEntity<String> readMessage() {
		return uimessageService.readMessage();
	}
}