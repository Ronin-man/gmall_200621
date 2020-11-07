package com.ronin.dw_publisher.mapper;

import java.util.List;
import java.util.Map;

/**
 * @author ronin
 * @create 2020-11-06 10:11
 */
public interface DauMapper {
    /*
     <select id="selectDauTotal" resultType="Integer">
        select count(*) from GMALL2020_DAU where  LOGDATE=#{date}
    </select>
     */
    //与DauMapper.xml文件的sql,要一一对应,方法名与id一致,返回值与resultType对应,参数与#{date}中的date一致
    //返回的是一个行一列的数据,所以可以用单个类型
    //查询当天新增日活
    public Integer selectDauTotal(String date);

    //因为返回的是多行数据,用List<Map>接收数据
    //查询当天每小时新增日活
    public List<Map> selectDauTotalHourMap (String date);

}
