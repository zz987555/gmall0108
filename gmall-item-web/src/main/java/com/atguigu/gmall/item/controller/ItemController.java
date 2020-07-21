package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;


    @Reference
    SpuService spuService;
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap map, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
//        request.getHeader("");//nginx负载均衡查ip


        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId, remoteAddr);
//sku对象
        map.put("skuInfo",pmsSkuInfo);
//销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        map.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        HashMap<String, String> skuSaleAttrHash = new HashMap<>();

        List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());

        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k ="";
            String v = skuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValuesList=skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValuesList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId()+"|";
            }

            skuSaleAttrHash.put(k,v);

        }

        String skuSaleAttrHashJsonStr = JSON.toJSONString(skuSaleAttrHash);
        map.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);

        return "item";
    }



    @RequestMapping("index")
    public String index(ModelMap modelMap) {

        List<String> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add("LoopData" + i);
        }
        modelMap.put("list", list);
        modelMap.put("hello", "hello thymeleaf !!");

        modelMap.put("check", "1");
        return "index";
    }

}
