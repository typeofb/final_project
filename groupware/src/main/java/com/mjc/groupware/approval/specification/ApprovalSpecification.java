package com.mjc.groupware.approval.specification;

import org.springframework.data.jpa.domain.Specification;

import com.mjc.groupware.approval.entity.Approval;

import jakarta.persistence.criteria.Join;

public class ApprovalSpecification {

	public static Specification<Approval> approvalTitleContains(String keyword){
		return (root, query, criteriaBuilder) ->
			criteriaBuilder.like(root.get("apprTitle"), "%"+keyword+"%");
	}
	
	public static Specification<Approval> approvalFormNameContains(String keyword) {
	    return (root, query, criteriaBuilder) -> {
	        Join<Object, Object> formJoin = root.join("approvalForm"); // approval.approvalForm
	        return criteriaBuilder.like(formJoin.get("approvalFormName"), "%" + keyword + "%");
	    };
	}
	
	public static Specification<Approval> approvalSenderContains(Long keyword) {
	    return (root, query, criteriaBuilder) -> {
	        Join<Object, Object> memberJoin = root.join("member"); // approval.member (Member 객체)
	        return criteriaBuilder.equal(memberJoin.get("memberNo"), keyword);
	    };
	}
	
	public static Specification<Approval> approvalStatusContains(String keyword) {
		return (root, query, criteriaBuilder) -> 
			criteriaBuilder.like(root.get("apprStatus"), "%"+keyword+"%");
	}
	
	public static Specification<Approval> approvalReturnApprovalContains(Long keyword) {
		return (root, query, criteriaBuilder) -> {
			Join<Object, Object> memberJoin = root.join("parentApproval");
	        return criteriaBuilder.equal(memberJoin.get("apprNo"), keyword);
		};
	}
	
	public static Specification<Approval> approvalMemberNameContains(String keyword) {
		return (root, query, criteriaBuilder) -> {
			Join<Object, Object> memberJoin = root.join("member");
			return criteriaBuilder.like(memberJoin.get("memberName"), "%" + keyword + "%");
		};
	}

	public static Specification<Approval> approvalDeptNameContains(String keyword) {
		return (root, query, criteriaBuilder) -> {
			Join<Object, Object> memberJoin = root.join("member");
			Join<Object, Object> deptJoin = memberJoin.join("dept");
			return criteriaBuilder.like(deptJoin.get("deptName"), "%" + keyword + "%");
		};
	}

	public static Specification<Approval> approvalRegDateBetween(String startDateStr, String endDateStr) {
		return (root, query, criteriaBuilder) -> {
			if ((startDateStr == null || startDateStr.trim().isEmpty()) && (endDateStr == null || endDateStr.trim().isEmpty())) {
				return null;
			}
			try {
				if (startDateStr != null && !startDateStr.trim().isEmpty() && endDateStr != null && !endDateStr.trim().isEmpty()) {
					java.time.LocalDateTime start = java.time.LocalDate.parse(startDateStr.trim()).atStartOfDay();
					java.time.LocalDateTime end = java.time.LocalDate.parse(endDateStr.trim()).atTime(java.time.LocalTime.MAX);
					return criteriaBuilder.between(root.get("apprRegDate"), start, end);
				} else if (startDateStr != null && !startDateStr.trim().isEmpty()) {
					java.time.LocalDateTime start = java.time.LocalDate.parse(startDateStr.trim()).atStartOfDay();
					return criteriaBuilder.greaterThanOrEqualTo(root.get("apprRegDate"), start);
				} else {
					java.time.LocalDateTime end = java.time.LocalDate.parse(endDateStr.trim()).atTime(java.time.LocalTime.MAX);
					return criteriaBuilder.lessThanOrEqualTo(root.get("apprRegDate"), end);
				}
			} catch (Exception e) {
				return null;
			}
		};
	}
}
