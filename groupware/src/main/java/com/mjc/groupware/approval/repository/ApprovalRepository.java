package com.mjc.groupware.approval.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.mjc.groupware.approval.entity.Approval;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
	@EntityGraph(attributePaths = {"member", "member.dept", "approvalForm"})
	Page<Approval> findAll(Specification<Approval> spec, Pageable pageable);
	
	// Approval 엔티티에서 member.memberNo가 일치하는 모든 데이터 찾기
	
	List<Approval> findAllByMember_MemberNo(Long member_no);
	
	// List<Approval> findAll(Specification<Approval> spec);
	List<Approval> findAllByParentApproval_ApprNo(Long parentApprNo);

	long countByApprStatus(String apprStatus);

	@Query("SELECT a.apprStatus, COUNT(a) FROM Approval a GROUP BY a.apprStatus")
	List<Object[]> countGroupedByApprStatus();
}
