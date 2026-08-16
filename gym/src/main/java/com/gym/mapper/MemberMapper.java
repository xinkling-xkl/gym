package com.gym.mapper;

import com.gym.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MemberMapper {
    List<Member> getAllMembers();
    Member getMemberByAccount(Integer memberAccount);
    void addMember(Member member);
    void updateMember(Member member);
    void deleteMember(Integer memberAccount);
    int deductClass(Integer memberAccount);
    int refundClass(Integer memberAccount);
    int updatePassword(@Param("memberAccount") Integer memberAccount, @Param("password") String password);

    /**
     * 查询所有已过期的会员（card_expire_date 早于指定日期）
     * @param today 当前日期，由上层 TimeService 提供（支持 mock.time）
     */
    List<Member> getExpiredMembers(@Param("today") LocalDate today);

    /**
     * 会员自助修改个人资料（仅更新姓名/性别/年龄/身高/体重/电话/头像，不涉及卡信息）
     */
    int updateProfile(Member member);

    /**
     * 前台续费：更新卡类型、办卡时间、过期时间、剩余天数
     */
    int renewMember(Member member);
}
