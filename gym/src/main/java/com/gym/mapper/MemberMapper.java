package com.gym.mapper;

import com.gym.entity.Member;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MemberMapper {
    List<Member> getAllMembers();
    Member getMemberByAccount(Integer memberAccount);
    void addMember(Member member);
    void updateMember(Member member);
    void deleteMember(Integer memberAccount);
}
