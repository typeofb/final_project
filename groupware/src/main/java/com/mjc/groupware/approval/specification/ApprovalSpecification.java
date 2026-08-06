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
}
