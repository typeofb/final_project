package com.mjc.groupware.approval.dto;

import java.util.ArrayList;
import java.util.List;

import com.mjc.groupware.approval.entity.ApprReferencer;
import com.mjc.groupware.approval.entity.Approval;
import com.mjc.groupware.member.entity.Member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprReferencerDto {
	private Long appr_referencer_no;
	private Long appr_no;
	private List<Long> referencers;
	
	public List<ApprReferencer> toEntityList() {
		List<ApprReferencer> entityList = new ArrayList<>();
		if (referencers == null) return entityList;
		
		for(Long no : referencers) {
			ApprReferencer referencer = ApprReferencer.builder()
					.approval(Approval.builder().apprNo(appr_no).build())
					.member(Member.builder().memberNo(no).build())
					.build();
			
			entityList.add(referencer);
		}
		
		return entityList;
	}
	
}
