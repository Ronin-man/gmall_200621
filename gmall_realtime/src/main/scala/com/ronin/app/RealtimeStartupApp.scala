package com.ronin.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.atguigu.Constants.GmallConstants
import com.ronin.bean.StartUpLog
import com.ronin.handler.DAUHandler
import com.ronin.utils.MyKafkaUtil
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.phoenix.spark._


/**
 * @author ronin
 * @create 2020-11-04 14:58
 */
object RealtimeStartupApp {
  def main(args: Array[String]): Unit = {
    //1 创建SparkConf
    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("RealtimeStartupApp")
    //2 创建StreamingContext
    val ssc: StreamingContext = new StreamingContext(sparkConf, Seconds(5))
    //3 创建DStream
    val kafkaDStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstants.KAFKA_TOPIC_STARTUP, ssc)
    //4.将每一行数据转换为样例类对象,并补充时间字段
    val startUpLogDStream: DStream[StartUpLog] = DAUHandler.kafkaToStartLog(kafkaDStream)
    //5.通过redis跨批次去重
    val filterByRedisDStream: DStream[StartUpLog] = DAUHandler.filterByRedis(startUpLogDStream, ssc.sparkContext)
    //6.通过mid同批次去重
    val filterByMidDStream: DStream[StartUpLog] = DAUHandler.filterByMid(filterByRedisDStream)
    //7.将去重之后的数据中的MID写入redis(为了后续的批次去重)
    //由于5,6 都是去重,不会对数据结构产生变化,所以先写步骤7
    DAUHandler.saveMidToRedis(filterByMidDStream)
    //8.将两次去重后的数据明细写入hbase中(Phoneix)
    filterByMidDStream.foreachRDD(rdd => {
      //saveToPhoneix是隐式方法,需要手动导入包 import org.apache.phoenix.spark._
      rdd.saveToPhoenix("GMALL2020_DAU",
        Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"),
        HBaseConfiguration.create(),
        Some("hadoop102,hadoop103,hadoop104:2181"))
    })
    //6 启动
    ssc.start()
    ssc.awaitTermination()
  }
}
