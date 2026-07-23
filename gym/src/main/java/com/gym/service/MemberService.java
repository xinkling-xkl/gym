package com.gym.service;

import com.gym.entity.Member;

import java.util.List;

public interface MemberService {
    List<Member> getAllMembers();
    Member getMemberByAccount(Integer memberAccount);
    void addMember(Member member);
    void updateMember(Member member);
    void deleteMember(Integer memberAccount);
    boolean deductClass(Integer memberAccount);
    boolean refundClass(Integer memberAccount);
}
