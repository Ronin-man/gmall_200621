package com.ronin.dw_publisher.controller;

import com.alibaba.fastjson.JSON;
import com.ronin.dw_publisher.service.PublisherService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ronin
 * @create 2020-11-06 11:36
 */
@RestController
public class PublisherController {
    @Autowired
    PublisherService publisherService;
    @PostMapping("realtime-total")
    public String getDauTotal(@RequestParam("date") String date){
        //从service端获取dauTotal数据
        Integer dauTotal = publisherService.getDauTotal(date);
        // 用Map保存数据
        HashMap<String, Object> dauTotalMap = new HashMap<>();
        dauTotalMap.put("id","dau");
        dauTotalMap.put("name","新增日活");
        dauTotalMap.put("value",dauTotal);
        HashMap<String, Object> newMidMap = new HashMap<>();
        newMidMap.put("id","new_mid");
        newMidMap.put("name","新增设备");
        newMidMap.put("value",233);
        //List<Map>保存数据
        ArrayList<Map> list= new ArrayList<>();
        list.add(dauTotalMap);
        list.add(newMidMap);
        return JSON.toJSONString(list);
    }
}
