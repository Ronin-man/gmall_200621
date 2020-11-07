package com.ronin.handler

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.ronin.bean.OrderInfo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.{DStream, InputDStream}

/**
 * @author ronin
 * @create 2020-11-06 21:17
 */
object OrderHandler {
  val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
  def kafkaToOrderInfo(kafkaDStream: InputDStream[ConsumerRecord[String, String]]) = {
    kafkaDStream.map(record => {
      //获取kafka中的数据
      val value: String = record.value()
      //将从kafka中获取的数据(Json格式的字符串),解析成样例类
      val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
      //获取创建时间,将创建时间格式化,获取日期和小时,
      val create_time: String = orderInfo.create_time
      val dateAndHour: String = sdf.format(new Date(create_time))
      val dateAndHourArr: Array[String] = dateAndHour.split(" ")
      //将日期时间 和 小时两个字段添加到样例类中
      orderInfo.create_date = dateAndHourArr(0)
      orderInfo.create_hour = dateAndHourArr(1)
      //收件人 电话 脱敏
      //将收件人电话切割,去后四位,前七位都为*******
      orderInfo.consignee_tel= "*******" + orderInfo.consignee_tel.splitAt(7)._2
      //返回数据
      orderInfo
    })
  }

}
