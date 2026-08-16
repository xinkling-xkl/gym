package com.gym.serviceImpl;

import com.gym.entity.Member;
import com.gym.mapper.MemberMapper;
import com.gym.service.MemberService;
import com.gym.service.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private TimeService timeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean isEncrypted(String pwd) {
        return pwd != null && (pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$"));
    }

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
        calcCardTime(member);
        if (member.getMemberPassword() != null && !isEncrypted(member.getMemberPassword())) {
            member.setMemberPassword(passwordEncoder.encode(member.getMemberPassword()));
        }
        memberMapper.addMember(member);
    }

    @Override
    public void updateMember(Member member) {
        calcCardTime(member);
        // 全字段更新：密码为空或已加密则回填原密码，明文则加密
        if (member.getMemberPassword() == null || member.getMemberPassword().isEmpty()
                || isEncrypted(member.getMemberPassword())) {
            Member origin = memberMapper.getMemberByAccount(member.getMemberAccount());
            member.setMemberPassword(origin != null ? origin.getMemberPassword() : null);
        } else {
            member.setMemberPassword(passwordEncoder.encode(member.getMemberPassword()));
        }
        memberMapper.updateMember(member);
    }

    @Override
    public void deleteMember(Integer memberAccount) {
        memberMapper.deleteMember(memberAccount);
    }

    @Override
    public boolean updatePassword(Integer memberAccount, String password) {
        String encoded = isEncrypted(password) ? password : passwordEncoder.encode(password);
        return memberMapper.updatePassword(memberAccount, encoded) > 0;
    }

    /**
     * 根据卡类型自动计算会员卡到期时间和剩余天数：
     * 1=月卡(+1月, 30天)  2=季卡(+3月, 90天)  3=年卡(+1年, 365天)
     * cardTime = 办卡当天；cardExpireDate = 自动计算的到期日期；cardNextClass = 剩余天数初值
     */
    private void calcCardTime(Member member) {
        Integer cardClass = member.getCardClass();
        if (cardClass == null || cardClass == 0) return;
        LocalDate now = timeService.nowDate();
        LocalDate expireDate = switch (cardClass) {
            case 1 -> {
                member.setCardNextClass(30);
                yield now.plusMonths(1);
            }
            case 2 -> {
                member.setCardNextClass(90);
                yield now.plusMonths(3);
            }
            case 3 -> {
                member.setCardNextClass(365);
                yield now.plusYears(1);
            }
            default -> null;
        };
        if (expireDate != null) {
            member.setCardTime(now);
            member.setCardExpireDate(expireDate);
        }
    }

    @Override
    public boolean deductClass(Integer memberAccount) {
        return memberMapper.deductClass(memberAccount) > 0;
    }

    @Override
    public boolean refundClass(Integer memberAccount) {
        return memberMapper.refundClass(memberAccount) > 0;
    }

    @Override
    public List<Member> getExpiredMembers() {
        // 使用 TimeService 当前日期，支持 mock.time 模拟任意时间点
        return memberMapper.getExpiredMembers(timeService.nowDate());
    }

    @Override
    public boolean updateProfile(Member member) {
        if (member.getMemberAccount() == null) return false;
        return memberMapper.updateProfile(member) > 0;
    }

    /**
     * 前台续费：根据卡类型或自定义天数延长会员卡有效期
     * - 指定 cardClass(1/2/3)：按月卡/季卡/年卡对应时长续费
     * - cardClass 为空但指定 addDays：按自定义天数续费
     * - 续费起点：若原过期时间未过期则从原过期时间往后加，否则从今天往后加
     */
    @Override
    public boolean renewMember(Integer memberAccount, Integer cardClass, Integer addDays) {
        Member member = memberMapper.getMemberByAccount(memberAccount);
        if (member == null) return false;

        LocalDate now = timeService.nowDate();
        LocalDate base = (member.getCardExpireDate() != null
                && member.getCardExpireDate().isAfter(now))
                ? member.getCardExpireDate() : now;

        LocalDate newExpire;
        int newDays;
        if (cardClass != null && cardClass > 0) {
            newExpire = switch (cardClass) {
                case 1 -> base.plusMonths(1);
                case 2 -> base.plusMonths(3);
                case 3 -> base.plusYears(1);
                default -> null;
            };
            if (newExpire == null) return false;
            newDays = (int) java.time.temporal.ChronoUnit.DAYS.between(now, newExpire);
            member.setCardClass(cardClass);
        } else if (addDays != null && addDays > 0) {
            newExpire = base.plusDays(addDays);
            newDays = (int) java.time.temporal.ChronoUnit.DAYS.between(now, newExpire);
        } else {
            return false;
        }

        member.setCardTime(now);
        member.setCardExpireDate(newExpire);
        member.setCardNextClass(newDays);
        return memberMapper.renewMember(member) > 0;
    }
}
