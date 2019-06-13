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

package com.tmobile.cso.vault.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Sarath
 *
 */
public class AWSRoleMetadata implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7357707900650088181L;
    private String path;
    /**
     * AWSRoleMetadataDetails details
     */
    @JsonProperty("data")
    private AWSRoleMetadataDetails awsRoleMetadataDetails;
    public AWSRoleMetadata() {
        super();
    }
    public AWSRoleMetadata(String path, AWSRoleMetadataDetails awsRoleMetadataDetails) {
        super();
        this.path = path;
        this.awsRoleMetadataDetails = awsRoleMetadataDetails;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public AWSRoleMetadataDetails getAwsRoleMetadataDetails() {
        return awsRoleMetadataDetails;
    }

    public void setAwsRoleMetadataDetails(AWSRoleMetadataDetails awsRoleMetadataDetails) {
        this.awsRoleMetadataDetails = awsRoleMetadataDetails;
    }
}
