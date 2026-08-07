package com.mjc.groupware.approval.dto;

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
public class SearchDto {
	private String search_text;
	private String search_type;
	private int order_type;
	private String search_status;
	private String start_date;
	private String end_date;
	private String date_range_preset;
}
