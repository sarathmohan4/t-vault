// =========================================================================
// Copyright 2021 T-Mobile, US
// 
// Licensed under the Apache License, Version 2.0 (the "License")
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

package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

public class IAMServiceAccountSecretResponse implements Serializable {


    private static final long serialVersionUID = 1755638187042734602L;

    Integer statusCode;
    IAMServiceAccountSecret iamServiceAccountSecret;

    public IAMServiceAccountSecretResponse() {
        super();
    }

    public IAMServiceAccountSecretResponse(Integer statusCode, IAMServiceAccountSecret iamServiceAccountSecret) {
        this.statusCode = statusCode;
        this.iamServiceAccountSecret = iamServiceAccountSecret;
    }


    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public IAMServiceAccountSecret getIamServiceAccountSecret() {
        return iamServiceAccountSecret;
    }

    public void setIamServiceAccountSecret(IAMServiceAccountSecret iamServiceAccountSecret) {
        this.iamServiceAccountSecret = iamServiceAccountSecret;
    }

    @Override
    public String toString() {
        return "IAMServiceAccountSecretResponse{" +
                "statusCode=" + statusCode +
                ", iamServiceAccountSecret=" + iamServiceAccountSecret +
                '}';
    }
}