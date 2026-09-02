package com.mjc.groupware.approval.mybatis.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ApprovalVo {
	
	private Long appr_no;
	private LocalDateTime appr_reg_date;
	private String appr_title;
	private String appr_status;
	private int appr_order_status;
	private String relationship;
	private String member_name;
	private String approval_form_name;
	private String approver_decision_status;
	
}
