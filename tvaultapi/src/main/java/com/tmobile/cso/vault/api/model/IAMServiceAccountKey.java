package com.tmobile.cso.vault.api.model;

import java.io.Serializable;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import io.swagger.annotations.ApiModelProperty;

public class IAMServiceAccountKey implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3639469557176102034L;
	
	@Size(min = 16, max = 128, message = "AccessKeyId specified should be minimum 16 chanracters and maximum 128 characters only")
	private String accessKeyId;
	
    private String accessKeySecret;
	@NotNull
	@Min(1)
	private Long expiryDateEpoch;
	@NotBlank
	@Size(min = 1, max = 64, message = "UserName specified should be minimum 1 character and maximum 64 characters only")
	@Pattern(regexp = "^[a-zA-Z0-9+=,.@_-]+$", message = "Name can have alphabets, numbers, plus (+), equal (=), comma (,), period (.), at (@), underscore (_), and hyphen (-)  only")
    private String userName;
	@NotBlank
	@Pattern( regexp = "^$|^[0-9]+$", message="Invalid AWS account id")
	@Size(min = 1, max = 12, message = "AWSAccountId specified should be maximum 12 characters only")
    private String awsAccountId;
    private String expiryDate;
    private String createDate;
    private String status;
	/**
	 * @return the accessKeyId
	 */
    @ApiModelProperty(example = "testaccesskeyid", position = 3)
	public String getAccessKeyId() {
		return accessKeyId;
	}
	/**
	 * @return the accessKeySecret
	 */
    @ApiModelProperty(example = "", position = 4)
	public String getAccessKeySecret() {
		return accessKeySecret;
	}
	/**
	 * @return the expiryDateEpoch
	 */
    @ApiModelProperty(example = "", position = 7)
	public Long getExpiryDateEpoch() {
		return expiryDateEpoch;
	}
	/**
	 * @return the userName
	 */
	@ApiModelProperty(example = "testaccountname", position = 2)
	public String getUserName() {
		return userName;
	}
	/**
	 * @return the awsAccountId
	 */
	@ApiModelProperty(example = "testaccountid", position = 1)
	public String getAwsAccountId() {
		return awsAccountId;
	}
	/**
	 * @return the expiryDate
	 */
	@ApiModelProperty(example = "", position = 6)
	public String getExpiryDate() {
		return expiryDate;
	}
	/**
	 * @return the createDate
	 */
	@ApiModelProperty(example = "", position = 5)
	public String getCreateDate() {
		return createDate;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param accessKeyId the accessKeyId to set
	 */
	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}
	/**
	 * @param accessKeySecret the accessKeySecret to set
	 */
	public void setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
	}
	/**
	 * @param expiryDateEpoch the expiryDateEpoch to set
	 */
	public void setExpiryDateEpoch(Long expiryDateEpoch) {
		this.expiryDateEpoch = expiryDateEpoch;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}
	/**
	 * @param awsAccountId the awsAccountId to set
	 */
	public void setAwsAccountId(String awsAccountId) {
		this.awsAccountId = awsAccountId;
	}
	/**
	 * @param expiryDate the expiryDate to set
	 */
	public void setExpiryDate(String expiryDate) {
		this.expiryDate = expiryDate;
	}
	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	public IAMServiceAccountKey() {
		
	}
	public IAMServiceAccountKey(String accessKeyId, String accessKeySecret, Long expiryDateEpoch, String userName,
			String awsAccountId, String expiryDate, String createDate, String status) {
		super();
		this.accessKeyId = accessKeyId;
		this.accessKeySecret = accessKeySecret;
		this.expiryDateEpoch = expiryDateEpoch;
		this.userName = userName;
		this.awsAccountId = awsAccountId;
		this.expiryDate = expiryDate;
		this.createDate = createDate;
		this.status = status;
	}
	@Override
	public String toString() {
		return "IAMServiceAccountKey [accessKeyId=" + accessKeyId + ", accessKeySecret=" + accessKeySecret
				+ ", expiryDateEpoch=" + expiryDateEpoch + ", userName=" + userName + ", awsAccountId=" + awsAccountId
				+ ", expiryDate=" + expiryDate + ", createDate=" + createDate + ", status=" + status + "]";
	}
    
}
