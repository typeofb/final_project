package com.mjc.groupware.approval.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.mjc.groupware.approval.dto.ApprovalDto;
import com.mjc.groupware.approval.dto.ApprovalFormDto;
import com.mjc.groupware.approval.dto.ApprovalStatusTypeDto;
import com.mjc.groupware.approval.dto.AutoApprovalLineResponseDto;
import com.mjc.groupware.approval.dto.PageDto;
import com.mjc.groupware.approval.dto.SearchDto;
import com.mjc.groupware.approval.entity.ApprAgreementer;
import com.mjc.groupware.approval.entity.ApprApprover;
import com.mjc.groupware.approval.entity.ApprReferencer;
import com.mjc.groupware.approval.entity.Approval;
import com.mjc.groupware.approval.entity.ApprovalAttach;
import com.mjc.groupware.approval.entity.ApprovalForm;
import com.mjc.groupware.approval.mybatis.vo.ApprovalStatusVo;
import com.mjc.groupware.approval.mybatis.vo.ApprovalVo;
import com.mjc.groupware.approval.service.ApprovalAttachService;
import com.mjc.groupware.approval.service.ApprovalService;
import com.mjc.groupware.member.dto.MemberDto;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.service.MemberService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class ApprovalController {

	private Logger logger = LoggerFactory.getLogger(ApprovalController.class);
	
	private final ApprovalService service;
	private final MemberService memberService;
	private final ApprovalAttachService approvalAttachService;

	// 관리자 : 관리자만 접근 가능한 url
	
	@GetMapping("/admin/approval") 
	public String approvalAdminView(Model model) {
		
		List<ApprovalForm> resultList = service.selectApprovalFormAll();
		model.addAttribute("formList", resultList);
		return "approval/admin/approvalManagement";
	}
	
	// 관리자 : 전사 전체 결재 관리 페이지로 이동
	@GetMapping("/admin/approval/list")
	public String approvalAdminListView(Model model, SearchDto searchDto, PageDto pageDto,
			@AuthenticationPrincipal UserDetails userDetails) {
		
		String userId = userDetails.getUsername();
		MemberDto memberDto = new MemberDto();
		memberDto.setMember_id(userId);
		Member entity = memberService.selectMemberOne(memberDto);
		MemberDto member = new MemberDto().toDto(entity);
		
		if(pageDto.getNowPage() == 0) pageDto.setNowPage(1);
		
		Page<Approval> approvalList = service.selectApprovalAdminAll(searchDto, pageDto);
		ApprovalStatusTypeDto astd = service.selectApprovalAdminStatusType();
		
		pageDto.setTotalPage(approvalList.getTotalPages());
		pageDto.setTotalCount((int)approvalList.getTotalElements());
		
		model.addAttribute("member", member);
		model.addAttribute("approvalList", approvalList);
		model.addAttribute("pageDto", pageDto);
		model.addAttribute("searchDto", searchDto);
		model.addAttribute("approvalStatusTypeDto", astd);
		
		return "approval/admin/adminApprovalList";
	}
	
	// 결재 양식 생성 페이지로 이동
	@GetMapping("/admin/approvalForm/create")
	public String createApprovalAdminView() {
		return "approval/admin/createApprovalForm";
	}
	
	// 결재 양식 생성
	@PostMapping("/admin/approvalForm/create")
	@ResponseBody
	public Map<String,String> createApprovalApi(ApprovalFormDto dto) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 양식 생성 실패");

		logger.info("진입 test");
		
		int result = service.createApprovalApi(dto);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재 양식 생성 성공");
		}
		
		return resultMap;
	}
	
	// 결재 양식 선택
	@PostMapping("/admin/approvalForm/view/{id}")
	@ResponseBody
	public ApprovalFormDto approvalFormView(@PathVariable("id") Long id) {
	    ApprovalFormDto dto = service.selectApprovalFormById(id);
	    return dto;
	}
	
	// 결재 양식 수정 페이지로 이동
	@GetMapping("/admin/approvalForm/update/{id}")
	public String updateApprovalFormView(@PathVariable("id") Long id, Model model) {
	    ApprovalFormDto dto = service.selectApprovalFormById(id);
	    model.addAttribute("form", dto);
	    return "approval/admin/updateApprovalForm";
	}
	
	
	// 결재 양식 수정
	@PostMapping("/admin/approvalForm/update")
	@ResponseBody
	public Map<String,String> updateApprovalForm(ApprovalFormDto dto) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 양식 수정 실패");

		int result = service.createApprovalApi(dto);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재 양식 수정 성공");
		}
		
		return resultMap;
		
	}
	
	// 결재 양식 상태 변경
	@PostMapping("/admin/approvalForm/statusUpdate/{id}")
	@ResponseBody
	public Map<String,String> updateApprovalFormStatus(@PathVariable("id") Long id) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 양식 상태변경 실패");

		int result = service.updateApprovalFormStatus(id);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재 양식 상태변경 성공");
		}
		
		return resultMap;
	}
	
	
	
	// 사용자 : 인증받은 모든 사원이 접근 가능한 url
	
	@GetMapping("/approval")
	public String approvalView(Model model, SearchDto searchDto, PageDto pageDto,
							@AuthenticationPrincipal UserDetails userDetails) {
		
		String userId = userDetails.getUsername();
	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
	    
	    if(pageDto.getNowPage() == 0) pageDto.setNowPage(1);
	    Page<Approval> approvalList = service.selectApprovalAll(member, searchDto, pageDto);
	    List<Approval> list = service.selectApprovalAllById(member);
	    
	    ApprovalStatusTypeDto astd = new ApprovalStatusTypeDto();
	    for(Approval approval : list) {
	    	if("A".equals(approval.getApprStatus())) {
	    		astd.setCount_A(astd.getCount_A()+1);
	    	} else if("D".equals(approval.getApprStatus())) {
	    		astd.setCount_D(astd.getCount_D()+1);
	    	} else if("R".equals(approval.getApprStatus())) {
	    		astd.setCount_R(astd.getCount_R()+1);
	    	} else if("C".equals(approval.getApprStatus())) {
	    		astd.setCount_C(astd.getCount_C()+1);
	    	}
	    }
	    
	    pageDto.setTotalPage(approvalList.getTotalPages());
	    pageDto.setTotalCount((int)approvalList.getTotalElements());
	    
	    model.addAttribute("member", member);
	    model.addAttribute("approvalList", approvalList);
	    model.addAttribute("pageDto", pageDto);
	    model.addAttribute("searchDto", searchDto);
	    model.addAttribute("approvalStatusTypeDto", astd);
		
		return "approval/user/sendApproval";
	}
	
	@GetMapping("/approval/send/detail/{id}")
	public String approvalSendDetailView(@PathVariable("id") Long id, Model model) {
		Approval approval = service.selectApprovalOneByApprovalNo(id);
	    List<ApprApprover> approverList = service.selectApprApproverAllByApprovalNo(id);
	    List<ApprAgreementer> agreementerList = service.selectApprAgreementerAllByApprovalNo(id);
	    List<ApprReferencer> referencerList = service.selectApprReferencerAllByApprovalNo(id);
	    
	    int return_result = service.selectReturnApprovalByApprovalNo(id);
	    
	    List<ApprovalAttach> attachList= approvalAttachService.findByApproval(approval);
	    
	    model.addAttribute("approval", approval);
	    model.addAttribute("approverList", approverList);
	    model.addAttribute("agreementerList", agreementerList);
	    model.addAttribute("referencerList", referencerList);
	    model.addAttribute("attachList", attachList);
	    model.addAttribute("return_result", return_result);
		
		return "approval/user/sendApprovalDetail";
	}
	
	@GetMapping("/approval/receive")
	public String receiveApprovalView(Model model, SearchDto searchDto, PageDto pageDto, @AuthenticationPrincipal UserDetails userDetails) {
		
		String userId = userDetails.getUsername();
	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
	    
	    if(pageDto.getNowPage() == 0) pageDto.setNowPage(1);
	    
	    List<ApprovalVo> fullList = service.selectApprovalAllByApproverId(member, searchDto, pageDto);
	    ApprovalStatusVo statusCnt = service.selectApprovalStatusByApproverId(member);
	    
	    int start = (pageDto.getNowPage() - 1) * pageDto.getNumPerPage();
		int end = Math.min(start + pageDto.getNumPerPage(), fullList.size());
		
		List<ApprovalVo> pageContent = fullList.subList(start, end); // 현재 페이지의 데이터만 추출
		Page<ApprovalVo> approvalVoList = new PageImpl<>(pageContent, PageRequest.of(pageDto.getNowPage() - 1, pageDto.getNumPerPage()), fullList.size());
	    
		// pageDto에 총 페이지 수 설정
		int totalPage = (int) Math.ceil((double) fullList.size() / pageDto.getNumPerPage());
		pageDto.setTotalPage(totalPage);
		
	    model.addAttribute("member", member);
	    model.addAttribute("approvalVoList", approvalVoList);
	    model.addAttribute("pageDto", pageDto);
	    model.addAttribute("searchDto", searchDto);
	    model.addAttribute("statusCnt", statusCnt);
	    
		return "approval/user/receiveApproval";
	}
	
	@GetMapping("/approval/receive/detail/{id}")
	public String receiveApprovalDetailView(@PathVariable("id") Long id, Model model, @AuthenticationPrincipal UserDetails userDetails) {
		
	    Approval approval = service.selectApprovalOneByApprovalNo(id);
	    List<ApprApprover> approverList = service.selectApprApproverAllByApprovalNo(id);
	    List<ApprAgreementer> agreementerList = service.selectApprAgreementerAllByApprovalNo(id);
	    List<ApprReferencer> referencerList = service.selectApprReferencerAllByApprovalNo(id);
	    
	    List<ApprovalAttach> attachList= approvalAttachService.findByApproval(approval);
	    
	    String userId = userDetails.getUsername();
	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member member = memberService.selectMemberOne(memberDto);
	    model.addAttribute("member", member);
	    
	    model.addAttribute("approval", approval);
	    model.addAttribute("attachList", attachList);
	    model.addAttribute("approverList", approverList);
	    model.addAttribute("agreementerList", agreementerList);
	    model.addAttribute("referencerList", referencerList);
		
		return "approval/user/receiveApprovalDetail";
	}
	
	@GetMapping("/approval/create")
	public String createApprovalView(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		
		String userId = userDetails.getUsername();

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member member = memberService.selectMemberOne(memberDto);
		
		List<ApprovalForm> resultList = service.selectApprovalFormAll();
		List<Member> memberList = memberService.selectMemberAll();
		model.addAttribute("formList", resultList);
		model.addAttribute("member", member);
		model.addAttribute("memberList", memberList);
		return "approval/user/createApproval";
	}
	
	// 결재 양식 선택
	@PostMapping("/approvalForm/view/{id}")
	@ResponseBody
	public ApprovalFormDto UserApprovalFormView(@PathVariable("id") Long id) {
	    ApprovalFormDto dto = service.selectApprovalFormById(id);
	    return dto;
	}
	
	@PostMapping("/approval/create/{id}")
	@ResponseBody
	public Map<String,Object> createApprovalDiv(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
		String userId = userDetails.getUsername();

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
	    
	    ApprovalFormDto dto = service.selectApprovalFormById(id);
	    
	    Map<String, Object> result = new HashMap<>();
	    result.put("approvalForm", dto);
	    result.put("member", member);
	    

	    return result;
	}
	
	// Dept dept = deptService.selectDeptAll();
	
	// 사원 결재 요청(만들기)
	@PostMapping("/approval/create")
	@ResponseBody
	public Map<String,String> createApprovalApi(ApprovalDto approvalDto,
												@RequestParam("files") List<MultipartFile> files,
												@AuthenticationPrincipal UserDetails userDetails) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 요청에 실패하였습니다.");
		
		String userId = userDetails.getUsername();
		MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
		
		if (approvalDto.getApprover_no() != null && !approvalDto.getApprover_no().isEmpty()) {
			System.out.println("결재자 : " + approvalDto.getApprover_no().get(0));
		}
		
		int result = service.createApprovalApi(approvalDto, files, entity);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재가 요청되었습니다.");
		}
		
		return resultMap;
	}
	
	// 결재자 - 승인버튼
	@PostMapping("/approval/success/{id}")
	@ResponseBody
	public Map<String,String> approvalSuccessApi(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 승인에 실패하였습니다.");
		
		String userId = userDetails.getUsername();

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
		
		int result = service.approvalSuccessApi(id, member);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재가 승인되었습니다.");
		}
		
		return resultMap;
	}
	
	// 결재자 - 반려버튼
	@PostMapping("/approval/companion/{id}")
	@ResponseBody
	public Map<String,String> approvalFailApi(@PathVariable("id") Long id, @RequestParam("decision_reason") String reason, @AuthenticationPrincipal UserDetails userDetails) {
		Map<String,String> resultMap = new HashMap<String,String>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 반려에 실패하였습니다.");
		
		String userId = userDetails.getUsername();

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
		
		int result = service.approvalFailApi(id, reason, member);
		
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재가 반려되었습니다.");
		}
		
		return resultMap;
	}
	
	// 합의자 - 수락버튼
	@PostMapping("/approval/agree/{id}")
	@ResponseBody
	public Map<String,String> approvalAgreeApi(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
		Map<String,String> resultMap = new HashMap<String,String>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 수락에 실패하였습니다.");
		System.out.println("test");
		String userId = userDetails.getUsername();
		

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
	    
	    System.out.println(member);
	    
	    int result = service.approvalAgreeApi(id, member);
	    
	    System.out.println(result);
	    
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재가 수락되었습니다.");
		}
	    
	    return resultMap;
	}
	
	
	// 합의자 - 거절버튼
	@PostMapping("/approval/reject/{id}")
	@ResponseBody
	public Map<String,String> approvalRejectApi(@PathVariable("id") Long id, @RequestParam("agree_reason") String reason, @AuthenticationPrincipal UserDetails userDetails) {
		Map<String,String> resultMap = new HashMap<String,String>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 거절에 실패하였습니다.");
		
		String userId = userDetails.getUsername();
		

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member entity = memberService.selectMemberOne(memberDto);
	    MemberDto member = new MemberDto().toDto(entity);
	    
	    int result = service.approvalRejectApi(id, reason, member);
	    
	    
	    if(result == 2) {
	    	resultMap.put("res_code", "501");
	    	resultMap.put("res_msg", "다른 합의자가 이미 거절하였습니다.");
	    } else if(result > 0 && result < 2) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재가 거절되었습니다.");
		}
	    
	    return resultMap;
		
	}
	
	
	@PostMapping("/approval/return/{id}")
	@ResponseBody
	public Map<String,String> approvalReturnApi(@PathVariable("id") Long id, @RequestParam("return_reason") String reason) {
		Map<String,String> resultMap = new HashMap<String,String>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 회수요청에 실패하였습니다.");
		
		System.out.println("회수 사유:"+reason);
		
	    int result = service.approvalReturnApi(id, reason);
	    
	    System.out.println(result);
	    
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재가 회수요청되었습니다.");
		}
	    
	    return resultMap;
	}
	
	@GetMapping("/approval/retry/{id}")
	public String retryApprovalViewApi(@PathVariable("id") Long id, Model model,  @AuthenticationPrincipal UserDetails userDetails) {

	    // 현재 로그인한 사용자 정보 불러오기
		String userId = userDetails.getUsername();
		

	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member member = memberService.selectMemberOne(memberDto);
//	    MemberDto member = new MemberDto().toDto(entity);

	    // 기존 결재 데이터 가져오기
	    Approval approval = service.selectApprovalOneByApprovalNo(id);
	    List<ApprApprover> approverList = service.selectApprApproverAllByApprovalNo(id);
	    List<ApprAgreementer> agreementerList = service.selectApprAgreementerAllByApprovalNo(id);
	    List<ApprReferencer> referencerList = service.selectApprReferencerAllByApprovalNo(id);
	    List<ApprovalAttach> attachList= approvalAttachService.findByApproval(approval);
	    
	    
	    model.addAttribute("approval", approval);
	    model.addAttribute("attachList", attachList);
	    model.addAttribute("approverList", approverList);
	    model.addAttribute("agreementerList", agreementerList);
	    model.addAttribute("referencerList", referencerList);
	    model.addAttribute("member", member);

	    return "approval/user/retryApproval";
	}
	
	@PostMapping("/approval/retry")
	@ResponseBody
	public Map<String,String> retryApprovalApi(ApprovalDto approvalDto,
			@RequestParam(value = "files", required = false) List<MultipartFile> files,
			   @RequestParam(value = "deleteFiles", required = false) List<Long> deleteFiles) {
		Map<String,String> resultMap = new HashMap<String,String>();
		
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "결재 재요청에 실패하였습니다.");
		
	    int result = service.retryApprovalApi(approvalDto, files, deleteFiles);
	    
		if(result > 0) {
			resultMap.put("res_code", "200");
			resultMap.put("res_msg", "결재 재요청에 성공하였습니다.");
		}
	    
	    return resultMap;
	}
	
	// 회람 확인 API
	@PostMapping("/approval/referencer/confirm/{id}")
	@ResponseBody
	public Map<String, String> confirmReferencerApi(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
		Map<String, String> resultMap = new HashMap<>();
		resultMap.put("res_code", "500");
		resultMap.put("res_msg", "회람 확인 처리 실패");

		if (userDetails != null) {
			String userId = userDetails.getUsername();
			MemberDto memberDto = new MemberDto();
			memberDto.setMember_id(userId);
			Member entity = memberService.selectMemberOne(memberDto);

			if (entity != null) {
				int result = service.confirmReferencer(id, entity.getMemberNo());
				if (result > 0) {
					resultMap.put("res_code", "200");
					resultMap.put("res_msg", "회람이 확인되었습니다.");
				}
			}
		}

		return resultMap;
	}
	
	// 조직 및 DB 양식/부서 맞춤 결재 라인 자동 가져오기
	@GetMapping("/approval/auto-line")
	@ResponseBody
	public List<AutoApprovalLineResponseDto> getAutoApprovalLine(
			@RequestParam(value = "targetMemberNo", required = false) Long targetMemberNo,
			@RequestParam(value = "formNo", required = false) Long formNo,
			@AuthenticationPrincipal UserDetails userDetails) {
		Long memberNo = targetMemberNo;
		if (memberNo == null && userDetails != null) {
			String userId = userDetails.getUsername();
			MemberDto memberDto = new MemberDto();
			memberDto.setMember_id(userId);
			Member member = memberService.selectMemberOne(memberDto);
			if (member != null) {
				memberNo = member.getMemberNo();
			}
		}
		
		if (memberNo == null) {
			return new ArrayList<>();
		}
		
		return service.selectAutoApprovalLineResponse(memberNo, formNo);
	}
	
}
