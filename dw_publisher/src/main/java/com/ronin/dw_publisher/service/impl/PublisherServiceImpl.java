package com.ronin.dw_publisher.service.impl;

import com.ronin.dw_publisher.mapper.DauMapper;
import com.ronin.dw_publisher.mapper.OrderMapper;
import com.ronin.dw_publisher.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ronin
 * @create 2020-11-06 11:07
 */
@Service
public class PublisherServiceImpl  implements PublisherService {
    //自动注入,会自动建立对象
    @Autowired
    DauMapper dauMapper;

    @Override
    public Integer getDauTotal(String date) {
        return dauMapper.selectDauTotal(date);
    }

    @Override
    public Map getDauTotalHourMap(String date) {
        List<Map> dauHourList = dauMapper.selectDauTotalHourMap(date);
        //用一个Map保存数据
        HashMap<String, Long> dauHourMap = new HashMap<String,Long>();
        for (Map map : dauHourList) {
            dauHourMap.put(map.get("LH").toString(),(Long) map.get("CT"));
        }
        return dauHourMap;
    }
    @Autowired
    OrderMapper orderMapper;
    @Override
    public Double getOrderAmountTotal(String date) {
        return orderMapper.selectOrderAmountTotal(date);
    }

    @Override
    public Map getOrderAmountHourMap(String date) {
        List<Map> orderAmountHourList = orderMapper.selectOrderAmountHourMap(date);
        //用一个Map保存数据
        HashMap<String, Long> orderAmountHourMap = new HashMap<>();
        for (Map map : orderAmountHourList) {
            orderAmountHourMap.put((String) map.get("CREATE_HOUR"),(Long) map.get("SUM_AMOUNT"));
        }
        return orderAmountHourMap;
    }
}
