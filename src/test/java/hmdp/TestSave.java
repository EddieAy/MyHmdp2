package hmdp;

import com.hmdp.HmDianPingApplication;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SpringBootTest(classes = HmDianPingApplication.class)
public class TestSave {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

/*    @Test
    void t1() throws InterruptedException {
        shopService.saveShop2Redis(1L,20L);
    }*/

    @Test
    void t2(){
        Shop shop = shopService.getById(1L);
        String key = RedisConstants.CACHE_SHOP_KEY + 1L;
        cacheClient.setLogicalExpire(key,shop,10L, TimeUnit.SECONDS);
    }


    @Test
    void t3(){
        List<Shop> list = shopService.list();

        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));

        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            Long typeId = entry.getKey();

            String key = "shop:geo:" + typeId;

            List<Shop> value = entry.getValue();

            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            for (Shop shop : value) {
                locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),
                        new Point(shop.getX(),shop.getY())));
            }
            stringRedisTemplate.opsForGeo().add(key,locations);
        }
    }
}
