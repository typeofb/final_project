package com.mjc.groupware.approval.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mjc.groupware.approval.entity.ApprovalLine;

@Repository
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

	@Query("SELECT a FROM ApprovalLine a WHERE a.approvalForm.approvalFormNo = :formNo AND a.dept.deptNo = :deptNo AND a.useYn = 'Y'")
	Optional<ApprovalLine> findByFormNoAndDeptNo(@Param("formNo") Long formNo, @Param("deptNo") Long deptNo);

	@Query("SELECT a FROM ApprovalLine a WHERE a.approvalForm.approvalFormNo = :formNo AND a.dept IS NULL AND a.useYn = 'Y'")
	Optional<ApprovalLine> findByFormNoAndDeptIsNull(@Param("formNo") Long formNo);
}
