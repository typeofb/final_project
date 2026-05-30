package com.mjc.groupware.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor; 

@Entity
@Table(name="approval_form")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ApprovalForm {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="approval_form_no")
	private Long approvalFormNo;
	
	@Column(name="approval_form_name")
	private String approvalFormName;
	
	@Column(name="approval_form", columnDefinition = "LONGTEXT")
	private String approvalForm;
	
	@Column(name="approval_form_status")
	private String approvalFormStatus;
	
	@Column(name="approval_form_type")
	private int approvalFormType;
	
}
