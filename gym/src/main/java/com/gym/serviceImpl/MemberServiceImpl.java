package com.gym.serviceImpl;

import com.gym.entity.Member;
import com.gym.mapper.MemberMapper;
import com.gym.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberMapper memberMapper;

    @Override
    public List<Member> getAllMembers() {
        return memberMapper.getAllMembers();
    }

    @Override
    public Member getMemberByAccount(Integer memberAccount) {
        return memberMapper.getMemberByAccount(memberAccount);
    }

    @Override
    public void addMember(Member member) {
        memberMapper.addMember(member);
    }

    @Override
    public void updateMember(Member member) {
        memberMapper.updateMember(member);
    }

    @Override
    public void deleteMember(Integer memberAccount) {
        memberMapper.deleteMember(memberAccount);
    }
}
