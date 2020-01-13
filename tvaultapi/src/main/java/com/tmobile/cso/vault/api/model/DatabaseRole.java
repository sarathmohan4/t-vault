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

package com.tmobile.cso.vault.api.model;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class DatabaseRole implements Serializable {


	private static final long serialVersionUID = -368953893266873604L;
	@NotNull
	private String role_name;
	@NotNull
	private String db_name;
	private String default_ttl;
	private String max_ttl;
	@NotNull
	private String[] creation_statements;
	private String[] revocation_statements;
	private String[] rollback_statements;
	private String[] renew_statements ;



	public DatabaseRole() {
	}

	public DatabaseRole(String role_name, String db_name, String default_ttl, String max_ttl, String[] creation_statements, String[] revocation_statements, String[] rollback_statements, String[] renew_statements) {
		this.role_name = role_name;
		this.db_name = db_name;
		this.default_ttl = default_ttl;
		this.max_ttl = max_ttl;
		this.creation_statements = creation_statements;
		this.revocation_statements = revocation_statements;
		this.rollback_statements = rollback_statements;
		this.renew_statements = renew_statements;
	}

	public String getRole_name() {
		return role_name;
	}

	public void setRole_name(String role_name) {
		this.role_name = role_name;
	}

	public String getDb_name() {
		return db_name;
	}

	public void setDb_name(String db_name) {
		this.db_name = db_name;
	}

	@ApiModelProperty(example="1h", position=3)
	public String getDefault_ttl() {
		return default_ttl;
	}

	public void setDefault_ttl(String default_ttl) {
		this.default_ttl = default_ttl;
	}

	@ApiModelProperty(example="24h", position=4)
	public String getMax_ttl() {
		return max_ttl;
	}

	public void setMax_ttl(String max_ttl) {
		this.max_ttl = max_ttl;
	}

	public String[] getCreation_statements() {
		return creation_statements;
	}

	public void setCreation_statements(String[] creation_statements) {
		this.creation_statements = creation_statements;
	}

	public String[] getRevocation_statements() {
		return revocation_statements;
	}

	public void setRevocation_statements(String[] revocation_statements) {
		this.revocation_statements = revocation_statements;
	}

	public String[] getRollback_statements() {
		return rollback_statements;
	}

	public void setRollback_statements(String[] rollback_statements) {
		this.rollback_statements = rollback_statements;
	}

	public String[] getRenew_statements() {
		return renew_statements;
	}

	public void setRenew_statements(String[] renew_statements) {
		this.renew_statements = renew_statements;
	}
}
