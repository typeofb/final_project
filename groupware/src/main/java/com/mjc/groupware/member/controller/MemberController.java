package com.mjc.groupware.member.controller;

import java.time.LocalDate;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mjc.groupware.address.service.AddressService;
import com.mjc.groupware.common.annotation.CheckPermission;
import com.mjc.groupware.common.redis.RedisLoginLogService;
import com.mjc.groupware.company.dto.CompanyDto;
import com.mjc.groupware.company.service.CompanyService;
import com.mjc.groupware.dept.dto.DeptDto;
import com.mjc.groupware.dept.entity.Dept;
import com.mjc.groupware.dept.service.DeptService;
import com.mjc.groupware.member.dto.LogPageDto;
import com.mjc.groupware.member.dto.LogRedisDto;
import com.mjc.groupware.member.dto.MemberAttachDto;
import com.mjc.groupware.member.dto.MemberCreateRequestDto;
import com.mjc.groupware.member.dto.MemberCreationDto;
import com.mjc.groupware.member.dto.MemberDto;
import com.mjc.groupware.member.dto.MemberResponseDto;
import com.mjc.groupware.member.dto.MemberSearchDto;
import com.mjc.groupware.member.dto.PageDto;
import com.mjc.groupware.member.dto.RoleDto;
import com.mjc.groupware.member.dto.StatusDto;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.entity.Role;
import com.mjc.groupware.member.security.MemberDetails;
import com.mjc.groupware.member.service.LoginLogService;
import com.mjc.groupware.member.service.MemberAttachService;
import com.mjc.groupware.member.service.MemberService;
import com.mjc.groupware.member.service.RoleService;
import com.mjc.groupware.pos.dto.PosDto;
import com.mjc.groupware.pos.entity.Pos;
import com.mjc.groupware.pos.repository.PosRepository;
import com.mjc.groupware.pos.service.PosService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MemberController {
	
	private Logger logger = LoggerFactory.getLogger(MemberController.class);
	
	private static int cnt = 0;
	
	private final MemberService service;
	private final MemberAttachService memberAttachService;
	private final PosService posService;
	private final DeptService deptService;
	private final RoleService roleService;
	private final AddressService addressService;
	private final CompanyService companyService;
	private final LoginLogService loginLogService;
	private final PosRepository posRepository;
	
	private final RedisLoginLogService redisLoginLogService;
	
	@GetMapping("/login")
	public String loginView(
			@RequestParam(value="error", required=false) String error,
			@RequestParam(value="errorMsg", required=false) String errorMsg,
			Model model) {
		
		if(error == null) {
			cnt = 0;
		} else {
			cnt++;
		}
		
		model.addAttribute("error", error);
		model.addAttribute("errorMsg", errorMsg);
		
		System.out.println("MemberController - 로그인 연속 실패 횟수 : "+cnt+"회");
		
		return "member/login";
	}
	
	@GetMapping("/forgetPassword")
	public String forgetPasswordView() {
		return "member/forgetPassword";
	}
	
	@PostMapping("/forgetPassword")
	@ResponseBody
	public Map<String, String> checkUsernameAndEmail(MemberDto dto, HttpSession session) {
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "계정 조회 중 알 수 없는 오류가 발생하였습니다.");
		
		try {
			Member member = service.selectMemberOneByMemberIdAndMemberEmail(dto);
			
			if(member != null) {
				// 인증을 위한 코드를 만드는 부분
				String verificationCode = generateVerificationCode();

				// 실제로 SMTP를 통해 이메일을 보내는 부분
				service.sendVerificationEmail(dto.getMember_email(), verificationCode);
				
				// 인증코드를 세션에 저장하는 부분 - 예시: session.setAttribute("verificationCode", verificationCode);
				session.setAttribute("verificationCode", verificationCode);
				session.setAttribute("verificationCodeCreatedAt", System.currentTimeMillis());
				
				session.setAttribute("target_member_id", dto.getMember_id());
				session.setAttribute("target_member_email", dto.getMember_email());
				session.setAttribute("target_member_created_at", System.currentTimeMillis());
				
				resultMap.put("res_code", "200");
				resultMap.put("res_msg", "이메일이 성공적으로 발송되었습니다.");
			} else {
	            resultMap.put("res_code", "404");
	            resultMap.put("res_msg", "일치하는 계정을 찾을 수 없습니다.");
	        }
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
	        resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "계정 조회 중 알 수 없는 오류가 발생하였습니다.");
		}
		
		return resultMap;
	}
	
	private String generateVerificationCode() {
	    Random random = new Random();
	    StringBuilder code = new StringBuilder();
	    
	    // 6자리 랜덤 숫자 생성
	    for(int i = 0; i < 6; i++) {
	        code.append(random.nextInt(10));
	    }
	    
	    return code.toString();
	}
	
	@GetMapping("/forgetPassword/verifyCode")
	public String verifyCodeView(HttpSession session, RedirectAttributes redirectAttributes) {
		Object memberId = session.getAttribute("target_member_id");
	    Object memberEmail = session.getAttribute("target_member_email");
	    Long memberTimestamp = (Long) session.getAttribute("target_member_created_at");
	    
	    if(memberId == null || memberEmail == null) {
	        redirectAttributes.addFlashAttribute("error_msg", "인증되지 않은 접근입니다.");
	        return "redirect:/forgetPassword";
	    }
	    
	    long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - memberTimestamp;
        
        if(elapsedTime > 5 * 60 * 1000) {
        	redirectAttributes.addFlashAttribute("error_msg", "요청한 비밀번호 재설정 시간이 만료되었습니다. 다시 시도해주세요.");
	        return "redirect:/forgetPassword";
        }
        
        session.setAttribute("target_member_created_at", System.currentTimeMillis());
		
		return "member/verifyCode";
	}
	
	@PostMapping("/forgetPassword/verifyCode")
	@ResponseBody
	public Map<String, String> verifyCodeApi(@RequestParam("auth_code") String authCode, HttpSession session) {
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "코드 인증 중 알 수 없는 오류가 발생하였습니다.");
		
		try {
			String verificationCode = (String) session.getAttribute("verificationCode");
			Long verificationCodeCreatedAt = (Long) session.getAttribute("verificationCodeCreatedAt");
			
			if (verificationCode == null || verificationCodeCreatedAt == null) {
	            throw new IllegalArgumentException("인증번호가 존재하지 않거나 만료되었습니다.");
	        }
			
			long now = System.currentTimeMillis();
	        long elapsed = now - verificationCodeCreatedAt;
	        
	        if (elapsed > 5 * 60 * 1000) { // 인증 코드는 5분간만 유효함.
	            session.removeAttribute("verificationCode");
	            session.removeAttribute("verificationCodeCreatedAt");
	            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 요청해주세요.");
	        }
	        
	        if (!verificationCode.equals(authCode)) {
	            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
	        }
			
	        session.removeAttribute("verificationCode");
	        session.removeAttribute("verificationCodeCreatedAt");
	        
	        resultMap.put("res_code", "200");
	        resultMap.put("res_msg", "인증번호가 확인되었습니다.");
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
	        resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "코드 인증 중 알 수 없는 오류가 발생하였습니다.");
		}
		
		return resultMap;
	}
	
	@GetMapping("/forgetPassword/changePassword")
	public String changePasswordView(HttpSession session, RedirectAttributes redirectAttributes) {
		Object memberId = session.getAttribute("target_member_id");
	    Object memberEmail = session.getAttribute("target_member_email");
	    Long memberTimestamp = (Long) session.getAttribute("target_member_created_at");
	    
	    if (memberId == null || memberEmail == null) {
	        redirectAttributes.addFlashAttribute("error_msg", "인증되지 않은 접근입니다.");
	        return "redirect:/forgetPassword";
	    }
	    
	    long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - memberTimestamp;
        
        if(elapsedTime > 5 * 60 * 1000) {
        	redirectAttributes.addFlashAttribute("error_msg", "요청한 비밀번호 재설정 시간이 만료되었습니다. 다시 시도해주세요.");
	        return "redirect:/forgetPassword";
        }
        
        session.removeAttribute("target_member_created_at");
		
		return "member/changePassword";
	}
	
	@PostMapping("/forgetPassword/changePassword")
	@ResponseBody
	public Map<String, String> changePasswordApi(@RequestParam("member_pw") String memberPw, HttpSession session) {
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "비밀번호 변경 중 알 수 없는 오류가 발생하였습니다.");
		
		try {
			// 서버단에서의 복잡도 검증
			String newPw = memberPw;
			
			if (!isValidPassword(newPw)) {
	            throw new IllegalArgumentException("비밀번호는 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
	        }
			
			String memberId = (String) session.getAttribute("target_member_id");
			String memberEmail = (String) session.getAttribute("target_member_email");
			
			Member target = service.selectMemberOneByMemberIdAndMemberEmail(MemberDto.builder()
					.member_id(memberId)
					.member_email(memberEmail)
					.build());
			
			if(target != null) {
				// 실질적인 비밀번호 수정 로직
				MemberDto dto = MemberDto.builder()
										.member_no(target.getMemberNo())
										.member_pw(memberPw)
										.build();
				
				service.updateMemberForgetPw(dto);
				
				// 세션을 지워주는 로직
				session.removeAttribute("target_member_id");
	            session.removeAttribute("target_member_email");
				
				resultMap.put("res_code", "200");
		        resultMap.put("res_msg", "비밀번호 변경이 성공적으로 완료되었습니다.");
			} else {
				resultMap.put("res_code", "404");
	            resultMap.put("res_msg", "일치하는 계정을 찾을 수 없습니다.");
			}
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
	        resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "비밀번호 변경 중 알 수 없는 오류가 발생하였습니다.");
		}
		
		return resultMap;
	}
	
	@CheckPermission("MEMBER_ADMIN_C")
	@GetMapping("/admin/member/create")
	public String createMemberView(Model model) {
		
		List<Pos> posList = posService.selectPosAllByPosOrderAsc();
		List<Dept> deptList = deptService.SelectDeptAllOrderByDeptNameAsc();
		CompanyDto companyDto = companyService.selectLatestCompanyProfile();
		
		model.addAttribute("posList", posList);
		model.addAttribute("deptList", deptList);
		model.addAttribute("companyDto", companyDto);
		
		if(companyDto != null) {
			String companyInitial = companyDto.getCompany_initial();
			if(companyInitial != null) {
				String entryYear = String.valueOf(LocalDate.now(ZoneId.of("Asia/Seoul")).getYear());
				String yearSuffix = entryYear.equals("2025") ? "25" : entryYear.substring(2); 
				Long lastMemberNo = service.selectMemberOneByLastNo().getMemberNo();
				Long newMemberNo = (lastMemberNo != null) ? lastMemberNo + 1 : 1L;
				
				String defaultId = companyInitial + entryYear + String.format("%05d", newMemberNo % 100000);
				String defaultPw = companyInitial.toUpperCase() + "@" + yearSuffix + String.format("%02d", newMemberNo % 100);
				
				MemberCreationDto creationDto = MemberCreationDto.builder()
						.defaultMemberId(defaultId)
						.defaultMemberPw(defaultPw)
						.build();
				
				model.addAttribute("memberCreationDto", creationDto);				
			}
		}
		
		Optional<Pos> maxOrderPos = posRepository.findTopByOrderByPosOrderDesc();
		maxOrderPos.ifPresent(pos -> model.addAttribute("selectedPosNo", pos.getPosNo()));
		
		return "member/create";
	}
	
	@CheckPermission("MEMBER_ADMIN_C")
	@PostMapping("/admin/member/create")
	@ResponseBody
	public Map<String, String> createMemberApi(MemberCreateRequestDto dto) {
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "사원 등록 도중 알 수 없는 오류가 발생하였습니다.");
		
		try {
			LocalDate regDate = dto.getHire_date();
//			LocalDateTime regDateTime = regDate.atStartOfDay();
			dto.setReg_date(regDate);
			
			Member member = service.createMember(dto);
			
			if(member != null) {
				resultMap.put("res_code", "200");
				resultMap.put("res_msg", "사원 등록이 성공적으로 완료되었습니다.");
			}
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
	        resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			logger.error("사원 등록 중 오류 발생", e);
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "사원 등록 도중 알 수 없는 오류가 발생하였습니다.");
		}
		
		return resultMap;
	}
	
	@CheckPermission("MEMBER_ADMIN_RU")
	@GetMapping("/admin/member")
	public String selectMemberAll(Model model, MemberSearchDto searchDto, PageDto pageDto) {
		if(pageDto.getNowPage() == 0) pageDto.setNowPage(1);
		
		Page<Member> memberList = service.selectMemberAll(searchDto, pageDto);
		
		pageDto.setTotalPage(memberList.getTotalPages());
		
		List<Dept> deptList = deptService.selectDeptAll();
		List<Pos> posList = posService.selectPosAll();
		List<Role> roleList = roleService.selectRoleAll();

		List<StatusDto> statusList = Arrays.asList(
		        new StatusDto(100, "재직"),
		        new StatusDto(101, "수습"),
		        new StatusDto(102, "인턴"),
		        new StatusDto(200, "파견"),
		        new StatusDto(300, "휴직"),
		        new StatusDto(301, "대기"),
		        new StatusDto(400, "해고"),
		        new StatusDto(401, "은퇴"),
		        new StatusDto(402, "퇴사"),
		        new StatusDto(900, "사망")
		    );
		
		model.addAttribute("memberList", memberList);
		model.addAttribute("deptList", deptList);
		model.addAttribute("posList", posList);
		model.addAttribute("roleList", roleList);
		model.addAttribute("statusList", statusList);
		model.addAttribute("searchText", searchDto.getSearch_text());
		model.addAttribute("pageDto", pageDto);
		
		return "member/list";
	}
	
	@CheckPermission("MEMBER_ADMIN_RU")
	@PostMapping("/admin/member/select")
	@ResponseBody
	public MemberResponseDto selectMemberAllByDeptNoApi(@RequestParam("deptId") String deptId) {
		MemberResponseDto responseDto = new MemberResponseDto();
		
		responseDto.setRes_code("500");
	    responseDto.setRes_msg("사원 조회 중 알 수 없는 오류가 발생했습니다.");
        
        logger.info("deptId: {}", deptId);
        
        try {
        	if(deptId != null && !deptId.trim().isEmpty()) {
        		Long deptNo = Long.parseLong(deptId);
        		
        		List<Member> memberList = service.selectMemberAllByDeptIdByPosOrder(deptNo);
        		
        		List<MemberDto> memberDtoList = new ArrayList<>();
        		for (Member member : memberList) {
        			MemberDto memberDto = MemberDto.builder()
                            .member_no(member.getMemberNo())
                            .member_id(member.getMemberId())
                            .member_pw(member.getMemberPw())
                            .member_name(member.getMemberName())
                            .member_birth(member.getMemberBirth())
                            .member_gender(member.getMemberGender())
                            .member_addr1(member.getMemberAddr1())
                            .member_addr2(member.getMemberAddr2())
                            .member_addr3(member.getMemberAddr3())
                            .member_email(member.getMemberEmail())
                            .member_phone(member.getMemberPhone())
                            .pos_no(member.getPos() != null ? member.getPos().getPosNo() : null)
                            .dept_no(member.getDept() != null ? member.getDept().getDeptNo() : null)
                            .role_no(member.getRole() != null ? member.getRole().getRoleNo() : null)
                            .status(member.getStatus())
                            .dept_name(member.getDept() != null ? member.getDept().getDeptName() : null)
                            .pos_name(member.getPos() != null ? member.getPos().getPosName() : null)
                            .role_name(member.getRole() != null ? member.getRole().getRoleName() : null)
                            .build();
        			
        			memberDtoList.add(memberDto);						
                }
        		
        		List<Dept> deptList = deptService.SelectDeptAllOrderByDeptNameAsc();
        		List<DeptDto> deptDtoList = new ArrayList<>();
        		
        		for(Dept dept : deptList) {
        			DeptDto deptDto = DeptDto.builder()
        					.dept_no(dept.getDeptNo())
        					.dept_name(dept.getDeptName())
        					.build();
        			
        			deptDtoList.add(deptDto);
        		}
        		
        		
        		List<Pos> posList = posService.selectPosAllByPosOrderAsc();
        		List<PosDto> posDtoList = new ArrayList<>();
        		
        		for(Pos pos : posList) {
        		    PosDto posDto = PosDto.builder()
        		        .pos_no(pos.getPosNo())
        		        .pos_name(pos.getPosName())
        		        .pos_order(pos.getPosOrder())
        		        .build();

        		    posDtoList.add(posDto);
        		}
        		
        		List<Role> roleList = roleService.selectRoleAll();
        		List<RoleDto> roleDtoList = new ArrayList<>();
        		
        		for(Role role : roleList) {
        		    RoleDto roleDto = RoleDto.builder()
        		        .role_no(role.getRoleNo())
        		        .role_name(role.getRoleName())
	        		    .role_nickname(role.getRoleNickname())
        		        .build();

        		    roleDtoList.add(roleDto);
        		}

        		responseDto.setDept_no(deptNo);
        		responseDto.setMember_list_by_dept(memberDtoList);
        		responseDto.setDept_list_all(deptDtoList);
        		responseDto.setPos_list_all(posDtoList);
        		responseDto.setRole_list_all(roleDtoList);
        		                
                responseDto.setRes_code("200");
                responseDto.setRes_msg("사원 목록 조회가 성공적으로 완료되었습니다.");
			}
        } catch(IllegalArgumentException e) {
        	responseDto.setRes_code("400");
        	responseDto.setRes_msg(e.getMessage());
		} catch(Exception e) {
			logger.error("사원 조회 중 오류 발생", e);
			responseDto.setRes_code("500");
			responseDto.setRes_msg("사원 조회 중 알 수 없는 오류가 발생했습니다.");
		}
        
		return responseDto;
	}
	
	@CheckPermission("MEMBER_ADMIN_RU")
	@PostMapping("/admin/member/update")
	@ResponseBody
	public Map<String, String> updateMember(@RequestBody MemberResponseDto dto) {
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "사원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
		
		logger.info("MemberResponseDto: {}", dto);
		
		try {			
			service.updateMember(dto);
			
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "사원 정보 수정이 성공적으로 완료되었습니다.");
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
			resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "사원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
		}
		
		return resultMap;
	}
	
	@CheckPermission("MEMBER_ADMIN_RU")
	@PostMapping("/admin/members/update")
	@ResponseBody
	public Map<String, String> updateMembersApi(@RequestBody List<MemberResponseDto> dtoList) {
		// 체크박스를 통한 사원정보 다중 수정
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "사원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
		
		logger.info("List<MemberResponseDto>: {}", dtoList);
		
		try {
			for(MemberResponseDto dto : dtoList) {
				service.updateMember(dto);
			}
			
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "사원 정보 수정이 성공적으로 완료되었습니다.");
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
			resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "사원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
		}
		
		return resultMap;
	}
	
	@CheckPermission("MEMBER_ADMIN_RU")
	@PostMapping("/admin/memberAll/update")
	@ResponseBody
	public Map<String, String> updateMemberAllApi(@RequestBody List<MemberResponseDto> dtoList) {
		// 체크박스를 통한 사원정보 다중 수정
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "사원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
		
		logger.info("List<MemberResponseDto>: {}", dtoList);
		
		try {
			for(MemberResponseDto dto : dtoList) {
				
				service.updateMember(dto);
			}
			
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "사원 정보 수정이 성공적으로 완료되었습니다.");
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
			resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "사원 정보 수정 중 알 수 없는 오류가 발생했습니다.");
		}
		
		return resultMap;
	}
	
	@GetMapping("/member/{id}/update")
	public String updateMyProfileView(@PathVariable("id") Long memberNo,
										@RequestParam(name = "loginDateStart", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate loginStartDate,
										@RequestParam(name = "loginDateEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate loginEndDate,
										Model model,
										LogPageDto pageDto) {
		// 본인이 아닌데 URL 을 바꿔서 진입하려고 하면 Security에 의해 차단해야 함
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("로그인 정보가 없습니다.");
		}
		
		MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
		Long currentMemberNo = memberDetails.getMember().getMemberNo();
		
		if (!memberNo.equals(currentMemberNo)) {
	        throw new AccessDeniedException("접근 권한이 없습니다.");
	    }
		
		// 각각의 사원이 본인의 정보를 수정하는 페이지로 이동
		Member member = service.selectMemberOneByMemberNo(MemberDto.builder().member_no(memberNo).build());
		if (member == null) {
		    throw new EntityNotFoundException("해당 회원을 찾을 수 없습니다.");
		}
		
		List<String> addr1List = addressService.selectAddr1Distinct();
		List<String> addr2List = addressService.selectAddr2ByAddr1(member.getMemberAddr1());
		
		MemberAttachDto memberAttachDto = memberAttachService.selectLatestMyProfile(memberDetails.getMember());
		
		model.addAttribute("member", member);
		model.addAttribute("addr1List", addr1List);
		model.addAttribute("addr2List", addr2List);
		model.addAttribute("selectedAddr1", member.getMemberAddr1());
		model.addAttribute("selectedAddr2", member.getMemberAddr2());
		model.addAttribute("memberAttachDto", memberAttachDto);
		
		// MariaDB에서 로그인 이력을 조회
//		if(pageDto.getNowPage() == 0) pageDto.setNowPage(1);
		
//		Page<LoginLog> loginLogs = loginLogService.findByMemberOrderByLoginTimeDesc(member, loginStartDate, loginEndDate, pageDto);
//		
//		pageDto.setTotalPage(loginLogs.getTotalPages());
//		
//		model.addAttribute("loginLogs", loginLogs);
//		model.addAttribute("pageDto", pageDto);
//		
//		List<String> loginLogWithBrowserInfo = new ArrayList<>();
//		for (LoginLog log : loginLogs) {
//	        String browserInfo = getBrowserInfo(log.getLoginAgent());
//	        loginLogWithBrowserInfo.add(browserInfo);
//	    }
//		
//		model.addAttribute("loginLogWithBrowserInfo", loginLogWithBrowserInfo);
//		
//		List<String> loginLogWithDeviceInfo = new ArrayList<>();
//		for (LoginLog log : loginLogs) {
//	        String deviceInfo = getDeviceType(log.getLoginAgent());
//	        loginLogWithDeviceInfo.add(deviceInfo);
//	    }
//
//		model.addAttribute("loginLogWithDeviceInfo", loginLogWithDeviceInfo);
//		
//		model.addAttribute("loginDateStart", loginStartDate);
//		model.addAttribute("loginDateEnd", loginEndDate);
		
		// Redis에서 로그인 이력을 조회
		if(pageDto.getNowPage() == 0) pageDto.setNowPage(1);
		
		List<LogRedisDto> loginLogList = redisLoginLogService.getLoginLogs(memberNo, loginStartDate, loginEndDate);
		
		// 총 페이지 수 계산 (페이징 처리)
		int totalLogs = loginLogList.size();
		int totalPages = (totalLogs + pageDto.getNumPerPage() - 1) / pageDto.getNumPerPage();
		pageDto.setTotalPage(totalPages);
		
		// 페이징에 맞는 로그인 로그 추출
	    int startIndex = (pageDto.getNowPage() - 1) * pageDto.getNumPerPage();
	    int endIndex = Math.min(startIndex + pageDto.getNumPerPage(), totalLogs);
	    List<LogRedisDto> paginatedLogList = loginLogList.subList(startIndex, endIndex);
   
	    List<String> loginLogWithBrowserInfo = new ArrayList<>();
	    List<String> loginLogWithDeviceInfo = new ArrayList<>();
	    
	    for (LogRedisDto log : paginatedLogList) {
	        String browserInfo = getBrowserInfo(log.getLoginAgent());
	        loginLogWithBrowserInfo.add(browserInfo);
	        
	        String deviceInfo = getDeviceType(log.getLoginAgent());
	        loginLogWithDeviceInfo.add(deviceInfo);
	    }
	    
	    model.addAttribute("loginLogs", paginatedLogList);
	    
	    model.addAttribute("loginLogWithBrowserInfo", loginLogWithBrowserInfo);
	    model.addAttribute("loginLogWithDeviceInfo", loginLogWithDeviceInfo);
	    
	    model.addAttribute("loginDateStart", loginStartDate);
	    model.addAttribute("loginDateEnd", loginEndDate);
	    
	    model.addAttribute("pageDto", pageDto);
	    
		return "member/myPage";
	}
	
	private String getBrowserInfo(String userAgent) {
	    // 브라우저 정보에서 브라우저 이름만 파싱하는 메소드
	    if (userAgent.contains("Edg")) {
	        // "Edg/"로 정확한 버전 추출
	        String[] edgeParts = userAgent.split("Edg/");
	        if (edgeParts.length > 1) {
	            String version = edgeParts[1].split(" ")[0];
	            String[] versionParts = version.split("\\.");
	            return "Edge " + versionParts[0] + "." + versionParts[1];
	        }
	    } else if (userAgent.contains("Chrome")) {
	        // "Chrome/" 버전이 "Edg/"보다 뒤에 나오는 경우 처리
	        String[] chromeParts = userAgent.split("Chrome/");
	        if (chromeParts.length > 1) {
	            String version = chromeParts[1].split(" ")[0];
	            String[] versionParts = version.split("\\.");
	            return "Chrome " + versionParts[0] + "." + versionParts[1];
	        }
	    } else if (userAgent.contains("Firefox")) {
	        String version = userAgent.split("Firefox/")[1].split(" ")[0];
	        String[] versionParts = version.split("\\.");
	        return "Firefox " + versionParts[0] + "." + versionParts[1];
	    } else if (userAgent.contains("Safari") && userAgent.contains("Version")) {
	        String version = userAgent.split("Version/")[1].split(" ")[0];
	        String[] versionParts = version.split("\\.");
	        return "Safari " + versionParts[0] + "." + versionParts[1];
	    } else if (userAgent.contains("Trident")) {
	        String version = userAgent.split("rv:")[1].split(" ")[0];
	        String[] versionParts = version.split("\\.");
	        return "IE " + versionParts[0] + "." + versionParts[1];
	    }
	    return "Unknown";
	}
	
	private String getDeviceType(String userAgent) {
		// 브라우저 정보에서 디바이스 이름만 파싱하는 메소드
	    if (userAgent == null) {
	        return "Unknown";
	    }
	    
	    if (userAgent.contains("iPhone")) {
	        return "iPhone";
	    } else if (userAgent.contains("Android")) {
	        return "Android";
	    } else if (userAgent.contains("Windows Phone")) {
	        return "Windows Phone";
	    } else if (userAgent.contains("iPad")) {
	        return "iPad";
	    } else if (userAgent.contains("Tablet")) {
	        return "Tablet";
	    } else if (userAgent.contains("Windows")) {
	        return "Windows";
	    } else if (userAgent.contains("Macintosh")) {
	        return "Mac";
	    } else if (userAgent.contains("Linux")) {
	        return "Linux";
	    }
	    
	    return "Unknown";
	}
	
	@PostMapping("/member/{id}/update/image")
	@ResponseBody
	public Map<String, String> updateMemberProfileImage(@PathVariable("id") Long memberNo, MemberAttachDto memberAttachDto) {
		// 본인이 아닌데 URL 을 바꿔서 진입하려고 하면 Security에 의해 차단해야 함
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("로그인 정보가 없습니다.");
		}
		
		MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
		Long currentMemberNo = memberDetails.getMember().getMemberNo();
		
		if (!memberNo.equals(currentMemberNo)) {
			throw new AccessDeniedException("접근 권한이 없습니다.");
		}
		
		// 각각의 사원이 본인의 프로필 이미지를 수정하는 로직
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "프로필 정보 수정 중 알 수 없는 오류가 발생하였습니다.");
		
		if(memberAttachDto != null) {
			try {
				MemberAttachDto existingProfile = memberAttachService.selectLatestMyProfile(Member.builder().memberNo(memberNo).build());
		        String previousFilePath = (existingProfile != null) ? existingProfile.getAttach_path() : null;
				
				MultipartFile file = memberAttachDto.getProfile_image();
				
				MemberAttachDto param = memberAttachService.uploadFile(file);
				param.setMember_no(memberNo);
				
				memberAttachService.updateMyProfile(param);
				
				if (previousFilePath != null) {
	                memberAttachService.deleteFile(previousFilePath);
	            }
				
				resultMap.put("res_code", "200");
				resultMap.put("res_msg", "프로필 사진 수정이 성공적으로 완료되었습니다.");
			} catch(Exception e) {
				logger.error("프로필 사진 수정 중 오류 발생", e);
				resultMap.put("res_code", "500");
		        resultMap.put("res_msg", "프로필 사진 수정 중 알 수 없는 오류가 발생했습니다.");
			}
		}
		
		return resultMap;
		
	}
	
	@PostMapping("/member/{id}/update/pw")
	@ResponseBody
	public Map<String, String> updateMemberPw(@PathVariable("id") Long memberNo, MemberDto dto, HttpServletResponse response) {
		// 본인이 아닌데 URL 을 바꿔서 진입하려고 하면 Security에 의해 차단해야 함
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("로그인 정보가 없습니다.");
		}
		
		MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
		Long currentMemberNo = memberDetails.getMember().getMemberNo();
		
		if (!memberNo.equals(currentMemberNo)) {
			throw new AccessDeniedException("접근 권한이 없습니다.");
		}
		
		Map<String, String> resultMap = new HashMap<>();

		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "비밀번호 수정 중 알 수 없는 오류가 발생하였습니다.");
		
		try {
			// 서버단에서의 복잡도 검증
			String newPw = dto.getMember_new_pw();
			
			if (!isValidPassword(newPw)) {
	            throw new IllegalArgumentException("비밀번호는 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.");
	        }
			
			// 실질적인 비밀번호 수정 로직
			dto.setMember_no(memberNo);
			
			service.updateMemberPw(dto);
			
			Cookie rememberMeCookie = new Cookie("remember-me", null);
			rememberMeCookie.setMaxAge(0);
			rememberMeCookie.setPath("/");
			response.addCookie(rememberMeCookie);

			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "비밀번호 수정이 성공적으로 완료되었습니다.");
		} catch(IllegalArgumentException e) {
			logger.warn("비밀번호 수정 실패 - 잘못된 입력: {}", e.getMessage());
			resultMap.put("res_code", "400");
			resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			logger.error("비밀번호 수정 중 오류 발생", e);
			resultMap.put("res_code", "500");
	        resultMap.put("res_msg", "비밀번호 수정 중 알 수 없는 오류가 발생하였습니다.");
		}
		
		return resultMap;
	}
	
	// 비밀번호 정규식 검사를 위한 private 메소드 - updateMemberPw() 에서 끌어다 씀
	private boolean isValidPassword(String password) {
	    if (password == null) return false;
	    
	    boolean lengthCheck = password.length() >= 8;
	    boolean numberCheck = password.matches(".*[0-9].*");
	    boolean specialCheck = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
	    
	    return lengthCheck && numberCheck && specialCheck;
	}
	
	@PostMapping("/member/{id}/update")
	@ResponseBody
	public Map<String, String> updateMemberProfileInfo(@PathVariable("id") Long memberNo, MemberDto dto) {
		// 본인이 아닌데 URL 을 바꿔서 진입하려고 하면 Security에 의해 차단해야 함
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("로그인 정보가 없습니다.");
		}
		
		MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();
		Long currentMemberNo = memberDetails.getMember().getMemberNo();
		
		if (!memberNo.equals(currentMemberNo)) {
			throw new AccessDeniedException("접근 권한이 없습니다.");
		}
		
		Map<String, String> resultMap = new HashMap<>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "개인정보 수정 중 알 수 없는 오류가 발생하였습니다.");

		try {
			dto.setMember_no(memberNo);
			
			service.updateMemberInfo(dto);
			
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "개인정보 수정이 성공적으로 완료되었습니다.");
		} catch(IllegalArgumentException e) {
			logger.warn("개인정보 수정 실패 - 회원정보가 존재하지 않음: {}", e.getMessage());
			resultMap.put("res_code", "400");
			resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			logger.error("개인정보 수정 중 오류 발생", e);
			resultMap.put("res_code", "500");
	        resultMap.put("res_msg", "개인정보 수정 중 알 수 없는 오류가 발생하였습니다.");
		}
		
		return resultMap;
	}

	 // 결재라인 부서의 속한 사원들 select
	 @GetMapping("/member/dept/{id}")
	 @ResponseBody
	 public List<MemberDto> selectMemberAllByDeptId(@PathVariable("id") Long id) {
		 List<Member> memberList = service.selectMemberAllByDeptId(id);
		 List<MemberDto> memberDtoList = new ArrayList<MemberDto>();
		 for(Member m : memberList) {
			 MemberDto dto = new MemberDto().toDto(m);
			 memberDtoList.add(dto);
		 }
		 
		 return memberDtoList;
	 }
	 
	 @GetMapping("/member/{id}")
	 @ResponseBody
	 public MemberDto selectMemberOneById(@PathVariable("id") Long id) {
		 MemberDto dto = new MemberDto();
		 dto.setMember_no(id);
		 Member member = service.selectMemberOneByMemberNo(dto);
		 MemberDto memberDto = new MemberDto().toDto(member);
		 return memberDto;
	 }
	 
	 @PostMapping("/member/create/signature")
	 @ResponseBody
	 public Map<String,String> createSignatureApi(@RequestParam("memberNo") Long memberNo,
		        @RequestParam("signature") String signature) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "전자서명 저장에 실패하였습니다.");
		
		int result = 0;
		
		result = service.createSignatureApi(memberNo, signature);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "전자서명 저장에 성공하였습니다.");
		}
		
		return resultMap;
	 }
	 
	 @PostMapping("/member/update/signature")
	 @ResponseBody
	 public Map<String,String> updateSignatureApi(@RequestParam("memberNo") Long memberNo,
		        @RequestParam("updateSignature") String signature) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "전자서명 변경에 실패하였습니다.");
		
		int result = 0;
		
		result = service.createSignatureApi(memberNo, signature);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "전자서명 변경에 성공하였습니다.");
		}
		
		return resultMap;
	 }
	

	@CheckPermission("MEMBER_ADMIN_RU")
	@PostMapping("/admin/member/reset-password")
	@ResponseBody
	public Map<String, String> resetMemberPasswordApi(@RequestBody Map<String, Object> paramMap) {
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "비밀번호 초기화 중 오류가 발생했습니다.");
		
		try {
			if (paramMap.get("member_no") == null) {
				resultMap.put("res_code", "400");
				resultMap.put("res_msg", "사원 번호가 유효하지 않습니다.");
				return resultMap;
			}
			
			Long memberNo = Long.parseLong(paramMap.get("member_no").toString());
			String newPassword = paramMap.get("new_password") != null ? paramMap.get("new_password").toString() : "1234";
			
			service.adminResetMemberPassword(memberNo, newPassword);
			
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "비밀번호가 성공적으로 초기화되었습니다. (초기 비밀번호: " + newPassword + ")");
		} catch(IllegalArgumentException e) {
			resultMap.put("res_code", "400");
			resultMap.put("res_msg", e.getMessage());
		} catch(Exception e) {
			logger.error("관리자 비밀번호 초기화 오류", e);
			resultMap.put("res_code", "500");
			resultMap.put("res_msg", "비밀번호 초기화 처리 중 오류가 발생했습니다.");
		}
		
		return resultMap;
	}

	// 부재중 상태 토글 API
	@PostMapping("/member/absent/toggle")
	@ResponseBody
	public Map<String, String> toggleAbsentStatusApi(@AuthenticationPrincipal UserDetails userDetails) {
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "부재 상태 변경 실패");
		if (userDetails != null) {
			String userId = userDetails.getUsername();
			MemberDto memberDto = new MemberDto();
			memberDto.setMember_id(userId);
			Member member = service.selectMemberOne(memberDto);
			if (member != null) {
				String newStatus = service.toggleAbsentStatus(member.getMemberNo());
				// 1. 최신 Member 정보를 DB에서 재조회
				Member updatedMember = service.selectMemberOne(memberDto);
				// 2. SecurityContext의 Authentication 객체 갱신
				MemberDetails updatedUserDetails = new MemberDetails(updatedMember);
				Authentication newAuth = new UsernamePasswordAuthenticationToken(
						updatedUserDetails,
						updatedUserDetails.getPassword(),
						updatedUserDetails.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(newAuth);
				resultMap.put("res_code", "200");
				resultMap.put("is_absent", newStatus);
				resultMap.put("res_msg", "Y".equals(newStatus) ? "부재중 상태로 설정되었습니다." : "부재중 상태가 해제되었습니다.");
			}
		}
		return resultMap;
	}
}
