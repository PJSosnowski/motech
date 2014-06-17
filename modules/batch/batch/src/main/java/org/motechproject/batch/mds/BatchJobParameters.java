package org.motechproject.batch.mds;

// Generated Apr 11, 2014 10:49:43 AM by Hibernate Tools 3.4.0.CR1

import java.util.Date;

import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;


/**
 * BatchJobParameters generated by hbm2java
 */
@Entity
public class BatchJobParameters implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//This should be int
	@Field(required=true)
	private Integer batchJobId;
	
	public Integer getBatchJobId() {
		return batchJobId;
	}

	public void setBatchJobId(Integer batchJobId) {
		this.batchJobId = batchJobId;
	}

	public String getParameterName() {
		return parameterName;
	}

	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}

	public String getParameterValue() {
		return parameterValue;
	}

	public void setParameterValue(String parameterValue) {
		this.parameterValue = parameterValue;
	}

	@Field(required=true)
	private String parameterName;
	
	@Field
	private String parameterValue;
	

	public BatchJobParameters() {
	}

	public BatchJobParameters(Integer batchJobId,
			String parameterName) {
		this.batchJobId = batchJobId;
		this.parameterName = parameterName;
	}

	public BatchJobParameters(Integer batchJobId,
			String parameterName,String parameterValue) {
		this.batchJobId = batchJobId;
		this.parameterName = parameterName;
		this.parameterValue = parameterValue;
		
	}

	
}
