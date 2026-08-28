package com.mjc.groupware.common.websocket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mjc.groupware.common.websocket.entity.AlarmMapping;

public interface AlarmMappingRepository extends JpaRepository<AlarmMapping, Long> {
	Optional<AlarmMapping> findByAlarm_AlarmNoAndMember_MemberNo(Long alarmNo, Long memberNo);
	List<AlarmMapping> findByMember_MemberNoAndReadYnOrderByAlarm_RegDateDesc(Long memberNo, String readYn);
	void deleteAllByAlarm_AlarmNo(Long alarmNo);
}
