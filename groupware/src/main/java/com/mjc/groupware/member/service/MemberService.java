package com.mjc.groupware.member.service;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mjc.groupware.dept.entity.Dept;
import com.mjc.groupware.dept.repository.DeptRepository;
import com.mjc.groupware.member.dto.MemberCreateRequestDto;
import com.mjc.groupware.member.dto.MemberDto;
import com.mjc.groupware.member.dto.MemberResponseDto;
import com.mjc.groupware.member.dto.MemberSearchDto;
import com.mjc.groupware.member.dto.PageDto;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.entity.Role;
import com.mjc.groupware.member.repository.MemberRepository;
import com.mjc.groupware.member.security.MemberDetails;
import com.mjc.groupware.member.specification.MemberSpecification;
import com.mjc.groupware.pos.entity.Pos;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class MemberService {
	
	private final MemberRepository repository;
	private final DeptRepository deptRepository;
	private final PasswordEncoder passwordEncoder;
	private final DataSource dataSource;
	private final UserDetailsService userDetailsService;
	
	private final JavaMailSender mailSender;
	
	public Member selectMemberOne(MemberDto dto) {
		Member result = repository.findByMemberId(dto.getMember_id());
		
		return result;
	}
	
	public Member selectMemberOneByMemberNo(MemberDto dto) {
		Member result = repository.findById(dto.getMember_no()).orElse(null);
		
		return result;
	}
	
	public List<Member> selectMemberAll() {
		List<Member> resultList = repository.findAll();
		
		return resultList;
	}
	
	public Page<Member> selectMemberAll(MemberSearchDto searchDto, PageDto pageDto) {		
		Specification<Member> spec = (root,query,criteriaBuilder) -> null;
		
		Pageable pageable = PageRequest.of(pageDto.getNowPage()-1, pageDto.getNumPerPage(), Sort.by("memberNo").ascending());
		
		if("".equals(searchDto.getSearch_text()) || searchDto.getSearch_text() == null) {
			// 아무것도 입력하지않으면 findAll() 과 동일함
			spec = spec.and(MemberSpecification.memberNotAdmin());
		} else {
			
			spec = spec.and(MemberSpecification.memberNameContains(searchDto.getSearch_text()))
					.and(MemberSpecification.memberNotAdmin());
			
			try {
				Long memberNo = Long.parseLong(searchDto.getSearch_text());
				spec = spec.or(MemberSpecification.memberNoEquals(memberNo));
			} catch(Exception e) {
				
			}
		}
		
		Page<Member> resultList = repository.findAll(spec, pageable);
		
		return resultList;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public Member createMember(MemberCreateRequestDto dto) {
		Member result = null;
		
		try {
			dto.setMember_pw(passwordEncoder.encode(dto.getMember_pw()));
			
			result = repository.save(Member.builder()
					.memberId(dto.getMember_id())
					.memberPw(dto.getMember_pw())
					.memberName(dto.getMember_name())
					.pos(dto.getPos_no() != 0 ?	Pos.builder().posNo(dto.getPos_no()).build() : null)
					.dept(dto.getDept_no() != 0 ? Dept.builder().deptNo(dto.getDept_no()).build() : null)
					.role(Role.builder().roleNo((long)2).build())
					.status(100)
					.regDate(dto.getReg_date())
					.annualLeave(15.0)
					.build());
					
		} catch(DataIntegrityViolationException e) {
			throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
		}
		
		return result;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void transferMembersOfDept(Long fromDeptNo, Long toDeptNo) {
		// 삭제인 경우(get_status == 3 인 경우 - 해당 부서의 멤버 구성원을 선택한 부서로 이관시켜주는 작업
		List<Member> members = repository.findAllByDept_DeptNo(fromDeptNo);
		
		Dept transferDept = null;
		if (toDeptNo != null) {
			transferDept = deptRepository.findById(toDeptNo)
					.orElseThrow(() -> new IllegalArgumentException("이관 대상 부서가 존재하지 않습니다."));
		} // 이 또한 영속성 예외를 방지하기 위함 - ID 기준으로 한 번 더 찾아서 그 후에 작업을 시행하는 느낌 - JPA에서 권장하는 방식임
		
		for (Member member : members) {
	        member.changeDept(transferDept);
	    } // 예전에 배웠던 방식인데, Entity에는 Setter를 두지 않으면서 무결성을 유지하고, 도메인 메서드를 하나 만들어서 좀 더 비지니스 로직을 명시적으로 표현할 수 있음
		
	}
	
	public void updateMemberPw(MemberDto dto) {
		try {
			Specification<Member> spec = (root, query, criteriaBuilder) -> null;
			spec = spec.and(MemberSpecification.equalMemberNo(dto.getMember_no()));
			
			Member target = repository.findOne(spec).orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다"));
			
			if(!passwordEncoder.matches(dto.getMember_pw(), target.getMemberPw())) {
				throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
			}
			
			target.changePassword(passwordEncoder.encode(dto.getMember_new_pw()));
			
			repository.save(target);
			
			// 비밀번호 수정 후 -> 자동로그인이 있다면 자동로그인을 해제
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql = "DELETE FROM persistent_logins WHERE username = ?";
			jdbcTemplate.update(sql, dto.getMember_id());
			
			// 비밀번호 수정 후 -> 로그아웃 시키지 않고, 인증정보를 바꾸고 싶을 때 - 비밀번호 외 비교적 간단한 정보 바꿀 때
//			UserDetails updatedUserDetails = userDetailsService.loadUserByUsername(dto.getMember_id());
//			Authentication newAuth = new UsernamePasswordAuthenticationToken(
//					updatedUserDetails,
//					updatedUserDetails.getPassword(),
//					updatedUserDetails.getAuthorities());
//			SecurityContextHolder.getContext().setAuthentication(newAuth);
			
			// 비밀번호 수정 후 -> 로그인 된 사원의 인증 상태를 해제 (즉, 인증이 풀리면서 /login 으로 강제로 끌려들어감)
			SecurityContextHolder.getContext().setAuthentication(null);
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch(Exception e) {
			throw new RuntimeException("비밀번호 수정 중 알 수 없는 문제가 발생했습니다.");
		}
	}
	
	public void updateMemberForgetPw(MemberDto dto) {
		try {
			Specification<Member> spec = (root, query, criteriaBuilder) -> null;
			spec = spec.and(MemberSpecification.equalMemberNo(dto.getMember_no()));
			
			Member target = repository.findOne(spec).orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다"));
			
			target.changePassword(passwordEncoder.encode(dto.getMember_pw()));
			
			repository.save(target);
			
			// 비밀번호 수정 후 -> 자동로그인이 있다면 자동로그인을 해제
			JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
			String sql = "DELETE FROM persistent_logins WHERE username = ?";
			jdbcTemplate.update(sql, dto.getMember_id());
			
			// 비밀번호 수정 후 -> 로그인 된 사원의 인증 상태를 해제 (즉, 인증이 풀리면서 /login 으로 강제로 끌려들어감)
			SecurityContextHolder.getContext().setAuthentication(null);
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch(Exception e) {
			throw new RuntimeException("비밀번호 수정 중 알 수 없는 문제가 발생했습니다.");
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void updateMemberInfo(MemberDto dto) {
		// 앞서 많이 썼지만 @Transaction + 도메인메소드 응용해서 바뀐 부분만 수정하는 로직 - 이렇게 안 하면 데이터로 넣지 않는 부분에 null이 들어감
		try {
			Member target = repository.findById(dto.getMember_no()).orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다."));
			
			target.updateProfileInfo(
			        dto.getMember_name(),
			        dto.getMember_gender(),
			        dto.getMember_birth(),
			        dto.getMember_phone(),
			        dto.getMember_email(),
			        dto.getMember_addr1(),
			        dto.getMember_addr2(),
			        dto.getMember_addr3()
			    );
			
			// 정보를 수정했으므로 MemberDetails를 즉각 수정해줌 : 갱신
			MemberDetails updatedDetails = new MemberDetails(target);
			
			Authentication newAuth = new UsernamePasswordAuthenticationToken(
			        updatedDetails,
			        null,								// 이미 인증된 유저의 세션을 갱신하는 거라서 null 로 놓는 것이 훨씬 안전하다고 함
			        updatedDetails.getAuthorities()
			);
			
			SecurityContextHolder.getContext().setAuthentication(newAuth);
			
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch(Exception e) {
			throw new RuntimeException("개인정보 수정 중 알 수 없는 문제가 발생했습니다.");
		}
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void updateMember(MemberResponseDto dto) {
		// 당연하게도 dto에 삽입된 정보만 바꿀 것이므로 @Transaction + 도메인메소드 활용
		try {
			Member target = repository.findById(dto.getMember_no()).orElseThrow(() -> new IllegalArgumentException("잘못된 요청입니다."));
			
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
			Long currentMemberNo = memberDetails.getMember().getMemberNo();
			
			if (dto.getMember_no().equals(currentMemberNo)) {
			    throw new IllegalArgumentException("자신의 정보는 수정할 수 없습니다.");
			}
			
			Dept currentDept = target.getDept();
			Long currentDeptNo = currentDept != null ? currentDept.getDeptNo() : null;
			
			Dept newDept = dto.getDept_no() != null ? Dept.builder().deptNo(dto.getDept_no()).build() : null;
			
			if (newDept != null && !newDept.getDeptNo().equals(currentDeptNo)) {
	            // 변경하려는 사람이 부서장이라면 해당 부서의 부서장 정보를 null로 수정
	            if (currentDept != null && currentDept.getMember() != null &&
	                currentDept.getMember().getMemberNo().equals(target.getMemberNo())) {
	                currentDept.clearDeptHead();
	            }
	            
	            newDept.changeDeptManager(target);
			}
			
			target.updateMember(
				newDept,
				dto.getPos_no() != null ? Pos.builder().posNo(dto.getPos_no()).build() : null,
				Role.builder().roleNo(dto.getRole_no()).build(),
				dto.getStatus()
			);
			
		} catch(IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch(Exception e) {
			throw new RuntimeException("사원 정보 수정 중 알 수 없는 문제가 발생했습니다.");
		}
	}
	
	// 특정 부서에 속한 모든 사원들을 조회(직급 순서 기준으로 오름차순, 같다면 PK기준으로 오름차순)
	public List<Member> selectMemberAllByDeptIdByPosOrder(Long id) {
		List<Member> memberList = repository.findAllByDeptNoSortedByPosOrder(id);
		return memberList;
	}
	
	// 가장 마지막 pk를 가지고 있는 member 추출
	public Member selectMemberOneByLastNo() {
	    return repository.findTopByOrderByMemberNoDesc();
	}
	
	// 아이디와 이메일을 기준으로 계정이 존재하는지 확인 
	public Member selectMemberOneByMemberIdAndMemberEmail(MemberDto dto) {
		return repository.findByMemberIdAndMemberEmail(dto.getMember_id(), dto.getMember_email());
	}
	
	// 비밀번호 재설정을 위한 인증번호 이메일을 보내는 로직
	public void sendVerificationEmail(String toEmail, String verificationCode) {
		String subject = "[회사명] 비밀번호 재설정 인증번호 안내";
        String content = """
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>비밀번호 재설정 요청</h2>
                    <p>요청하신 인증번호는 아래와 같습니다.</p>
                    <div style="font-size: 24px; font-weight: bold; color: #2d3748;">
                        %s
                    </div>
                    <p style="margin-top: 20px;">인증번호는 5분간 유효합니다.</p>
                </div>
                """.formatted(verificationCode);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalArgumentException("이메일 전송에 실패했습니다.", e);
        }
	}
	
	// 결재라인 부서의 속한 사원들select
	public List<Member> selectMemberAllByDeptId(Long id) { 
		List<Member> memberList = repository.findAllByDept_DeptNo(id); 
		return memberList;
	}
	
	// 전자서명 저장
	@Transactional(rollbackFor=Exception.class)
	public int createSignatureApi(Long member_no, String signature) {
		
		int result = 0;
		
		try {
			Member entity = repository.findById(member_no).orElse(null);

			Member saved = null;
			if(entity != null) {
				entity.createAndUpdateSignature(signature);
				saved = repository.save(entity);
			}
			
			if(saved != null) {
				result = 1;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public Page<Member> selectMemberAllForAnnual(MemberSearchDto searchDto, PageDto pageDto) {		
		Specification<Member> spec = (root,query,criteriaBuilder) -> null;
		
		Pageable pageable = PageRequest.of(pageDto.getNowPage()-1, pageDto.getNumPerPage(), Sort.by("regDate").ascending());
		if(searchDto.getReg_date_order() == 2) {
			pageable = PageRequest.of(pageDto.getNowPage()-1, pageDto.getNumPerPage(), Sort.by("regDate").descending());
		}
		
		if("".equals(searchDto.getSearch_text()) || searchDto.getSearch_text() == null) {
			// 아무것도 입력하지않으면 findAll() 과 동일함
			spec = spec.and(MemberSpecification.memberNotAdmin());
		} else {
			
			spec = spec.and(MemberSpecification.memberNameContains(searchDto.getSearch_text()))
					.and(MemberSpecification.memberNotAdmin());
			
			try {
				Long memberNo = Long.parseLong(searchDto.getSearch_text());
				spec = spec.or(MemberSpecification.memberNoEquals(memberNo));
			} catch(Exception e) {
				
			}
		}
		
		Page<Member> resultList = repository.findAll(spec, pageable);
		
		return resultList;
	}

}
