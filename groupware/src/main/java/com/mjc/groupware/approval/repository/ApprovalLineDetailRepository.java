package com.mjc.groupware.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mjc.groupware.approval.entity.ApprovalLineDetail;

@Repository
public interface ApprovalLineDetailRepository extends JpaRepository<ApprovalLineDetail, Long> {

	List<ApprovalLineDetail> findAllByApprovalLine_LineIdOrderByApprOrderAsc(Long lineId);
}
