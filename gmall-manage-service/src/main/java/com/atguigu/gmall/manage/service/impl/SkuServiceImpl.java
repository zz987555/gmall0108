package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        // 插入skuInfo
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }


    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId) {

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImages);
        return skuInfo;
    }

    //上面方法被下面这个替代
    @Override
    public PmsSkuInfo getSkuById(String skuId, String ip) {

        System.out.println("ip为" + ip + Thread.currentThread().getName() + "进入");
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        Jedis jedis = redisUtil.getJedis();

        String skuKey = "sku:" + skuId + ":info";
        String skuJson = jedis.get(skuKey);
        if (StringUtils.isNoneBlank(skuJson)) {
            System.out.println("ip为" + ip + Thread.currentThread().getName() + "从redistribution中获取");
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);

        } else {
            System.out.println("ip为" + ip + Thread.currentThread().getName() + "发现缓存没有，申请一个锁：" + "sku:" + skuId + ":info");
            //设置分布式锁
            String token = UUID.randomUUID().toString();
            String isOK = jedis.set("sku:" + skuId + ":token", "1", "nx", "px", 10 * 1000);

            if (StringUtils.isNoneBlank(isOK) && isOK.equals("OK")) {
                //设置成功，能在过期时间内访问数据库
                System.out.println("ip为" + ip + Thread.currentThread().getName() + "申请到了一个锁：" + "sku:" + skuId + ":info");

                pmsSkuInfo = getSkuByIdFromDb(skuId);

                try {
                    Thread.sleep(1000*5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (pmsSkuInfo != null) {
                    jedis.set("sku:" + skuId + ":info", JSON.toJSONString(pmsSkuInfo));
                } else {
                    //为防止缓存穿透，将null或空字符串设置给redis
                    jedis.setex("sku:" + skuId + ":info", 60 * 3, JSON.toJSONString(""));
                }



                //访问完成后释放lock

                System.out.println("ip为" + ip + Thread.currentThread().getName() + "访问完，归还锁");
                String lockToken = jedis.get("sku:"+skuId+":lock");
                if(StringUtils.isNoneBlank(lockToken)&&lockToken.equals(token)){

                    jedis.del("sku:" + skuId + ":lock");

                }

            } else {
                System.out.println("ip为" + ip + Thread.currentThread().getName() + "没有申请到锁，开始自旋");

                //设置失败，自旋（该线程睡眠后重新尝试访问）
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return getSkuById(skuId, ip);
            }


        }


        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {

        List<PmsSkuInfo> PmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return PmsSkuInfos;
    }
}
