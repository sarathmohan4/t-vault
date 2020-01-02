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

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public class DatabaseStaticRole implements Serializable {


	private static final long serialVersionUID = -5076797646111163349L;

	@NotNull
	private String name;
	@NotNull
	private String username;
	@NotNull
	private String rotation_period;
	@NotNull
	private String db_name;
	private String[] rotation_statements;

	public DatabaseStaticRole() {
	}

	public DatabaseStaticRole(String name, String username, String rotation_period, String db_name, String[] rotation_statements) {
		this.name = name;
		this.username = username;
		this.rotation_period = rotation_period;
		this.db_name = db_name;
		this.rotation_statements = rotation_statements;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@ApiModelProperty(example="1h", position=3)
	public String getRotation_period() {
		return rotation_period;
	}

	public void setRotation_period(String rotation_period) {
		this.rotation_period = rotation_period;
	}

	public String getDb_name() {
		return db_name;
	}

	public void setDb_name(String db_name) {
		this.db_name = db_name;
	}

	public String[] getRotation_statements() {
		return rotation_statements;
	}

	public void setRotation_statements(String[] rotation_statements) {
		this.rotation_statements = rotation_statements;
	}
}
