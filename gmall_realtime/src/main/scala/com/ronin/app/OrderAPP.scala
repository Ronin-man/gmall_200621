package com.ronin.app


import com.ronin.bean.OrderInfo
import com.ronin.handler.OrderHandler
import com.ronin.utils.MyKafkaUtil
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.phoenix.spark._

/**
 * @author ronin
 * @create 2020-11-06 20:32
 */
object OrderAPP {
  def main(args: Array[String]): Unit = {
    //1 创建SparkConf
    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("OrderAPP")

    //2 创建StreamingContext
    val ssc = new StreamingContext(sparkConf, Seconds(5))
    //3 消费kafka数据,创建kafkaDStream
    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream("GMALL_ORDER_INFO", ssc)
    //4 封装成样例类
    val orderInfoDStream: DStream[OrderInfo] = OrderHandler.kafkaToOrderInfo(kafkaDStream)
    //将数据写入HBase(phoneix)中
    orderInfoDStream.foreachRDD(rdd => {
      rdd.saveToPhoenix("GMALL2020_ORDER_INFO",
        Seq("ID",
      "PROVINCE_ID",
      "CONSIGNEE",
      "ORDER_COMMENT",
      "CONSIGNEE_TEL",
      "ORDER_STATUS",
      "PAYMENT_WAY",
      "USER_ID",
      "IMG_URL",
      "TOTAL_AMOUNT",
      "EXPIRE_TIME",
      "DELIVERY_ADDRESS",
      "CREATE_TIME",
      "OPERATE_TIME",
      "TRACKING_NO",
      "PARENT_ORDER_ID",
      "OUT_TRADE_NO",
      "TRADE_BODY",
      "CREATE_DATE",
      "CREATE_HOUR"),
        HBaseConfiguration.create(),
        Some("hadoop102,hadoop103,hadoop104:2181"))
    })

    //6 启动
    ssc.start()
    ssc.awaitTermination()
  }

}
