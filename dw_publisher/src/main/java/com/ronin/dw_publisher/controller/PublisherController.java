package com.ronin.dw_publisher.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ronin.dw_publisher.service.PublisherService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    public String getDauTotal(@RequestParam("date") String date) {
        //从service端获取dauTotal数据
        //1.获取新增日活
        Integer dauTotal = publisherService.getDauTotal(date);
        //2.封装新增日活的Map
        HashMap<String, Object> dauTotalMap = new HashMap<>();
        dauTotalMap.put("id", "dau");
        dauTotalMap.put("name", "新增日活");
        dauTotalMap.put("value", dauTotal);
        //3.封装新增设备的Map
        HashMap<String, Object> newMidMap = new HashMap<>();
        newMidMap.put("id", "new_mid");
        newMidMap.put("name", "新增设备");
        newMidMap.put("value", 233);
        //4.获取当日总交易额
        Double orderAmountTotal = publisherService.getOrderAmountTotal(date);
        //创建封装当日总交易额的Map
        HashMap<String, Object> orderAmountTotalAmountMap = new HashMap<>();
        orderAmountTotalAmountMap.put("id", "order_amount");
        orderAmountTotalAmountMap.put("name", "新增交易额");
        orderAmountTotalAmountMap.put("value", orderAmountTotal);
        //List<Map>保存数据
        ArrayList<Map> list = new ArrayList<>();
        list.add(dauTotalMap);
        list.add(newMidMap);
        list.add(orderAmountTotalAmountMap);
        //将result转换为字符串输出
        return JSON.toJSONString(list);
    }

    @PostMapping("realtime-hours")
    public String getDauTotalHour(@RequestParam("id") String id, @RequestParam("date") String date) {
        String yesterday = LocalDate.parse(date).plusDays(-1).toString();
        if ("dau".equals(id)) {
            //1.获取当天的日活分时数据
            Map todayMap = publisherService.getDauTotalHourMap(date);
            //2.获取昨天的日活分时数据
            Map yesterdayMap = publisherService.getDauTotalHourMap(yesterday);
            //3.创建Map用于存放结果数据
            //{"yesterday":{"11":383,"12":123,"17":88,"19":200 },
            //"today":{"12":38,"13":1233,"17":123,"19":688 }}
            //HashMap<String, Map>  String 为today和yesterday 后面为map
            HashMap<String, Map> result = new HashMap<>();
            //4.将两个map放入result中
            result.put("yesterday", yesterdayMap);
            result.put("today", todayMap);
            //5.返回结果,转换为json字符串
            return JSON.toJSONString(result);
        } else if ("order_amount".equals(id)) {
            //获取今天每小时的销售额
            Map todayOrderAmountHourMap = publisherService.getOrderAmountHourMap(date);
            //获取昨天每小时的销售额
            Map yesterdayOrderAmountHourMap = publisherService.getOrderAmountHourMap(yesterday);
            //封装一个Map保存两天的数据
            HashMap<String, Map> result = new HashMap<>();
            result.put("yesterday", yesterdayOrderAmountHourMap);
            result.put("today", todayOrderAmountHourMap);
            //返回结果
            return JSON.toJSONString(result);
        }
        return null;
    }

}
