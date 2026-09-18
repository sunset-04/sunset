package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@RequiredArgsConstructor
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopList() {
        String key = CACHE_SHOP_TYPE_KEY;
        // 1.从redis中查询店铺类型
        List<String> shopTypeLists = stringRedisTemplate.opsForList().range(key, 0, -1);
        // 2.判断redis中是否存在
        if(shopTypeLists != null && !shopTypeLists.isEmpty()){
            // 3.存在,直接返回
            List<ShopType> shopTypeList = shopTypeLists.stream().map(json -> JSONUtil.toBean(json, ShopType.class)).toList();
            return Result.ok(shopTypeList);
        }
        // 4.不存在,从数据库查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 5.判断数据库是否存在
        if(shopTypes == null || shopTypes.isEmpty()){
            // 6.不存在,返回错误
            return Result.fail("店铺类型不存在");
        }
        // 7.存在,将数据存入redis中
        List<String> list = shopTypes.stream().map(JSONUtil::toJsonStr).toList();
        stringRedisTemplate.opsForList().rightPushAll(key, list);
        // 8.返回
        return Result.ok(shopTypes);
    }

//    /**
//     * String类型查询店铺类型
//     * @return 店铺类型
//     */
//    @Override
//    public Result queryShopList() {
//        String key = CACHE_SHOP_TYPE_KEY;
//        // 1.从redis中查询店铺类型
//        String shopTypeJson = stringRedisTemplate.opsForValue().get(key);
//        // 2.判断是否存在
//        if (StrUtil.isNotBlank(shopTypeJson)) {
//            // 3.存在,直接返回
//            List<ShopType> shopTypeList = JSONUtil.toList(shopTypeJson, ShopType.class);
//            return Result.ok(shopTypeList);
//        }
//        // 4.不存在,从数据库查询
//        List<ShopType> shopTypes = query().orderByAsc("sort").list();
//        // 5.判断数据库中是否存在
//        if (shopTypes == null) {
//            // 6.不存在,返回错误
//            return Result.fail("店铺类型不存在");
//        }
//        // 7.存在,将shop类型存入redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypes));
//        // 8.返回
//        return Result.ok(shopTypes);
//    }
}
