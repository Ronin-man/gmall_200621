package com.ronin.dw_publisher.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author ronin
 * @create 2020-11-06 11:07
 */

public interface PublisherService {
    //查询当日新增日活
    public Integer getDauTotal(String date);
    //查询当日每小时的新增日活
    public Map getDauTotalHourMap(String date);
    // 查询当日交易额总数
    public Double getOrderAmountTotal(String date);
    //查询当日每小时的交易额
    public Map getOrderAmountHourMap(String date);
}
