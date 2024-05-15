package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String cacheShoptypeKey = CACHE_SHOPTYPE_KEY;
        String shopType = stringRedisTemplate.opsForValue().get(cacheShoptypeKey);

        if(shopType != null){
            List<ShopType> list = JSONUtil.toList(shopType, ShopType.class);
            return Result.ok(list);
        }

        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        if(shopTypeList == null || shopTypeList.isEmpty()){
            return Result.fail("商品分类列表不存在");
        }

        stringRedisTemplate.opsForValue().set(cacheShoptypeKey,JSONUtil.toJsonStr(shopTypeList),
                CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(shopTypeList);
    }
}
