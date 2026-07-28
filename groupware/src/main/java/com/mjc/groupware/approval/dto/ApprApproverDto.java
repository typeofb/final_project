package com.mjc.groupware.approval.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mjc.groupware.approval.entity.ApprAgreementer;
import com.mjc.groupware.approval.entity.ApprApprover;
import com.mjc.groupware.approval.entity.Approval;
import com.mjc.groupware.member.entity.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprApproverDto {
	private Long appr_approver_no;
	private int approver_order;
	private String approver_decision_status;
	private LocalDateTime approver_decision_status_time;
	private String decision_reason;
	private Long appr_no;
	
	// 여러 명의 결재자
	private List<Long> approvers;
	private Long approver;
	
	public List<ApprApprover> toEntityList() {
	    List<ApprApprover> entityList = new ArrayList<>();
	    if (approvers == null) return entityList;

	    for (int i = 0; i < approvers.size(); i++) {
	        Long no = approvers.get(i);

	        ApprApprover approver = ApprApprover.builder()
	            .approverOrder(i + 1) // 1부터 시작하는 결재 순서
	            .approverDecisionStatus("W")
	            .approverDecisionStatusTime(approver_decision_status_time)
	            .decisionReason(approver_decision_status)
	            .approval(Approval.builder().apprNo(appr_no).build())
	            .member(Member.builder().memberNo(no).build())
	            .build();

	        entityList.add(approver);
	    }

	    return entityList;
	}
	
	public ApprApprover toEntity() {
		return ApprApprover.builder()
				.apprApproverNo(appr_approver_no)
				.approverOrder(approver_order)
				.approverDecisionStatus(approver_decision_status)
				.approverDecisionStatusTime(approver_decision_status_time)
				.decisionReason(decision_reason)
				.approval(Approval.builder().apprNo(appr_no).build())
				.member(Member.builder().memberNo(approver).build())
				.build();
	}
	
	public ApprApproverDto toDto(ApprApprover approver) {
		return ApprApproverDto.builder()
				.appr_approver_no(approver.getApprApproverNo())
				.approver_order(approver.getApproverOrder())
				.approver_decision_status(approver.getApproverDecisionStatus())
				.approver_decision_status_time(approver.getApproverDecisionStatusTime())
				.decision_reason(approver.getDecisionReason())
				.appr_no(approver.getApproval().getApprNo())
				.approver(approver.getMember().getMemberNo())
				.build();
	}
	
}
