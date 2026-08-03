package com.mjc.groupware.approval.entity;

import com.mjc.groupware.dept.entity.Dept;

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
@Table(name = "approval_line")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ApprovalLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "line_id")
	private Long lineId;

	@ManyToOne
	@JoinColumn(name = "approval_form_no")
	private ApprovalForm approvalForm;

	@ManyToOne
	@JoinColumn(name = "dept_no")
	private Dept dept;

	@Column(name = "line_name")
	private String lineName;

	@Column(name = "use_yn")
	private String useYn;
}
