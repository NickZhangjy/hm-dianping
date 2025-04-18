package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //解决缓存穿透
//        Shop shop = cacheClient
//                .queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
        Shop shop = cacheClient
                .queryWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        //返回
        return Result.ok(shop);
    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool( 10);
//
//    public Shop queryWithLogicalExpire(Long id) {
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        //从redis查询商户缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        //判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            //未命中，直接返回
//            return null;
//        }
//
//        //命中，把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        //判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())) {
//            //未过期，直接返回店铺信息
//            return shop;
//        }
//
//        //已过期，需要缓存重建
//        //缓存重建
//        //获取互斥锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//
//        //判断是否获取成功
//        if (isLock) {
//            //成功，开启独立线程，实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    //重建缓存
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    //释放锁
//                    unlock(lockKey);
//                }
//            });
//        }
//
//        //失败，直接返回过期商铺信息
//        return shop;
//    }
//
//    public Shop queryWithPassThrough(Long id) {
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        //从redis查询商户缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //存在，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        //判断命中的是否为空值
//        if(shopJson != null){
//            //返回错误信息
//            return null;
//        }
//
//        //不存在，根据id查询数据库
//        Shop shop = getById(id);
//
//        //不存在，返回错误
//        if (shop == null) {
//            //将空值写入redis
//            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//            //返回错误信息
//            return null;
//        }
//
//        //存在，写入redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//
//        //返回
//        return shop;
//    }
//
//    public Shop queryWithMutex(Long id) {
//        String key = RedisConstants.CACHE_SHOP_KEY + id;
//        //从redis查询商户缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //存在，直接返回
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//
//        //判断命中的是否为空值
//        if(shopJson != null){
//            //返回错误信息
//            return null;
//        }
//
//        //实现缓存重建
//        //获取互斥锁
//        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//
//            //判断是否获取成功
//            if (!isLock) {
//                //失败，则休眠重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//
//            //成功，根据id查询数据库
//            shop = getById(id);
//            //模拟重建延迟
//            Thread.sleep(200);
//
//            //不存在，返回错误
//            if (shop == null) {
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//                //返回错误信息
//                return null;
//            }
//
//            //存在，写入redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            //释放互斥锁
//            unlock(lockKey);
//        }
//
//        //返回
//        return shop;
//    }
//
//    //获取互斥锁
//    private boolean tryLock(String key){
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//
//    //释放互斥锁
//    private void unlock(String key){
//        stringRedisTemplate.delete(key);
//    }

    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = getById(id);
        //模拟延迟
        Thread.sleep(200);

        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        //写入Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {

        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);

        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + id);

        //返回结果
        return Result.ok();
    }
}
