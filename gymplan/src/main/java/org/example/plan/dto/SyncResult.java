package org.example.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 课程同步结果：新增的训练项数 + 移除的失效训练项数
 */
@Data
@AllArgsConstructor
public class SyncResult {
    private int inserted;
    private int removed;
}
