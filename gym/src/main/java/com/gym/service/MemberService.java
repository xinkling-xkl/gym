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
    boolean updatePassword(Integer memberAccount, String password);

    /**
     * 查询所有已过期的会员
     */
    List<Member> getExpiredMembers();

    /**
     * 会员自助修改个人资料（不涉及卡类型/时长）
     */
    boolean updateProfile(Member member);

    /**
     * 前台续费：根据卡类型或自定义天数延长会员卡有效期
     * @param memberAccount 会员账号
     * @param cardClass 卡类型(1月卡/2季卡/3年卡)，为空时按 addDays 续费
     * @param addDays 自定义续费天数，cardClass 为空时生效
     */
    boolean renewMember(Integer memberAccount, Integer cardClass, Integer addDays);
}
