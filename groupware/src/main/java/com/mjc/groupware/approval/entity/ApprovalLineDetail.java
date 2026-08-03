package com.mjc.groupware.approval.entity;

import com.mjc.groupware.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "approval_line_detail")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ApprovalLineDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "detail_id")
	private Long detailId;

	@ManyToOne
	@JoinColumn(name = "line_id")
	private ApprovalLine approvalLine;

	@ManyToOne
	@JoinColumn(name = "member_no")
	private Member member;

	@Column(name = "appr_type")
	private String apprType; // "APPROVER", "AGREEMENTER", "REFERENCER"

	@Column(name = "appr_order")
	private int apprOrder;
}
