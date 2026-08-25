package com.mjc.groupware.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.mjc.groupware.company.dto.CompanyDto;
import com.mjc.groupware.company.entity.Func;
import com.mjc.groupware.company.entity.FuncMapping;
import com.mjc.groupware.company.repository.FuncMappingRepository;
import com.mjc.groupware.company.service.CompanyService;
import com.mjc.groupware.member.dto.MemberAttachDto;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.security.MemberDetails;
import com.mjc.groupware.member.service.MemberAttachService;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {
	
	private final CompanyService companyService;
	private final MemberAttachService memberAttachService;
	private final FuncMappingRepository funcMappingRepository;
	
	@ModelAttribute("theme")
    public String theme(@CookieValue(value = "theme", defaultValue = "light") String theme) {
		// 모든 페이지 전역으로 쿠키를 뿌리는 로직 - 지금 쿠키에 어떤 테마라고 저장되어있는지 프론트단에 뿌려줌 - 즉, 동적으로 thymeleaf 쓸 수 있게 됨
        return theme;
    }
	
	@ModelAttribute
	public void selectLatestCompanyProfile(Model model) {
		// 모든 페이지에 전역으로 마지막에 등록된 프로필 이미지 파일의 정보를 뿌리는 로직
		// 페이지가 로드될 때마다 service를 찾아가서 repository를 거쳐서 엔티티를 조회한 후 디티오로 바꿔서 가져온 다음 프론트에 뿌림
	    CompanyDto latestProfile = companyService.selectLatestCompanyProfile();
	    
	    if(latestProfile == null) {
	    	return;
	    }
	    
	    String latestProfileName = latestProfile.getCompany_name();
	    model.addAttribute("latestProfile", latestProfile);
	    model.addAttribute("latestProfileName", latestProfileName);
	}
	
	@ModelAttribute
	public void selectLatestMyProfile(Model model, @AuthenticationPrincipal MemberDetails memberDetails) {
		if(memberDetails != null) {
			// 지금 로그인하고 있는 사원이 마지막에 등록한 프로필 이미지 파일의 정보를 뿌리는 로직
			Member member = memberDetails.getMember();
			MemberAttachDto latestMyProfile = memberAttachService.selectLatestMyProfile(member);
			
			if(latestMyProfile == null) {
				return;
			}
			
			model.addAttribute("latestMyProfile", latestMyProfile);
		}
	}
	
	@ModelAttribute
	public void getAccessibleFunNoList(Model model, @AuthenticationPrincipal MemberDetails memberDetails) {
		// 지금 로그인한 사원이 접근할 수 있는 기능PK를 담아서 Set<Long> 반환
		
		// 로그인 상태가 이상하면 빈 set 반환
		if (memberDetails == null || memberDetails.getMember().getRole() == null || memberDetails.getMember().getRole().getRoleNo() == null) {
	        model.addAttribute("accessibleFuncNoList", new HashSet<Long>());
	        return;
	    }
		
		// 지금 로그인한 사원이 접근할 수 있는 funcNo를 뽑아서 전역에 뿌려줌 - func_mapping 테이블에서 조회해오는 로직임
		Long roleNo = memberDetails.getMember().getRole().getRoleNo();
		List<FuncMapping> mappings = funcMappingRepository.findByRole_RoleNo(roleNo);
		
		// 맵핑되는 기능이 없으면 빈 set 반환
		if (mappings == null || mappings.isEmpty()) {
	        model.addAttribute("accessibleFuncNoList", new HashSet<Long>());
	        return;
	    }
		
		// 접근가능한 기능번호 목록을 뿌려줌 + 메뉴의 사용 여부를 뿌려줌
		Set<Long> accessibleFuncNoList = new HashSet<>();
		Map<Long, Integer> funcStatusMap = new HashMap<>();
		
	    for (FuncMapping mapping : mappings) {
	    	Func func = mapping.getFunc();
	    	if(func != null) {
	    		Long funcNo = func.getFuncNo();
	    		Integer funcStatus = func.getFuncStatus(); // 1: 사용, 2: 미사용
	    		
	    		funcStatusMap.put(funcNo, funcStatus);	    		
	    	}
	        accessibleFuncNoList.add(mapping.getFunc().getFuncNo());
	    }
	    
	    model.addAttribute("accessibleFuncNoList", accessibleFuncNoList);
	    model.addAttribute("funcStatusMap", funcStatusMap);
	    
	    // 게시판 메뉴가 보여야할 지 판단하여 플래그를 뿌려줌
	    boolean boardMenu = accessibleFuncNoList.stream()
	    	    .anyMatch(no -> no == 24 || no == 26);
	    
	    model.addAttribute("boardMenu", boardMenu);
	    
	    
	    // 부가기능 메뉴가 보여야할 지 판단하여 플래그를 뿌려줌
	    boolean additionalMenu = accessibleFuncNoList.stream()
	    	    .anyMatch(no -> no == 29 || no == 31);
	    
	    model.addAttribute("additionalMenu", additionalMenu);
	    
	    // 인사관리자인 경우에 HR메뉴가 보일 수 있게 플래그를 뿌려줌
	    boolean showHrMenu = accessibleFuncNoList.stream()
	    	    .anyMatch(no -> no == 12 || no == 13 || no == 14 || no == 15);
	    
	    model.addAttribute("showHrMenu", showHrMenu);
	    
	    // 설정 메뉴가 보여야할지 판단하여 플래그를 뿌려줌
	    boolean showSettingMenu = accessibleFuncNoList.stream()
	    	    .anyMatch(no -> no == 12 || no == 13 || no == 14 || no == 15 || no == 16 || no == 18 || no == 33 || no == 28 || no == 37);
	    
	    model.addAttribute("showSettingMenu", showSettingMenu);
	}
	
}
