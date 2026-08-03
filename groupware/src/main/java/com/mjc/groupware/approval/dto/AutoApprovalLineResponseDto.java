package com.mjc.groupware.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AutoApprovalLineResponseDto {

	private Long member_no;
	private String member_name;
	private String dept_name;
	private String pos_name;
	private String appr_type; // "APPROVER", "AGREEMENTER", "REFERENCER"
	private int appr_order;
}
