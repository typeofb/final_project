package com.mjc.groupware.approval.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mjc.groupware.approval.dto.AutoApprovalLineResponseDto;
import com.mjc.groupware.approval.entity.ApprovalLine;
import com.mjc.groupware.approval.entity.ApprovalLineDetail;
import com.mjc.groupware.approval.repository.ApprovalLineDetailRepository;
import com.mjc.groupware.approval.repository.ApprovalLineRepository;
import com.mjc.groupware.approval.dto.ApprAgreementerDto;
import com.mjc.groupware.approval.dto.ApprApproverDto;
import com.mjc.groupware.approval.dto.ApprReferencerDto;
import com.mjc.groupware.approval.dto.ApprovalDto;
import com.mjc.groupware.approval.dto.ApprovalFormDto;
import com.mjc.groupware.approval.dto.ApprovalStatusTypeDto;
import com.mjc.groupware.approval.dto.PageDto;
import com.mjc.groupware.approval.dto.SearchDto;
import com.mjc.groupware.approval.entity.ApprAgreementer;
import com.mjc.groupware.approval.entity.ApprApprover;
import com.mjc.groupware.approval.entity.ApprReferencer;
import com.mjc.groupware.approval.entity.Approval;
import com.mjc.groupware.approval.entity.ApprovalAttach;
import com.mjc.groupware.approval.entity.ApprovalForm;
import com.mjc.groupware.approval.mybatis.mapper.ApprovalMapper;
import com.mjc.groupware.approval.mybatis.vo.ApprovalStatusVo;
import com.mjc.groupware.approval.mybatis.vo.ApprovalVo;
import com.mjc.groupware.approval.repository.ApprAgreementerRepository;
import com.mjc.groupware.approval.repository.ApprApproverRepository;
import com.mjc.groupware.approval.repository.ApprReferencerRepository;
import com.mjc.groupware.approval.repository.ApprovalFormRepository;
import com.mjc.groupware.approval.repository.ApprovalRepository;
import com.mjc.groupware.approval.specification.ApprovalSpecification;
import com.mjc.groupware.common.websocket.entity.Alarm;
import com.mjc.groupware.common.websocket.repository.AlarmMappingRepository;
import com.mjc.groupware.common.websocket.repository.AlarmRepository;
import com.mjc.groupware.member.dto.MemberDto;
import com.mjc.groupware.member.entity.Member;
import com.mjc.groupware.member.repository.MemberRepository;
import com.mjc.groupware.plan.repository.PlanRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ApprovalService {

	private final ApprovalFormRepository approvalFormRepository;
	private final ApprovalRepository approvalRepository;
	private final ApprApproverRepository apprApproverRepository;
	private final ApprAgreementerRepository apprAgreementerRepository;
	private final ApprReferencerRepository apprReferencerRepository;
	private final PlanRepository planRepository;
	private final MemberRepository memberRepository;
	private final ApprovalMapper approvalMapper;
	private final ApprovalAttachService approvalAttachService;
	private final ApprovalLineRepository approvalLineRepository;
	private final ApprovalLineDetailRepository approvalLineDetailRepository;
	
	// 알람
	private final ApprovalAlarmService approvalAlarmService;
	private final AlarmRepository alarmRepository;
	private final AlarmMappingRepository alarmMappingRepository;

	public int createApprovalApi(ApprovalFormDto dto) {
		int result = 0;
		try {
			ApprovalForm entity = dto.toEntity();
			ApprovalForm saved = approvalFormRepository.save(entity);
			if(saved != null) {
				result = 1;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public List<ApprovalForm> selectApprovalFormAll() {
		
		List<ApprovalForm> resultList = approvalFormRepository.findAll();
		
		return resultList;
	}

	public ApprovalFormDto selectApprovalFormById(Long id) {
		
		ApprovalForm entity = approvalFormRepository.findById(id).orElse(null);
		ApprovalFormDto dto = new ApprovalFormDto().toDto(entity);
		
		return dto;
	}

	public int updateApprovalFormStatus(Long id) {
		int result = 0;
		try {
			
			ApprovalForm entity = approvalFormRepository.findById(id).orElse(null);
			ApprovalFormDto dto = new ApprovalFormDto().toDto(entity);
			
			if("Y".equals(dto.getApproval_form_status())) {
				dto.setApproval_form_status("N");
			} else {
				dto.setApproval_form_status("Y");
			}
			
			ApprovalForm param = dto.toEntity();
			approvalFormRepository.save(param);
			
			result = 1;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	// 결재 승인 요청
	@Transactional(rollbackFor = Exception.class)
	public int createApprovalApi(ApprovalDto approvalDto , List<MultipartFile> files, Member member) {
		int result = 0;
		
		try {
			// 대리 기안 처리
			if ("Y".equals(approvalDto.getIs_proxy()) && approvalDto.getTarget_member_no() != null) {
				approvalDto.setAppr_sender(approvalDto.getTarget_member_no());
				approvalDto.setProxy_drafter(member.getMemberNo());
			} else {
				approvalDto.setAppr_sender(member.getMemberNo());
				approvalDto.setProxy_drafter(null);
				approvalDto.setIs_proxy("N");
			}
			
			boolean hasApprovers = approvalDto.getApprover_no() != null && !approvalDto.getApprover_no().isEmpty();
			boolean hasAgreementers = approvalDto.getAgreementer_no() != null && !approvalDto.getAgreementer_no().isEmpty();
			boolean hasReferencers = approvalDto.getReferencer_no() != null && !approvalDto.getReferencer_no().isEmpty();

			// 결재자 및 합의자가 모두 지정되지 않은 경우, DB 지정 기본 결재선 또는 조직 자동 결재라인 세팅
			if (!hasApprovers && !hasAgreementers) {
				List<AutoApprovalLineResponseDto> autoLine = selectAutoApprovalLineResponse(approvalDto.getAppr_sender(), approvalDto.getApproval_type_no());
				if (autoLine != null && !autoLine.isEmpty()) {
					List<Long> autoApproverNos = new ArrayList<>();
					List<Long> autoAgreementerNos = new ArrayList<>();
					List<Long> autoReferencerNos = new ArrayList<>();

					for (AutoApprovalLineResponseDto autoDto : autoLine) {
						if ("AGREEMENTER".equalsIgnoreCase(autoDto.getAppr_type())) {
							autoAgreementerNos.add(autoDto.getMember_no());
						} else if ("REFERENCER".equalsIgnoreCase(autoDto.getAppr_type())) {
							autoReferencerNos.add(autoDto.getMember_no());
						} else {
							autoApproverNos.add(autoDto.getMember_no());
						}
					}

					if (!autoApproverNos.isEmpty()) {
						approvalDto.setApprover_no(autoApproverNos);
						hasApprovers = true;
					}
					if (!autoAgreementerNos.isEmpty()) {
						approvalDto.setAgreementer_no(autoAgreementerNos);
						hasAgreementers = true;
					}
					if (!autoReferencerNos.isEmpty()) {
						approvalDto.setReferencer_no(autoReferencerNos);
						hasReferencers = true;
					}
				}
			}

			if (!hasApprovers && !hasAgreementers && !hasReferencers) {
				throw new IllegalArgumentException("결재자 또는 참조/회람자는 최소 한 명 이상 등록되어야 합니다.");
			}

			if (hasAgreementers) { 
				approvalDto.setAppr_status("A");
			} else if (hasApprovers) {
				approvalDto.setAppr_status("D");
				approvalDto.setAppr_order_status(1);
			} else {
				// 결재자/합의자가 없고 참조/회람자만 등록된 경우 즉시 승인/완료('C') 처리
				approvalDto.setAppr_status("C");
				approvalDto.setAppr_order_status(1);
			}
			
			if (approvalDto.getApproval_type_no() != 1) {
				approvalDto.setUse_annual_leave(0);
			}
			
			Approval saved = approvalRepository.save(approvalDto.toEntity());
			
			Long apprNo = saved.getApprNo();
			
			ApprApproverDto approverDto = new ApprApproverDto();
			ApprAgreementerDto agreementerDto = new ApprAgreementerDto();
			ApprReferencerDto referencerDto = new ApprReferencerDto();
			
			// 결재자
			if (hasApprovers) {
				approverDto.setAppr_no(apprNo);
				approverDto.setApprovers(approvalDto.getApprover_no());
				List<ApprApprover> approverList = approverDto.toEntityList();
				for (ApprApprover entity : approverList) {
					try {
						apprApproverRepository.save(entity);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			// 합의자
			agreementerDto.setAppr_no(apprNo);
			if(approvalDto.getAgreementer_no() != null) {
				agreementerDto.setAgreementers(approvalDto.getAgreementer_no());
				List<ApprAgreementer> agreementerList = agreementerDto.toEntityList();
				for(ApprAgreementer entity : agreementerList) {
					try {
						apprAgreementerRepository.save(entity);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			
			// 참조자
			referencerDto.setAppr_no(apprNo);
			if(approvalDto.getReferencer_no() != null) {
				referencerDto.setReferencers(approvalDto.getReferencer_no());
				List<ApprReferencer> referencerList = referencerDto.toEntityList();
				for(ApprReferencer entity : referencerList) {
					try {
						apprReferencerRepository.save(entity);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			 if (files != null && !files.isEmpty()) {
		            for (MultipartFile file : files) {
		                if (!file.isEmpty()) {
		                    try {
		                    	approvalAttachService.saveFile(file, saved);
		                    } catch (IOException e) {
		                        e.printStackTrace();
		                        return 0;
		                    }
		                }
		            }
		        }
			 
			 // 결재 알림 보내기
			 Set<Long> targetMemberNoSet = new HashSet<>();
			 
			// 1합의자
			if (hasAgreementers) {
			    targetMemberNoSet.addAll(approvalDto.getAgreementer_no());
			}

			// 2참조자
			if (hasReferencers) {
			    targetMemberNoSet.addAll(approvalDto.getReferencer_no());
			}

			// 결재자 (합의자 없을 때만)
			if (!hasAgreementers && hasApprovers) {
				List<ApprApprover> savedApproverList = apprApproverRepository.findAllByApproval_ApprNo(apprNo);
				processAutoSkipAbsentApprovers(saved, savedApproverList);
			} else {
				List<Long> targetMemberNos = new ArrayList<>(targetMemberNoSet);
				approvalAlarmService.sendAlarmToMembers(
				    targetMemberNos,
				    saved,
				    member.getMemberName() + "님이 새로운 결재를 요청하였습니다."
				);
			}

			result = 1;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
		return result;
		
	}

	// 결재리스트 받아오기 - 보낸 문서함 출력(검색, 페이징 O)
	public Page<Approval> selectApprovalAll(MemberDto member, SearchDto searchDto, PageDto pageDto) {
		Pageable pageable = PageRequest.of(pageDto.getNowPage() -1 , pageDto.getNumPerPage(),
				Sort.by("apprRegDate").descending());
		
		if(searchDto.getOrder_type() == 2) {
			pageable = PageRequest.of(pageDto.getNowPage() -1 , pageDto.getNumPerPage(),
					Sort.by("apprRegDate").ascending());
		}
		
		Specification<Approval> spec = (root, query, criteriaBuilder) -> null;
		if(searchDto.getSearch_status() == null) {
			if("appr_title".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalTitleContains(searchDto.getSearch_text()))
						.and(ApprovalSpecification.approvalSenderContains(member.getMember_no()));
			} else if("approval_form_name".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalFormNameContains(searchDto.getSearch_text()))
						.and(ApprovalSpecification.approvalSenderContains(member.getMember_no()));
			} else {
				spec = spec.and(ApprovalSpecification.approvalSenderContains(member.getMember_no()));
			}
		} else {
			if("appr_title".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalTitleContains(searchDto.getSearch_text()))
						.and(ApprovalSpecification.approvalStatusContains(searchDto.getSearch_status()))
						.and(ApprovalSpecification.approvalSenderContains(member.getMember_no()));
			} else if("approval_form_name".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalFormNameContains(searchDto.getSearch_text()))
						.and(ApprovalSpecification.approvalStatusContains(searchDto.getSearch_status()))
						.and(ApprovalSpecification.approvalSenderContains(member.getMember_no()));
			} else {
				spec = spec.and(ApprovalSpecification.approvalStatusContains(searchDto.getSearch_status()))
						.and(ApprovalSpecification.approvalSenderContains(member.getMember_no()));
			}
		}
		Specification<Approval> dateSpec = ApprovalSpecification.approvalRegDateBetween(searchDto.getStart_date(), searchDto.getEnd_date());
		if (dateSpec != null) {
			spec = spec.and(dateSpec);
		}
		
		Page<Approval> list = approvalRepository.findAll(spec, pageable);
		
		return list;
	}
	
	// 결재리스트 받아오기 - 보낸 문서함 출력 (검색X)
	public List<Approval> selectApprovalAllById(MemberDto member) {
		
		List<Approval> approvalList = new ArrayList<Approval>();
		
		approvalList = approvalRepository.findAllByMember_MemberNo(member.getMember_no());
		
		return approvalList;
	}
	
	// 결재리스트 받아오기 - 받은 문서함 출력
	public List<ApprovalVo> selectApprovalAllByApproverId(MemberDto member, SearchDto searchDto, PageDto pageDto) {
		List<ApprovalVo> approvalVoList = new ArrayList<ApprovalVo>();
		
		Map<String,Object> paramMap = new HashMap<String,Object>();
		
		paramMap.put("member_no", member.getMember_no());
		paramMap.put("search_type", searchDto.getSearch_type());
		paramMap.put("search_text", searchDto.getSearch_text());
		paramMap.put("order_type", searchDto.getOrder_type());
		paramMap.put("search_status", searchDto.getSearch_status());
		paramMap.put("start_date", searchDto.getStart_date());
		paramMap.put("end_date", searchDto.getEnd_date());
		
		approvalVoList = approvalMapper.selectApprovalAllByMemberNo(paramMap);

		return approvalVoList;
	}
	
	public ApprovalStatusVo selectApprovalStatusByApproverId(MemberDto member) {
		ApprovalStatusVo approvalStatusVo = new ApprovalStatusVo();
		Map<String,Object> paramMap = new HashMap<String,Object>();
		paramMap.put("member_no", member.getMember_no());
		
		approvalStatusVo = approvalMapper.selectApprovalStatusByMemberNo(paramMap);
		
		return approvalStatusVo;
	}

	public Approval selectApprovalOneByApprovalNo(Long id) {
		Approval approval = approvalRepository.findById(id).orElse(null);
		return approval;
	}

	public List<ApprApprover> selectApprApproverAllByApprovalNo(Long id) {
		List<ApprApprover> approverList = apprApproverRepository.findAllByApproval_ApprNo(id);
		return approverList;
	}
	
	public List<ApprAgreementer> selectApprAgreementerAllByApprovalNo(Long id) {
		List<ApprAgreementer> agreementerList = apprAgreementerRepository.findAllByApproval_ApprNo(id);
		return agreementerList;
	}

	public List<ApprReferencer> selectApprReferencerAllByApprovalNo(Long id) {
		List<ApprReferencer> referencerList = apprReferencerRepository.findAllByApproval_ApprNo(id);
		return referencerList;
	}

	@Transactional(rollbackFor = Exception.class)
	public int confirmReferencer(Long apprNo, Long memberNo) {
		try {
			ApprReferencer referencer = apprReferencerRepository
				.findByApproval_ApprNoAndMember_MemberNo(apprNo, memberNo)
				.orElse(null);
			if (referencer != null) {
				referencer.updateReferencerStatus("Y");
				apprReferencerRepository.save(referencer);
				return 1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	// 결재자 - 결재 승인(Dto에 Setter)
	@Transactional(rollbackFor = Exception.class)
	public int approvalSuccessApi(Long id, MemberDto member) {
	    int result = 0;

	    try {
	        Approval approval = approvalRepository.findById(id).orElse(null);
	        if (approval == null) return 0;

	        Approval parentApproval = approval.getParentApproval() != null
	                ? approvalRepository.findById(approval.getParentApproval().getApprNo()).orElse(null)
	                : null;

	        ApprApprover approver = apprApproverRepository.findByMember_MemberNoAndApproval_ApprNo(member.getMember_no(), id);

	        if (approval.getApprOrderStatus() == approver.getApproverOrder()) {

	            ApprovalDto approvalDto = new ApprovalDto().toDto(approval);
	            ApprApproverDto approverDto = new ApprApproverDto().toDto(approver);

	            approvalDto.setAppr_order_status(approval.getApprOrderStatus() + 1);
	            approverDto.setApprover_decision_status("C");

	            Approval approvalParam = approvalDto.toEntity(parentApproval);
	            ApprApprover approverParam = approverDto.toEntity();

	            Approval approvalEntity = approvalRepository.save(approvalParam);
	            ApprApprover approverEntity = apprApproverRepository.save(approverParam);

	            List<ApprApprover> approverList = apprApproverRepository.findAllByApproval_ApprNo(id);
	            boolean vali = false;
	            int max = approverList.stream().mapToInt(ApprApprover::getApproverOrder).max().orElse(0);

	            if (max < approvalEntity.getApprOrderStatus()) {
	                vali = true;
	            }

	            if (vali) {
	                ApprovalDto approvalDto2 = new ApprovalDto().toDto(approvalEntity);
	                approvalDto2.setAppr_status("C");
	                approvalDto2.setAppr_res_date(approverEntity.getApproverDecisionStatusTime());

	                Approval approvalParam2 = approvalDto2.toEntity(parentApproval);
	                Approval saved = approvalRepository.save(approvalParam2);
	                
	                Long requesterNo = saved.getMember().getMemberNo();
	                List<Long> targetMemberNos = new ArrayList<>();
	                targetMemberNos.add(requesterNo);

	                approvalAlarmService.sendAlarmToMembers(
	                    targetMemberNos,
	                    saved,
	                    member.getMember_name() + "님이 결재를 최종 승인하였습니다."
	                );
	            } else {
	            	processAutoSkipAbsentApprovers(approvalEntity, approverList);
	            }
	        }

	        result = 1;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return result;
	}

	// 부재중 결재자 자동 건너뛰기(전결) 처리 헬퍼 메서드
	@Transactional(rollbackFor = Exception.class)
	public void processAutoSkipAbsentApprovers(Approval approval, List<ApprApprover> approverList) {
		if (approval == null || approverList == null || approverList.isEmpty()) return;

		Approval parentApproval = approval.getParentApproval() != null
				? approvalRepository.findById(approval.getParentApproval().getApprNo()).orElse(null)
				: null;

		int currentOrder = approval.getApprOrderStatus();
		int maxOrder = approverList.stream().mapToInt(ApprApprover::getApproverOrder).max().orElse(0);

		boolean changed = false;

		while (currentOrder <= maxOrder) {
			int targetOrder = currentOrder;
			ApprApprover targetApprover = approverList.stream()
					.filter(a -> a.getApproverOrder() == targetOrder)
					.findFirst().orElse(null);

			if (targetApprover != null && targetApprover.getMember() != null) {
				Member approverMember = memberRepository.findById(targetApprover.getMember().getMemberNo()).orElse(null);
				if (approverMember != null && "Y".equals(approverMember.getIsAbsent()) 
						&& !"C".equals(targetApprover.getApproverDecisionStatus()) 
						&& !"R".equals(targetApprover.getApproverDecisionStatus())
						&& !"J".equals(targetApprover.getApproverDecisionStatus())) {

					ApprApproverDto aDto = new ApprApproverDto().toDto(targetApprover);
					aDto.setApprover_decision_status("J");
					aDto.setApprover_decision_status_time(LocalDateTime.now());
					apprApproverRepository.save(aDto.toEntity());

					currentOrder++;
					changed = true;
					continue;
				}
			}
			break;
		}

		if (changed) {
			ApprovalDto approvalDto = new ApprovalDto().toDto(approval);
			approvalDto.setAppr_order_status(currentOrder);

			if (currentOrder > maxOrder) {
				approvalDto.setAppr_status("C");
				approvalDto.setAppr_res_date(LocalDateTime.now());

				Approval saved = approvalRepository.save(approvalDto.toEntity(parentApproval));

				Long requesterNo = saved.getMember().getMemberNo();
				List<Long> targetMemberNos = new ArrayList<>();
				targetMemberNos.add(requesterNo);

				approvalAlarmService.sendAlarmToMembers(
					targetMemberNos,
					saved,
					"부재중 결재자 자동 건너뛰기로 결재가 최종 승인되었습니다."
				);
			} else {
				Approval savedEntity = approvalRepository.save(approvalDto.toEntity(parentApproval));
				int activeOrder = currentOrder;
				for (ApprApprover a : approverList) {
					if (a.getApproverOrder() == activeOrder && a.getMember() != null) {
						List<Long> targetMemberNos = new ArrayList<>();
						targetMemberNos.add(a.getMember().getMemberNo());

						approvalAlarmService.sendAlarmToMembers(
							targetMemberNos,
							savedEntity,
							savedEntity.getMember().getMemberName() + "님이 새로운 결재를 요청하였습니다. (이전 결재자 부재로 자동 건너뜀)"
						);
						break;
					}
				}
			}
		} else {
			int activeOrder = currentOrder;
			for (ApprApprover a : approverList) {
				if (a.getApproverOrder() == activeOrder && a.getMember() != null) {
					List<Long> targetMemberNos = new ArrayList<>();
					targetMemberNos.add(a.getMember().getMemberNo());

					approvalAlarmService.sendAlarmToMembers(
						targetMemberNos,
						approval,
						approval.getMember().getMemberName() + "님이 새로운 결재를 요청하였습니다."
					);
					break;
				}
			}
		}
	}

	
	
	// 결재자 - 결재 반려(Dto에 Setter)
	@Transactional(rollbackFor = Exception.class)
	public int approvalFailApi(Long id, String reason, MemberDto member) {
		int result = 0;
		
		try {
			// 결재자맵핑 데이터를 찾아서 상태변경
			ApprApprover approver = apprApproverRepository.findByMember_MemberNoAndApproval_ApprNo(member.getMember_no(), id);
			
			ApprApproverDto approverDto = new ApprApproverDto().toDto(approver);
			
			approverDto.setApprover_decision_status("R");
			approverDto.setDecision_reason(reason);
			
			ApprApprover approvalParam = approverDto.toEntity();
			
			ApprApprover approverEntity = apprApproverRepository.save(approvalParam);
			
			Approval approval = approvalRepository.findById(id).orElse(null);
			Approval parentApproval = approval.getParentApproval() != null
	                ? approvalRepository.findById(approval.getParentApproval().getApprNo()).orElse(null)
	                : null;
			
			ApprovalDto approvalDto = new ApprovalDto().toDto(approval);
			
			approvalDto.setAppr_status("R");
			approvalDto.setAppr_res_date(approverEntity.getApproverDecisionStatusTime() != null ? approverEntity.getApproverDecisionStatusTime() : LocalDateTime.now(ZoneId.of("Asia/Seoul")));
			approvalDto.setAppr_reason(approverEntity.getDecisionReason());
			
			
			Approval approvalEntity = approvalDto.toEntity(parentApproval);
			
			Approval saved = approvalRepository.save(approvalEntity);
			
			Long requesterNo = saved.getMember().getMemberNo();
            List<Long> targetMemberNos = new ArrayList<>();
            targetMemberNos.add(requesterNo);

            approvalAlarmService.sendAlarmToMembers(
                targetMemberNos,
                saved,
                member.getMember_name() + "님이 결재를 반려하였습니다."
            );
			
			result = 1;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

	
	// 합의자 - 결재 수락(Dto에 setter)
	@Transactional(rollbackFor = Exception.class)
	public int approvalAgreeApi(Long id, MemberDto member) {
		
		int result = 0;
		
		try {
			
			// 합의자맵핑 데이터를 찾아서 상태변경
			ApprAgreementer agreementer = apprAgreementerRepository.findByMember_MemberNoAndApproval_ApprNo(member.getMember_no(), id);
			ApprAgreementerDto agreementerDto = new ApprAgreementerDto().toDto(agreementer);
			agreementerDto.setAgreementer_agree_status("C");
			
			ApprAgreementer agreementerParam = agreementerDto.toEntity();
			
			apprAgreementerRepository.save(agreementerParam);
			
			// 모든 합의자맵핑 데이터 찾기
			List<ApprAgreementer> agreementerList = apprAgreementerRepository.findAllByApproval_ApprNo(id);
			
			int checkAgreeStatus = 0;
			
			if(agreementerList != null) {
				
				// 모든 합의자가 동의("C")일 시 결재 상태변경
				for(ApprAgreementer a : agreementerList) {
					if("W".equals(a.getAgreementerAgreeStatus())) {
						checkAgreeStatus = 1;
					}
				}
				
				// checkAgreeStatus가 0이면 모든 합의자가 동의 -> 결재 순서상태 1로 변환
				if(checkAgreeStatus == 0) {
					Approval approval = approvalRepository.findById(id).orElse(null);
					ApprovalDto approvalDto = new ApprovalDto().toDto(approval);
					approvalDto.setAppr_order_status(1);
					approvalDto.setAppr_status("D");
					
					Approval approvalParam = approvalDto.toEntity();
					
					Approval saved = approvalRepository.save(approvalParam);
					
					// 1차 결재자 알림
				    List<ApprApprover> approverList = apprApproverRepository.findAllByApproval_ApprNo(id);
				    List<Long> targetMemberNos = new ArrayList<>();

				    for (ApprApprover approver : approverList) {
				        if (approver.getApproverOrder() == 1) {
				            targetMemberNos.add(approver.getMember().getMemberNo());
				        }
				    }

				    approvalAlarmService.sendAlarmToMembers(
				        targetMemberNos,
				        saved,
				        approval.getMember().getMemberName() + "님이 새로운 결재를 요청하였습니다."
				    );
					
				}
			}
			result = 1;
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
		
	}

	
	// 합의자 - 결재 거절(Dto에 Setter)
	@Transactional(rollbackFor = Exception.class)
	public int approvalRejectApi(Long id, String reason, MemberDto member) {
		
		int result = 0;
		
		try {
			
			Approval rejectCheckApproval = approvalRepository.findById(id).orElse(null);
			if("R".equals(rejectCheckApproval.getApprStatus())) {
				return 2;
			}
			
			// 합의자맵핑 데이터를 찾아서 상태변경
			ApprAgreementer agreementer = apprAgreementerRepository.findByMember_MemberNoAndApproval_ApprNo(member.getMember_no(), id);
			ApprAgreementerDto agreementerDto = new ApprAgreementerDto().toDto(agreementer);
			
			agreementerDto.setAgreementer_agree_status("R");
			agreementerDto.setAgree_reason(reason);
			
			ApprAgreementer agreementerParam = agreementerDto.toEntity();
			
			ApprAgreementer entity = apprAgreementerRepository.save(agreementerParam);
			
			Approval approval = approvalRepository.findById(id).orElse(null);
			ApprovalDto approvalDto = new ApprovalDto().toDto(approval);
			approvalDto.setAppr_status("R");
			approvalDto.setAppr_res_date(entity.getAgreementerAgreeStatusTime() != null ? entity.getAgreementerAgreeStatusTime() : LocalDateTime.now(ZoneId.of("Asia/Seoul")));
			approvalDto.setAppr_reason(entity.getAgreeReason());
			
			Approval approvalParam = approvalDto.toEntity();
			
			Approval saved = approvalRepository.save(approvalParam);
			
			Long requesterNo = saved.getMember().getMemberNo();
            List<Long> targetMemberNos = new ArrayList<>();
            targetMemberNos.add(requesterNo);

            approvalAlarmService.sendAlarmToMembers(
                targetMemberNos,
                saved,
                member.getMember_name() + "님이 결재를 반려하였습니다."
            );
			
			result = 1;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	
	// 결재 회수
	@Transactional(rollbackFor = Exception.class)
	public int approvalReturnApi(Long id, String reason) {
		
		int result = 0;
		
		try {
			
			Approval approvalParam = approvalRepository.findById(id).orElse(null);
			List<ApprApprover> approvers = apprApproverRepository.findAllByApproval_ApprNo(approvalParam.getApprNo());
			int lastApproverId = 0;
			ApprApprover approver = null;
			for(ApprApprover a : approvers) {
				if(lastApproverId < a.getApproverOrder()) {
					lastApproverId = a.getApproverOrder();
				}
			}
			for(ApprApprover a : approvers) {
				if(lastApproverId == a.getApproverOrder()) {
					approver = a;
				}
			}
			System.out.println("dto 저장전 : "+reason);
			// 결재자 찾음
			// 결재를 재생성 하고, 결재자(ApprApprover) 재생성
			ApprovalDto approvalDto = ApprovalDto.builder()
					.appr_title("[결재 회수]"+approvalParam.getApprTitle())
					.appr_text(approvalParam.getApprText())
					.appr_status("D")
					.appr_order_status(1)
					.start_date(approvalParam.getStartDate())
					.end_date(approvalParam.getEndDate())
					.use_annual_leave(approvalParam.getUseAnnualLeave())
					.annual_leave_type(approvalParam.getAnnualLeaveType())
					.return_reason(reason)
					.appr_sender(approvalParam.getMember().getMemberNo())
					.approval_type_no(approvalParam.getApprovalForm().getApprovalFormNo())
					.parent_approval(approvalParam.getApprNo())
					.build();
			System.out.println("dto 저장후 : "+approvalDto.getReturn_reason());
			Approval newApproval = approvalDto.toEntity(approvalParam);
			
			System.out.println("entity 저장후 : "+newApproval.getReturnReason());
			Approval entity = approvalRepository.save(newApproval);
			System.out.println("데이터 저장후 : "+entity.getReturnReason());
			
			ApprApproverDto apprApproverDto = ApprApproverDto.builder()
					.approver_order(1)
					.approver_decision_status("W")
					.appr_no(entity.getApprNo())
					.approver(approver.getMember().getMemberNo())
					.build();
			
			ApprApprover approverEntity = apprApproverDto.toEntity();
			ApprApprover savedApprover = apprApproverRepository.save(approverEntity);
			
			// 알림 보내기: 최종 결재자에게 결재 회수 알림
			List<Long> targetMemberNos = new ArrayList<>();
			targetMemberNos.add(savedApprover.getMember().getMemberNo());

			approvalAlarmService.sendAlarmToMembers(
			    targetMemberNos,
			    entity,
			    approvalParam.getMember().getMemberName() + "님이 결재 회수를 요청하였습니다."
			);
			
			result = 1;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
		return result;
	}
	

	// 결재 삭제 가능 여부 확인
	public boolean isApprovalDeletable(Approval approval, List<ApprApprover> approverList, int returnResult) {
		if (approval == null) return false;
		if (!"D".equals(approval.getApprStatus())) return false;
		if (approval.getApprOrderStatus() > 1) return false;

		if (approverList != null && !approverList.isEmpty()) {
			for (ApprApprover approver : approverList) {
				if (!"W".equals(approver.getApproverDecisionStatus())) {
					return false;
				}
			}
		}

		if (returnResult != 0) {
			return false;
		}

		return true;
	}

	// 결재 삭제
	@Transactional(rollbackFor = Exception.class)
	public int deleteApprovalApi(Long id) {
		int result = 0;
		try {
			Approval approval = approvalRepository.findById(id).orElse(null);
			if (approval == null) return 0;

			// 1. 결재 대기 중('D') 상태인지 확인
			if (!"D".equals(approval.getApprStatus())) {
				return -1;
			}

			// 2. 1차 결재 발생 여부 확인 (결재 순서가 1을 초과했거나 결재자 의사결정 상태가 대기('W')가 아닌 경우)
			if (approval.getApprOrderStatus() > 1) {
				return -2;
			}

			List<ApprApprover> approvers = apprApproverRepository.findAllByApproval_ApprNo(id);
			if (approvers != null && !approvers.isEmpty()) {
				for (ApprApprover approver : approvers) {
					if (!"W".equals(approver.getApproverDecisionStatus())) {
						return -2;
					}
				}
			}

			// 3. 결재 회수 관련 확인
			int returnResult = selectReturnApprovalByApprovalNo(id);
			if (returnResult != 0) {
				return -3;
			}

			// 1. 결재와 관련된 알림 및 알림 맵핑 삭제 (외래키 제약조건 해제)
			List<Alarm> alarms = alarmRepository.findAllByApproval_ApprNo(id);
			if (alarms != null && !alarms.isEmpty()) {
				for (Alarm alarm : alarms) {
					alarmMappingRepository.deleteAllByAlarm_AlarmNo(alarm.getAlarmNo());
				}
				alarmRepository.deleteAll(alarms);
			}

			// 2. 첨부파일 삭제
			List<ApprovalAttach> attaches = approvalAttachService.findByApproval(approval);
			if (attaches != null) {
				for (ApprovalAttach attach : attaches) {
					approvalAttachService.deleteAttachById(attach.getAttachNo());
				}
			}

			// 3. 결재자, 합의자, 회람자 삭제
			if (approvers != null && !approvers.isEmpty()) {
				apprApproverRepository.deleteAll(approvers);
			}

			List<ApprAgreementer> agreementers = apprAgreementerRepository.findAllByApproval_ApprNo(id);
			if (agreementers != null && !agreementers.isEmpty()) {
				apprAgreementerRepository.deleteAll(agreementers);
			}

			List<ApprReferencer> referencers = apprReferencerRepository.findAllByApproval_ApprNo(id);
			if (referencers != null && !referencers.isEmpty()) {
				apprReferencerRepository.deleteAll(referencers);
			}

			// 4. 자식 결재 문서의 parentApproval 참조 해제
			List<Approval> childApprovals = approvalRepository.findAllByParentApproval_ApprNo(id);
			if (childApprovals != null && !childApprovals.isEmpty()) {
				for (Approval child : childApprovals) {
					ApprovalDto dto = new ApprovalDto().toDto(child);
					Approval updatedChild = dto.toEntity(null);
					approvalRepository.save(updatedChild);
				}
			}

			// 5. 결재 문서 삭제
			approvalRepository.deleteById(id);
			approvalRepository.flush();

			result = 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	// 결재 중 회수 대기나 회수 승인이 있는 결재
	public int selectReturnApprovalByApprovalNo(Long id) {
		
		int result = 0;
		
		List<Approval> childApproval = approvalRepository.findAllByParentApproval_ApprNo(id);
//		Specification<Approval> spec = (root, query, criteriaBuilder) -> null;
//		spec = spec.and(ApprovalSpecification.approvalReturnApprovalContains(id));
//
//		childApproval = approvalRepository.findAll(spec);
		
		
		for(Approval a : childApproval) {
			System.out.println("자식결재 : "+a.getApprNo());
		}
		
		if(childApproval != null) {
			for(Approval a : childApproval) {
				if("C".equals(a.getApprStatus()) || "D".equals(a.getApprStatus())) {
					result = 1;
				}
			}
		}
		
		// result가 1이면 회수버튼이 생기면 안되고 0일 때 생겨야함
		return result;
	}
	
	// 결재 재기안
	@Transactional(rollbackFor = Exception.class)
	public int retryApprovalApi(ApprovalDto approvalDto, List<MultipartFile> files, List<Long> deleteFiles) {
		int result = 0;
		
		try {
			// 이전 결재 entity
			Approval entity = approvalRepository.findById(approvalDto.getAppr_no()).orElse(null);
			// 합의자 여부 확인
			List<ApprAgreementer> agreementers = apprAgreementerRepository.findAllByApproval_ApprNo(entity.getApprNo());
			
			System.out.println(approvalDto.getApprover_no());
			// 합의자가 있으면 합의자 먼저, 없으면 바로 결재자로
			if(agreementers.size() != 0) {
				approvalDto.setAppr_status("A");
				approvalDto.setAppr_order_status(0);
			} else {
				approvalDto.setAppr_status("D");
				approvalDto.setAppr_order_status(1);
			}
			
			approvalDto.setAppr_res_date(null);
			approvalDto.setAppr_reason(null);
			approvalDto.setAppr_reg_date(LocalDateTime.now());
			
			Approval newEntity = approvalDto.toEntity();
			
			Approval saved = approvalRepository.save(newEntity);
			
			// Reset approvers and agreementers to Waiting ("W") status
			List<ApprApprover> approverListForReset = apprApproverRepository.findAllByApproval_ApprNo(entity.getApprNo());
			for (ApprApprover approver : approverListForReset) {
				ApprApproverDto approverDto = new ApprApproverDto().toDto(approver);
				approverDto.setApprover_decision_status("W");
				approverDto.setDecision_reason(null);
				approverDto.setApprover_decision_status_time(null);
				apprApproverRepository.save(approverDto.toEntity());
			}

			for (ApprAgreementer agreementer : agreementers) {
				ApprAgreementerDto agreementerDto = new ApprAgreementerDto().toDto(agreementer);
				agreementerDto.setAgreementer_agree_status("W");
				agreementerDto.setAgree_reason(null);
				agreementerDto.setAgreementer_agree_status_time(null);
				apprAgreementerRepository.save(agreementerDto.toEntity());
			}
			
			if (deleteFiles != null) {
				System.out.println("test");
		        for (Long id : deleteFiles) {
		            try {
		                approvalAttachService.deleteAttachById(id);  // 실제 삭제
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		        }
		    }
			
			if (files != null && !files.isEmpty()) {
		        for (MultipartFile file : files) {
		            if (!file.isEmpty()) {
		                try {
		                	approvalAttachService.saveFile(file, newEntity);
		                } catch (IOException e) {
		                    e.printStackTrace();
		                    return 0;
		                }
		            }
		        }
		    }
			
			Set<Long> targetMemberNoSet = new HashSet<>();
			
			List<ApprReferencer> referencers = apprReferencerRepository.findAllByApproval_ApprNo(entity.getApprNo());

			boolean hasAgreementers = agreementers != null && !agreementers.isEmpty();
			boolean hasReferencers = referencers != null && !referencers.isEmpty();

			// 합의자
			if (hasAgreementers) {
				for(ApprAgreementer ag : agreementers) {
					targetMemberNoSet.add(ag.getMember().getMemberNo());
				}
			}

			// 참조자
			if (hasReferencers) {
				for(ApprReferencer rf : referencers) {
					targetMemberNoSet.add(rf.getMember().getMemberNo());
				}
			}

			// 합의자가 없을 경우 → 1차 결재자 추가
			if (!hasAgreementers) {

			    List<ApprApprover> approverList = apprApproverRepository.findAllByApproval_ApprNo(entity.getApprNo());
			    for (ApprApprover a : approverList) {
			        if (a.getApproverOrder() == 1 && a.getMember() != null && a.getMember().getMemberNo() != null) {
			            targetMemberNoSet.add(a.getMember().getMemberNo());
			            break;
			        }
			    }
			}

			// 알림 전송
			List<Long> targetMemberNos = new ArrayList<>(targetMemberNoSet);
			approvalAlarmService.sendAlarmToMembers(
			    targetMemberNos,
			    saved,
			    saved.getMember().getMemberName() + "님이 새로운 결재를 요청하였습니다."
			);
			
			result = 1;
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

	// 양식/부서별 DB 결재선 및 조직도 기반 자동 결재선 통합 조회
	public List<AutoApprovalLineResponseDto> selectAutoApprovalLineResponse(Long memberNo, Long formNo) {
		List<AutoApprovalLineResponseDto> resultList = new ArrayList<>();
		Member drafter = memberRepository.findById(memberNo).orElse(null);
		if (drafter == null) {
			return resultList;
		}

		// 1. DB에 설정된 양식/부서별 결재선 조회
		ApprovalLine approvalLine = null;
		if (formNo != null && drafter.getDept() != null) {
			approvalLine = approvalLineRepository.findByFormNoAndDeptNo(formNo, drafter.getDept().getDeptNo()).orElse(null);
		}
		if (approvalLine == null && formNo != null) {
			approvalLine = approvalLineRepository.findByFormNoAndDeptIsNull(formNo).orElse(null);
		}

		if (approvalLine != null) {
			List<ApprovalLineDetail> details = approvalLineDetailRepository
					.findAllByApprovalLine_LineIdOrderByApprOrderAsc(approvalLine.getLineId());
			int approverOrder = 1;
			for (ApprovalLineDetail detail : details) {
				Member m = detail.getMember();
				// 기안자 본인이 결재선 목록에 존재할 경우 본인 셀프 결재 방지를 위해 자동 제외
				if (m != null && m.getStatus() != 3 && !m.getMemberNo().equals(memberNo)) {
					String type = detail.getApprType() != null ? detail.getApprType() : "APPROVER";
					int order = "APPROVER".equals(type) ? approverOrder++ : detail.getApprOrder();

					resultList.add(AutoApprovalLineResponseDto.builder()
							.member_no(m.getMemberNo())
							.member_name(m.getMemberName())
							.dept_name(m.getDept() != null ? m.getDept().getDeptName() : "")
							.pos_name(m.getPos() != null ? m.getPos().getPosName() : "")
							.appr_type(type)
							.appr_order(order)
							.build());
				}
			}
			if (!resultList.isEmpty()) {
				return resultList;
			}
		}

		// 2. DB 결재선이 없을 시 조직도 기반 상급자 추산
		List<MemberDto> orgLine = selectAutoApprovalLine(memberNo);
		int order = 1;
		for (MemberDto mDto : orgLine) {
			resultList.add(AutoApprovalLineResponseDto.builder()
					.member_no(mDto.getMember_no())
					.member_name(mDto.getMember_name())
					.dept_name(mDto.getDept_name())
					.pos_name(mDto.getPos_name())
					.appr_type("APPROVER")
					.appr_order(order++)
					.build());
		}

		return resultList;
	}

	// 기존 조직도 기반 상급자 추산 메서드
	public List<MemberDto> selectAutoApprovalLine(Long memberNo) {
		List<MemberDto> autoLine = new ArrayList<>();
		Member drafter = memberRepository.findById(memberNo).orElse(null);
		if (drafter == null || drafter.getDept() == null) {
			return autoLine;
		}

		Set<Long> addedMemberNos = new HashSet<>();
		addedMemberNos.add(drafter.getMemberNo()); // 기안자 본인 제외

		com.mjc.groupware.dept.entity.Dept currentDept = drafter.getDept();

		while (currentDept != null && autoLine.size() < 5) {
			Member deptHead = currentDept.getMember();
			boolean headAdded = false;

			if (deptHead != null && deptHead.getStatus() != 3 && !addedMemberNos.contains(deptHead.getMemberNo())) {
				addedMemberNos.add(deptHead.getMemberNo());
				autoLine.add(new MemberDto().toDto(deptHead));
				headAdded = true;
			}

			if (!headAdded) {
				List<Member> deptMembers = memberRepository.findAllByDeptNoSortedByPosOrder(currentDept.getDeptNo());
				if (deptMembers != null) {
					for (Member m : deptMembers) {
						if (m.getStatus() != 3 && !addedMemberNos.contains(m.getMemberNo())) {
							if (drafter.getPos() == null || m.getPos() == null || 
								(m.getPos().getPosOrder() != null && drafter.getPos().getPosOrder() != null && 
								 m.getPos().getPosOrder() < drafter.getPos().getPosOrder())) {
								addedMemberNos.add(m.getMemberNo());
								autoLine.add(new MemberDto().toDto(m));
								break;
							}
						}
					}
				}
			}

			currentDept = currentDept.getParentDept();
		}

		// 임원 부서(양인수 전무이사, 양철수 대표이사) 최종 결재자 자동 추가
		List<Member> executives = memberRepository.findExecutivesSortedByPosOrderDesc();
		if (executives != null) {
			for (Member exec : executives) {
				if (!addedMemberNos.contains(exec.getMemberNo())) {
					if (drafter.getPos() == null || exec.getPos() == null ||
						(exec.getPos().getPosOrder() != null && drafter.getPos().getPosOrder() != null &&
						 exec.getPos().getPosOrder() < drafter.getPos().getPosOrder())) {
						addedMemberNos.add(exec.getMemberNo());
						autoLine.add(new MemberDto().toDto(exec));
					}
				}
			}
		}

		return autoLine;
	}

	// 관리자 : 전사 전체 결재 관리 조회 (페이징, 검색, 상태별 필터링)
	public Page<Approval> selectApprovalAdminAll(SearchDto searchDto, PageDto pageDto) {
		Pageable pageable = PageRequest.of(pageDto.getNowPage() - 1, pageDto.getNumPerPage(),
				Sort.by("apprRegDate").descending());
		
		if(searchDto.getOrder_type() == 2) {
			pageable = PageRequest.of(pageDto.getNowPage() - 1, pageDto.getNumPerPage(),
					Sort.by("apprRegDate").ascending());
		}
		
		Specification<Approval> spec = Specification.where(null);
		
		if(searchDto.getSearch_text() != null && !searchDto.getSearch_text().trim().isEmpty()) {
			String searchText = searchDto.getSearch_text().trim();
			if("appr_title".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalTitleContains(searchText));
			} else if("approval_form_name".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalFormNameContains(searchText));
			} else if("member_name".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalMemberNameContains(searchText));
			} else if("dept_name".equals(searchDto.getSearch_type())) {
				spec = spec.and(ApprovalSpecification.approvalDeptNameContains(searchText));
			} else {
				Specification<Approval> titleSpec = ApprovalSpecification.approvalTitleContains(searchText);
				Specification<Approval> formSpec = ApprovalSpecification.approvalFormNameContains(searchText);
				Specification<Approval> memberSpec = ApprovalSpecification.approvalMemberNameContains(searchText);
				Specification<Approval> deptSpec = ApprovalSpecification.approvalDeptNameContains(searchText);
				spec = spec.and(titleSpec.or(formSpec).or(memberSpec).or(deptSpec));
			}
		}
		
		if(searchDto.getSearch_status() != null && !searchDto.getSearch_status().trim().isEmpty()) {
			spec = spec.and(ApprovalSpecification.approvalStatusContains(searchDto.getSearch_status().trim()));
		}
		
		Specification<Approval> dateSpec = ApprovalSpecification.approvalRegDateBetween(searchDto.getStart_date(), searchDto.getEnd_date());
		if (dateSpec != null) {
			spec = spec.and(dateSpec);
		}
		
		return approvalRepository.findAll(spec, pageable);
	}

	// 관리자 : 전사 전체 결재건의 상태별 건수 통계
	public ApprovalStatusTypeDto selectApprovalAdminStatusType() {
		ApprovalStatusTypeDto astd = new ApprovalStatusTypeDto();
		List<Object[]> counts = approvalRepository.countGroupedByApprStatus();
		if (counts != null) {
			for (Object[] row : counts) {
				String status = (String) row[0];
				long count = ((Number) row[1]).longValue();
				if ("A".equals(status)) astd.setCount_A((int) count);
				else if ("D".equals(status)) astd.setCount_D((int) count);
				else if ("R".equals(status)) astd.setCount_R((int) count);
				else if ("C".equals(status)) astd.setCount_C((int) count);
			}
		}
		return astd;
	}

}
