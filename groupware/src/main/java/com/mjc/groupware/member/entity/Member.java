package com.mjc.groupware.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import com.mjc.groupware.dept.entity.Dept;
import com.mjc.groupware.pos.entity.Pos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name="member")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Member {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="member_no")
	public Long memberNo;
	
	@Column(name="member_id", nullable = false, unique = true)
	public String memberId;
	
	@Column(name="member_pw")
	public String memberPw;
	
	@Column(name="member_name")
	private String memberName;
	
	@Column(name="member_birth")
	private String memberBirth;
	
	@Column(name="member_gender")
	private String memberGender;
	
	@Column(name="member_addr1")
	private String memberAddr1;
	
	@Column(name="member_addr2")
	private String memberAddr2;
	
	@Column(name="member_addr3")
	private String memberAddr3;
	
	@Column(name="member_email")
	private String memberEmail;
	
	@Column(name="member_phone")
	private String memberPhone;

    @Column(name = "status")
    private int status;
    
    @Column(name = "annual_leave")
    private double annualLeave;
    
    @Column(name = "signature", columnDefinition = "LONGTEXT")
    private String signature;
	
	@Column(name="reg_date")
	private LocalDate regDate;
	
	@UpdateTimestamp
	@Column(insertable=false,name="mod_date")
	private LocalDateTime modDate;
	
	@Column(name = "end_date")
	private LocalDateTime endDate;
	
	@ManyToOne
	@JoinColumn(name="dept_no")
	private Dept dept;
	
	@ManyToOne
	@JoinColumn(name="pos_no")
	private Pos pos;
	
	@ManyToOne
	@JoinColumn(name="role_no")
	private Role role;
	
	@OneToMany(mappedBy="member")
	private List<Dept> depts;

	@OneToMany(mappedBy="member")
	private List<MemberAttach> memberAttachs;
	
	@OneToMany(mappedBy="member")
	private List<LoginLog> memberLoginLogs;
	
	public void changePassword(String newEncodedPw) {
	    this.memberPw = newEncodedPw;
	}
	
	public void changeDept(Dept newDept) {
	    this.dept = newDept;
	}
	
	public void updateProfileInfo(String name, String gender, String birth, String phone, String email, String addr1, String addr2, String addr3) {
			this.memberName = name;
			this.memberGender = gender;
			this.memberBirth = birth;
			this.memberPhone = phone;
			this.memberEmail = email;
			this.memberAddr1 = addr1;
			this.memberAddr2 = addr2;
			this.memberAddr3 = addr3;
	}
	
	public void updateMember(Dept dept, Pos pos, Role role, int status) {
		this.dept = dept;
		this.pos = pos;
		this.role = role;
		this.status = status;
	}
	
	public void changeRoleNo(Role role) {
		this.role = role;
	}
	
	public void updateAnnualLeave(double leave) {
		this.annualLeave = this.annualLeave+leave;
	}
	
	public void createAndUpdateSignature(String signature) {
		this.signature = signature;
	}
	
}
