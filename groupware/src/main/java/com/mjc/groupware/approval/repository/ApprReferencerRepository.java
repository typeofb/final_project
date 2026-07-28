package com.mjc.groupware.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mjc.groupware.approval.entity.ApprReferencer;

import java.util.Optional;

public interface ApprReferencerRepository extends JpaRepository<ApprReferencer, Long> {
	List<ApprReferencer> findAllByApproval_ApprNo(Long id);
	Optional<ApprReferencer> findByApproval_ApprNoAndMember_MemberNo(Long apprNo, Long memberNo);
}
