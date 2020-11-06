package com.ronin.dw_publisher.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author ronin
 * @create 2020-11-06 11:07
 */

public interface PublisherService {
    public Integer getDauTotal(String date);
    public Map getDauTotalHourMap(String date);
}
