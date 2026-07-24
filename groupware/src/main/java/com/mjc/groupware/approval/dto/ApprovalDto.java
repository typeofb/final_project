package com.mjc.groupware.approval.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mjc.groupware.approval.entity.Approval;
import com.mjc.groupware.approval.entity.ApprovalForm;
import com.mjc.groupware.member.entity.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class ApprovalDto {
	private Long appr_no;
	private LocalDateTime appr_reg_date;
	private LocalDateTime appr_res_date;
	private String appr_title;
	private String appr_text;
	private String appr_status;
	private int appr_order_status;
	private String appr_reason;
	private Long appr_sender;
	private Long approval_type_no;
	private String return_reason;
	
	// 연차 신청서 주요정보
	private LocalDate start_date;
	private LocalDate end_date;
	private double use_annual_leave;
	private int annual_leave_type; // 반차 타입
	
	// 결재자, 합의자, 참조자
	private List<Long> approver_no;
	private List<Long> agreementer_no;
	private List<Long> referencer_no;
	
	private Long parent_approval;
	
	// 대리 기안 관련 정보
	private Long proxy_drafter;
	private String proxy_drafter_name;
	private String is_proxy;
	private Long target_member_no; // 대리 기안 시 선택된 대상 사원번호
	
	public Approval toEntity() {
		return toEntity(null); // 기본 결재 생성 시 parentApproval 없이
	}

	public Approval toEntity(Approval parentApproval) {
		return Approval.builder()
				.apprNo(appr_no)
				.apprRegDate(appr_reg_date)
				.apprResDate(appr_res_date)
				.apprTitle(appr_title)
				.apprText(appr_text)
				.apprStatus(appr_status)
				.apprOrderStatus(appr_order_status)
				.apprReason(appr_reason)
				.startDate(start_date)
				.endDate(end_date)
				.useAnnualLeave(use_annual_leave)
				.annualLeaveType(annual_leave_type)
				.returnReason(return_reason)
				.member(Member.builder().memberNo(appr_sender).build())
				.proxyDrafter(proxy_drafter != null ? Member.builder().memberNo(proxy_drafter).build() : null)
				.isProxy(is_proxy != null ? is_proxy : "N")
				.approvalForm(ApprovalForm.builder().approvalFormNo(approval_type_no).build())
				.parentApproval(parentApproval) // null 가능
				.build();
	}
	
	public ApprovalDto toDto(Approval approval) {
		return ApprovalDto.builder()
				.appr_no(approval.getApprNo())
				.appr_reg_date(approval.getApprRegDate())
				.appr_res_date(approval.getApprResDate())
				.appr_title(approval.getApprTitle())
				.appr_text(approval.getApprText())
				.appr_status(approval.getApprStatus())
				.appr_order_status(approval.getApprOrderStatus())
				.appr_reason(approval.getApprReason())
				.start_date(approval.getStartDate())
				.end_date(approval.getEndDate())
				.use_annual_leave(approval.getUseAnnualLeave())
				.annual_leave_type(approval.getAnnualLeaveType())
				.return_reason(approval.getReturnReason())
				.appr_sender(approval.getMember().getMemberNo())
				.proxy_drafter(approval.getProxyDrafter() != null ? approval.getProxyDrafter().getMemberNo() : null)
				.proxy_drafter_name(approval.getProxyDrafter() != null ? approval.getProxyDrafter().getMemberName() : null)
				.is_proxy(approval.getIsProxy())
				.approval_type_no(approval.getApprovalForm().getApprovalFormNo())
				.parent_approval(approval.getParentApproval() != null ? approval.getParentApproval().getApprNo() : null)
				.build();
	}
}
