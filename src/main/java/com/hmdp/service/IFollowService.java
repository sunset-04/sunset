package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注/取关
     * @param followUserId 目标用户id
     * @param isFollow true=关注 false=取关
     * @return 结果
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     * 判断是否关注某用户
     * @param followUserId 目标用户id
     * @return 结果（data=true/false）
     */
    Result isFollow(Long followUserId);

    /**
     * 查询与目标用户的共同关注
     * @param id 目标用户id
     * @return 共同关注的用户列表
     */
    Result followCommons(Long id);
}
