package com.mjc.groupware.approval.entity;

import com.mjc.groupware.member.entity.Member;

import groovy.transform.ToString;
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
@Table(name="appr_referencer")
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApprReferencer {
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="appr_referencer_no")
	private Long apprReferencerNo;
	
	@ManyToOne
	@JoinColumn(name="appr_no", nullable=false)
	private Approval approval;
	
	@ManyToOne
	@JoinColumn(name="referencer_no", nullable=false)
	private Member member;

	@Column(name="referencer_status", columnDefinition = "CHAR(1) DEFAULT 'N'")
	private String referencerStatus;

	public void updateReferencerStatus(String status) {
		this.referencerStatus = status;
	}
}
