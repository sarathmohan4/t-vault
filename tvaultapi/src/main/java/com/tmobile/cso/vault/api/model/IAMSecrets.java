/** *******************************************************************************
*  Copyright 2020 T-Mobile, US
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*  See the readme.txt file for additional language around disclaimer of warranties.
*********************************************************************************** */

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModelProperty;

public class IAMSecrets implements Serializable {

	private static final long serialVersionUID = -5704090790663628283L;

	@Size(min = 16, max = 128, message = "AccessKeyId specified should be minimum 16 chanracters and maximum 128 characters only")
	private String accessKeyId;

	@ApiModelProperty(hidden = true)
	private Long expiryDateEpoch;

	public IAMSecrets() {
		super();
	}

	/**
	 * @param accessKeyId
	 * @param expiryDateEpoch
	 */
	public IAMSecrets(String accessKeyId, Long expiryDateEpoch) {
		super();
		this.accessKeyId = accessKeyId;
		this.expiryDateEpoch = expiryDateEpoch;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public Long getExpiryDateEpoch() {
		return expiryDateEpoch;
	}

	public void setExpiryDateEpoch(Long expiryDateEpoch) {
		this.expiryDateEpoch = expiryDateEpoch;
	}
}
