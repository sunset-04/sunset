package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 查看探店笔记详情
     * @param id 笔记id
     * @return 笔记详情（含发布人信息、当前用户是否点赞）
     */
    Result queryBlogById(Long id);

    /**
     * 点赞 / 取消点赞
     * @param id 笔记id
     * @return 结果
     */
    Result likeBlog(Long id);

    /**
     * 点赞排行榜（按点赞时间倒序）
     * @param id 笔记id
     * @return 点赞用户列表
     */
    Result queryBlogLikes(Long id);
}
