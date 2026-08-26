package com.mjc.groupware.approval.mybatis.vo;

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
public class ApprovalStatusVo {
	private int P_count;
	private int A_count;
	private int R_count;
	private int C_count;
}
