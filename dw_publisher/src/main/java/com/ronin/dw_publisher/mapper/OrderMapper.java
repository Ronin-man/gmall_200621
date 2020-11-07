package com.ronin.dw_publisher.mapper;

import java.util.List;
import java.util.Map;

/**
 * @author ronin
 * @create 2020-11-07 0:07
 */
public interface OrderMapper {
    //1 查询当日交易额总数
    public Double selectOrderAmountTotal(String date);
    // 查询当日每小时的交易额
    public List<Map> selectOrderAmountHourMap(String date);
}
