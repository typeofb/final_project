package com.mjc.groupware.home.comtroller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.mjc.groupware.attendance.dto.AttendanceDto;
import com.mjc.groupware.attendance.entity.Attendance;
import com.mjc.groupware.attendance.entity.WorkSchedulePolicy;
import com.mjc.groupware.attendance.repository.AttendanceRepository;
import com.mjc.groupware.attendance.repository.WorkSchedulePolicyRepository;
import com.mjc.groupware.attendance.service.AttendanceService;
import com.mjc.groupware.board.entity.Board;
import com.mjc.groupware.board.service.BoardService;
import com.mjc.groupware.chat.dto.ChatRoomDto;
import com.mjc.groupware.chat.service.ChatRoomService;
import com.mjc.groupware.member.dto.MemberDto;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.service.MemberService;
import com.mjc.groupware.notice.entity.Notice;
import com.mjc.groupware.notice.service.NoticeService;
import com.mjc.groupware.plan.entity.Plan;
import com.mjc.groupware.plan.service.PlanService;

import com.mjc.groupware.company.dto.CompanyDto;
import com.mjc.groupware.company.service.CompanyService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {
	
	private final MemberService memberService;
	private final AttendanceService attendanceService;
	private final AttendanceRepository attendanceRepository;
	private final PlanService planService;
	private final WorkSchedulePolicyRepository workSchedulePolicyRepository;
	private final NoticeService noticeService;
    private final ChatRoomService chatRoomService;
    private final CompanyService companyService;
	private final BoardService boardService; // 게시글 추가
	
	@GetMapping({"", "/", "/home"})
	public String homeView(Model model, @AuthenticationPrincipal UserDetails userDetails) {
		
		String userId = userDetails.getUsername();
	    MemberDto memberDto = new MemberDto();
	    memberDto.setMember_id(userId);
	    Member member = memberService.selectMemberOne(memberDto);

	    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

	    Attendance attendance = attendanceRepository.findByMember_MemberNoAndAttendDate(member.getMemberNo(), today);
	    if (attendance != null) {
	        AttendanceDto dto = new AttendanceDto().toDto(attendance);
	        model.addAttribute("todayAttendance", dto);
	    }
	    model.addAttribute("member", member);
	    
	    CompanyDto company = companyService.selectLatestCompanyProfile();
	    model.addAttribute("company", company);
	    
	    // 오늘 날짜의 휴가가 있는지
	    Plan plan = planService.selectAnnualPlan(member, today);
	    model.addAttribute("plan", plan);
	    
	    WorkSchedulePolicy wsp = workSchedulePolicyRepository.findById(1L).orElse(null);
	    model.addAttribute("workPolicy", wsp);
	    
	
	    List<Board> recentAllBoards = boardService.selectRecentAllBoardsWithFixed(3); // 총 3개
	    model.addAttribute("recentBoards", recentAllBoards);


		// ✅ 공지사항 최신 3건 추가
		List<Notice> latestNoticeList = noticeService.getLatestNotices(3);
		model.addAttribute("latestNotices", latestNoticeList);
		

	    
	    /////// 채팅 //////
	    List<ChatRoomDto> resultList = chatRoomService.selectChatRoomAll();
		model.addAttribute("chatRoomList",resultList);

	    
		return "home";
	}
	
	@GetMapping("/starter")
	public String starterView() {
		return "starter";
	}
	
	@GetMapping("/sample")
	public String sampleView() {
		return "sample";
	}
}
