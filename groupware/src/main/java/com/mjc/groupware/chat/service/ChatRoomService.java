package com.mjc.groupware.chat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mjc.groupware.chat.dto.ChatMappingDto;
import com.mjc.groupware.chat.dto.ChatRoomDto;
import com.mjc.groupware.chat.dto.ChatRoomReadDto;
import com.mjc.groupware.chat.entity.ChatMapping;
import com.mjc.groupware.chat.entity.ChatRoom;
import com.mjc.groupware.chat.entity.ChatRoomRead;
import com.mjc.groupware.chat.repository.ChatMappingrepository;
import com.mjc.groupware.chat.repository.ChatMsgRepository;
import com.mjc.groupware.chat.repository.ChatRoomReadRepository;
import com.mjc.groupware.chat.repository.ChatRoomRepository;
import com.mjc.groupware.chat.specification.ChatMappingSpecification;
import com.mjc.groupware.chat.specification.ChatRoomReadSpecification;
import com.mjc.groupware.chat.specification.ChatRoomSpecification;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.entity.MemberAttach;
import com.mjc.groupware.member.repository.MemberAttachRepository;
import com.mjc.groupware.member.security.MemberDetails;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
	
	private final ChatRoomRepository chatRoomRepository;
	private final ChatMappingrepository mappingRepository;
	private final ChatMsgRepository msgRepository;
	private final ChatRoomReadRepository readRepository;
	private final MemberAttachRepository attachRepository;
	
	 @PersistenceContext
	private EntityManager entityManager;
	
	// 채팅 화면 전환
	 public List<ChatRoomDto> selectChatRoomAll() {

		    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		    MemberDetails md = (MemberDetails) authentication.getPrincipal();

		    Specification<ChatRoom> spec = ChatRoomSpecification.participatedBy(md.getMember());
		    Sort sort = Sort.by(Sort.Direction.DESC, "lastMsgDate");

		    List<ChatRoom> list = chatRoomRepository.findAll(spec, sort);
		    List<ChatRoomDto> result = new ArrayList<>();

		    for (ChatRoom room : list) {
		        String title = room.getChatRoomTitle();
		        List<Member> activeMembers = room.getMappings().stream()
		            .filter(m -> "Y".equals(m.getMemberStatus()))
		            .map(ChatMapping::getMemberNo)
		            .collect(Collectors.toList());

		        // 본인 제외
		        List<Member> others = activeMembers.stream()
		            .filter(m -> !m.getMemberNo().equals(md.getMember().getMemberNo()))
		            .collect(Collectors.toList());

		        
		     // ✅ 프로필 이미지 처리
		        String profileImgPath;

		        if (others.size() == 1) {
		            // 🔸 1:1 채팅
		            Member other = others.get(0);
		            MemberAttach attach = attachRepository.findTop1ByMemberOrderByRegDateDesc(other);

		            if (attach != null && attach.getAttachPath() != null && !attach.getAttachPath().isBlank()) {
		                String rawPath = attach.getAttachPath(); // ex: C:/upload/groupware/db3c0cfc1f59...png

		                // ✅ Windows 경로 → 웹 접근 경로로 변환
		                if (rawPath.contains("/upload/groupware/")) {
		                    profileImgPath = rawPath.substring(rawPath.indexOf("/upload/groupware/"));
		                } else {
		                    profileImgPath = rawPath.replace("C:\\upload\\groupware", "/upload/groupware")
		                                            .replace("C:/upload/groupware", "/upload/groupware")
		                                            .replace("\\", "/");
		                }

		            } else {
		                profileImgPath = "/img/one-people-circle.png";
		            }

		        } else {
		            // 🔸 단체방 or 혼자 있는 경우
		            profileImgPath = "/img/people-circle.png"; // ✅ 단체방 기본 이미지
		        }

		        // 제목 설정
		        if (title.trim().isEmpty()) {
		            List<String> nameList = others.stream()
		                .map(m -> m.getMemberName() + " " + (m.getPos() != null ? m.getPos().getPosName() : ""))
		                .collect(Collectors.toList());
		            title = nameList.isEmpty() ? "(알 수 없음)" : String.join(", ", nameList);
		        }

		        ChatRoomDto dto = ChatRoomDto.builder()
		            .chat_room_no(room.getChatRoomNo())
		            .chat_room_title(title)
		            .last_msg(room.getLastMsg())
		            .last_msg_date(room.getLastMsgDate())
		            .profile_img_path(profileImgPath) // ✅ 넣기
		            .build();

		        result.add(dto);
		    }


		    return result;
		}

	
	// 채팅방 생성
	@Transactional
	public ChatRoom createChatRoom(ChatRoomDto dto) {
	    ChatRoom chatroom = ChatRoom.builder()
	        .chatRoomTitle(dto.getChat_room_title())
	        .createMemberNo(Member.builder().memberNo(dto.getCreate_member_no()).build())
	        .build();

	    ChatRoom saved = chatRoomRepository.save(chatroom);

	    for (Long memberNo : dto.getMember_no()) {
	        ChatMapping mappings = ChatMapping.builder()
	            .chatRoomNo(saved)
	            .memberNo(Member.builder().memberNo(memberNo).build())
	            .build();
	        mappingRepository.save(mappings);
	    }

	    // 💥 여기 추가
	    entityManager.flush();
	    entityManager.clear();

	    return saved;
	}

	// 1:1 채팅 중복 검사 
	public boolean isDuplicateOneToOneRoom(List<Long> memberNos) {
	    if (memberNos.size() != 2) return false;

	    List<ChatRoom> allRooms = chatRoomRepository.findAll(); // ✅ 모든 채팅방 가져옴

	    for (ChatRoom room : allRooms) {
	        List<ChatMapping> mappings = room.getMappings();
	        
	        // 멤버가 2명이고 둘 다 Y 상태인지 확인
	        if (mappings.size() == 2 &&
	            mappings.stream().allMatch(m -> "Y".equals(m.getMemberStatus()))) {
	            
	            List<Long> mappedNos = mappings.stream()
	                .map(m -> m.getMemberNo().getMemberNo())
	                .sorted()
	                .toList();

	            List<Long> targetNos = memberNos.stream().sorted().toList();

	            if (mappedNos.equals(targetNos)) {
	                return true;
	            }
	        }
	    }

	    return false;
	}
	
	// 채팅방 상세 조회
	@Transactional(readOnly = true)
	public ChatRoom selectChatRoomOne(Long chatRoomNo) {
	    ChatRoom room = chatRoomRepository.findById(chatRoomNo).orElse(null);

	    if (room != null) {
	        // ✅ 여기서 Lazy 강제 초기화 (세션 살아있으니 터지지 않음)
	        room.getMappings().size();

	        List<ChatMapping> originList = room.getMappings();
	        List<ChatMapping> filteredList = new ArrayList<>();

	        for (ChatMapping mapping : originList) {
	            if ("Y".equals(mapping.getMemberStatus())) {
	                filteredList.add(mapping);
	                // ✅ 추가로 Member도 로딩 (알림 이름 때문에)
	                mapping.getMemberNo().getMemberName(); // lazy 강제 초기화
	            }
	        }

	        room.setMappings(filteredList); // ✅ 필터링된 리스트로 덮어쓰기
	    }

	    return room;
	}

	
	// 읽음 시간 조회 
	public int selectUnreadMsgCount(ChatRoomReadDto dto) {
	    // 1. 로그인한 사용자 가져오기
	    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	    MemberDetails md = (MemberDetails) authentication.getPrincipal();
	    Member loginUser = md.getMember();

	    // 2. 읽음 시간 조회
	    dto.setMember_no(loginUser.getMemberNo());
	    Specification<ChatRoomRead> spec = ChatRoomReadSpecification.ChatRoomNoEqualsAndMemberNoEquals(dto);
	    List<ChatRoomRead> readList = readRepository.findAll(spec);

	    LocalDateTime lastReadTime = readList.isEmpty()
	        ? LocalDateTime.of(1970, 1, 1, 0, 0)
	        : readList.get(0).getLastReadTime();

	    // 3. 채팅방 조회
	    ChatRoom chatRoom = chatRoomRepository.findById(dto.getChat_room_no())
	        .orElseThrow(() -> new RuntimeException("채팅방 없음"));

	    // 4. 내가 보낸 메시지 제외하고, 안읽은 메시지 개수 반환
	    return msgRepository.countByChatRoomNoAndSendDateAfterAndMemberNoNot(
	        chatRoom, lastReadTime, loginUser
	    );
	}


	// 채팅방 읽음 처리 
	public ChatRoomReadDto updateReadTime(ChatRoomReadDto dto) {
	    Specification<ChatRoomRead> spec = ChatRoomReadSpecification.ChatRoomNoEqualsAndMemberNoEquals(dto);
	    List<ChatRoomRead> list = readRepository.findAll(spec);

	    ChatRoomRead record;

	    if (list.isEmpty()) {
	        record = dto.toEntity(); // ✅ 딱 이 한 줄로 변환
	    } else {
	        record = list.get(0);
	        record.setLastReadTime(LocalDateTime.now()); 
	    }

	    ChatRoomRead saved = readRepository.save(record);
	    return ChatRoomReadDto.toDto(saved); 
	}


	// 채팅방 나가기 
	public int updateStatus(ChatMappingDto dto) {
	    int result = 0;
	    
	    try {
	        Specification<ChatMapping> spec = ChatMappingSpecification.ChatRoomNoEqualsAndMemberNoEquals(dto);
	        List<ChatMapping> list = mappingRepository.findAll(spec);

	       
	        for (ChatMapping mapping : list) {
	            mapping.setMemberStatus("N");
	        }

	        mappingRepository.saveAll(list);
	        result = list.size();

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return result;
	}
	
	//채팅방 이름 조회 
	public String getChatRoomDisplayTitle(ChatRoom chatRoom, Long currentMemberNo) {
	    if (chatRoom.getChatRoomTitle() != null && !chatRoom.getChatRoomTitle().trim().isEmpty()) {
	        return chatRoom.getChatRoomTitle().trim();
	    }

	    StringBuilder sb = new StringBuilder();
	    for (ChatMapping m : chatRoom.getMappings()) {
	        if ("Y".equals(m.getMemberStatus())) {
	            Long memberNo = m.getMemberNo().getMemberNo();
	            if (memberNo.equals(currentMemberNo)) continue; // 🔥 본인 제외

	            sb.append(m.getMemberNo().getMemberName())
	              .append(" ")
	              .append(m.getMemberNo().getPos().getPosName())
	              .append(", ");
	        }
	    }

	    if (sb.length() > 0) sb.setLength(sb.length() - 2);
	    return sb.length() > 0 ? sb.toString() : "이름 없는 채팅방";
	}
	


}
