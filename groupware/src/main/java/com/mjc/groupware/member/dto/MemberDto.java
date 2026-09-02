package com.mjc.groupware.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mjc.groupware.dept.entity.Dept;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.entity.Role;
import com.mjc.groupware.pos.entity.Pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class MemberDto {
	
	private Long member_no;
	private String member_id;
	private String member_pw;
	private String member_new_pw;
	private String member_name;
	private String member_birth;
	private String member_gender;
	private String member_addr1;
	private String member_addr2;
	private String member_addr3;
	private String member_email;
	private String member_phone;
	private Long pos_no;
	private Long dept_no;
	private Long role_no = (long) 2;
	private int status = 100;
	private String dept_name; // 04/09 JJI 사원 부서명 불러오기위해 추가
	private String pos_name; // 04/09 JJI 사원 직급명 불러오기위해 추가
	private String role_name;
	private LocalDate reg_date;
	private LocalDateTime mod_date;
	private LocalDateTime end_date;
	private double annual_leave;
	private String signature;
	@Builder.Default
	private String is_absent = "N";
	
	public Member toEntity() {
		return Member.builder()
				.memberNo(this.getMember_no())
				.memberId(this.getMember_id())
				.memberPw(this.getMember_pw())
				.memberName(this.getMember_name())
				.memberBirth(this.getMember_birth())
				.memberGender(this.getMember_gender())
				.memberEmail(this.getMember_email())
				.memberPhone(this.getMember_phone())
				.regDate(this.getReg_date())
				.memberAddr1(this.getMember_addr1())
				.memberAddr2(this.getMember_addr2())
				.memberAddr3(this.getMember_addr3())
				.pos(Pos.builder().posNo(this.getPos_no()).build())
				.dept(Dept.builder().deptNo(this.getDept_no()).build())
				.role(Role.builder().roleNo(this.getRole_no()).build())
				.status(this.getStatus())
				.annualLeave(this.getAnnual_leave())
				.signature(this.getSignature())
				.isAbsent(this.getIs_absent() != null ? this.getIs_absent() : "N")
				.build();
	}
	
	public MemberDto toDto(Member member) {
		return MemberDto.builder()
				.member_no(member.getMemberNo())
				.member_id(member.getMemberId())
				.member_pw(member.getMemberPw())
				.member_name(member.getMemberName())
				.pos_no(member.getPos() != null ? member.getPos().getPosNo() : null)
				.dept_no(member.getDept() != null ? member.getDept().getDeptNo() : null)
				.role_no(role_no)
				.dept_name(member.getDept() != null ? member.getDept().getDeptName() : null) // 04/09 JJI 사원 부서명 불러오기위해 추가
				.pos_name(member.getPos() != null ? member.getPos().getPosName() : null) // 04/09 JJI 사원 부서명 불러오기위해 추가
				.annual_leave(member.getAnnualLeave())
				.signature(member.getSignature())
				.is_absent(member.getIsAbsent())
				.reg_date(member.getRegDate())
				.build();
	}
	

	
}