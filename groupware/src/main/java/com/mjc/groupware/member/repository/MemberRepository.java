package com.mjc.groupware.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mjc.groupware.dept.entity.Dept;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.entity.Role;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
	
	Member findByMemberId(String keyword);
	
	List<Member> findAll(Specification<Member> spec);
	Page<Member> findAll(Specification<Member> spec, Pageable pageable);

	List<Member> findAllByDept_DeptNo(Long deptNo);
	
	@Query("SELECT m FROM Member m JOIN m.pos p WHERE m.dept.deptNo = :deptNo ORDER BY p.posOrder ASC")
    List<Member> findAllByDeptNoSortedByPosOrder(@Param("deptNo") Long deptNo);

	@Query("SELECT m FROM Member m JOIN m.pos p WHERE (m.dept.deptName = '임원' OR m.dept.deptNo = 1) AND m.status != 3 ORDER BY p.posOrder DESC")
	List<Member> findExecutivesSortedByPosOrderDesc();

	List<Member> findByDept(Dept dept);
	
	List<Member> findByRole_RoleNo(Long roleNo);
	
	int countByRole(Role role);
	
	Member findTopByOrderByMemberNoDesc();
	
	Member findByMemberIdAndMemberEmail(String memberId, String memberEmail);
	
}
