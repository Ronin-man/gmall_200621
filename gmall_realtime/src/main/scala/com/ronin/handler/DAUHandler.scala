package com.ronin.handler

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.alibaba.fastjson.JSON
import com.ronin.bean.StartUpLog
import com.ronin.utils.RedisUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import redis.clients.jedis.Jedis

/**
 * @author ronin
 * @create 2020-11-05 15:47
 */
object DAUHandler {
  /**
   * 将每一行数据转换为样例类对象,并补充时间字段
   * @param kafkaDStream 从kafka中获取的原始数据
   * @return
   */
  def kafkaToStartLog(kafkaDStream: InputDStream[ConsumerRecord[String, String]]) = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
    kafkaDStream.map(record => {
      //a.获取value
      val value: String = record.value()
      //b.将json格式的数据解析成样例类,取出时间戳字段
      val startUpLog: StartUpLog = JSON.parseObject(value, classOf[StartUpLog])
      val ts: Long = startUpLog.ts
      //c.格式化时间戳,获取时间和分钟
      val dateHourStr: String = sdf.format(new Date(ts))
      val dateAndHourArr: Array[String] = dateHourStr.split(" ")
      //d. 给时间字段重新赋值
      startUpLog.logDate = dateAndHourArr(0)
      startUpLog.logHour = dateAndHourArr(1)
      //e. 返回数据
      startUpLog
    })
  }

  /**
   * 通过mid同批次去重,转换结构,按照mid分组,按照时间排序,取第一条数据,达到去重的目的
   * KV结构数据:   是否需要Key,数据是否需要压平
   * map            不需要Key,不需要压平
   * mapValues        需要Key,不需要压平
   * flatMap        不需要Key,需要压平
   * flatMapValues    需要Key,需要压平
   *
   * @param filterByRedisDStream 通过redis过滤,去重后的数据
   * @return
   */
  def filterByMid(filterByRedisDStream: DStream[StartUpLog]) = {
    //转换数据结构 =>(mid_logDate,log)
    val midDateToLogDStream: DStream[((String, String), StartUpLog)] = filterByRedisDStream.map(log => {
      ((log.mid, log.logDate), log)
    })
    //按照mid和时间分组
    val midDateToLogIterDStream: DStream[((String, String), Iterable[StartUpLog])] = midDateToLogDStream.groupByKey()
    //按照时间排序取第一条数据
    midDateToLogIterDStream.flatMap {
      case ((mid, logDate), logIter) => {
        val logs: List[StartUpLog] = logIter.toList.sortBy(_.logDate).take(1)
        logs
      }
    }
  }

  private val sdf = new SimpleDateFormat("yyyy-MM-DD")

  /**
   * 通过redis跨批次去重
   *
   * @param startUpLogDStream 封装成样例类数据(原始数据)
   */

  def filterByRedis(startUpLogDStream: DStream[StartUpLog], sc: SparkContext) = {
    //方案一：使用filter单条数据过滤
    //    val value: DStream[StartUpLog] = startUpLogDStream.filter(log => {
    //      //a.获取连接
    //      val jedisClient: Jedis = RedisUtil.getJedisClient
    //      //b.业务处理,判断是否已经存在在redis中了
    //      //获取redisKey
    //      val redisKey = s"DAU:${sdf.format(System.currentTimeMillis())}"
    //      //判断mid是否存在 jedisClient.sismember(),存在就过滤掉,不存在就往后.所以取反
    //      val boolean: lang.Boolean = jedisClient.sismember(redisKey, log.mid)
    //      //c 归还连接
    //      jedisClient.close()
    //      !boolean
    //    })
    //    value
    //
    //    //方案二:使用transform算子 转换为RDD,一个分区一个处理,一分区获取一次连接,多少个分区就连接多少次redis,但是每条数据都要访问一次redis
    //    val value1: DStream[StartUpLog] = startUpLogDStream.transform(rdd => {
    //        rdd.mapPartitions(iter => {
    //            //a.获取连接
    //            val jedisClient: Jedis = RedisUtil.getJedisClient
    //            //b.业务处理
    //            val logs: Iterator[StartUpLog] = iter.filter(log => {
    //              //b.业务处理,判断是否已经存在在redis中了
    //              //获取redisKey
    //              val redisKey = s"DAU:${log.logDate}"
    //              //判断mid是否存在 jedisClient.sismember(),存在就过滤掉,不存在就往后.所以取反
    //              !jedisClient.sismember(redisKey, log.mid)
    //            })
    //            //c.归还连接
    //            jedisClient.close()
    //            //返回数据
    //            logs
    //          })
    //      })
    //    value1
    //方案三 :通过广播变量,在driver端中,提前将redis中数据mid给获取出来,发给每个executor端,
    //每个批次只获取一次连接,
    //每个批次都执行一次,将数据写入redis中,transform 算子与rdd之间的代码在driver端执行,每次都执行一次
    //获取数据量的时候 就用第二种方案  广播变量, 百万日活mid(20M)左右
    val value2: DStream[StartUpLog] = startUpLogDStream.transform(rdd => {
      //获取连接
      val jedisClient: Jedis = RedisUtil.getJedisClient
      //获取redis中的mid
      val MidSet: util.Set[String] = jedisClient.smembers(s"DAU:${sdf.format(new Date(System.currentTimeMillis()))}")
      //归还连接
      jedisClient.close()
      //封装成广播变量
      val bc: Broadcast[util.Set[String]] = sc.broadcast(MidSet)
      //过滤mid
      rdd.filter(log => {
        !bc.value.contains(log.mid)
      })
    })
    value2
  }

  /**
   * 将去重之后的数据中的MID写入redis(为了后续的批次去重)
   * @param filterByMidDStream 通过两次去重后的数据
   */
  def saveMidToRedis(filterByMidDStream: DStream[StartUpLog]) = {
    filterByMidDStream.foreachRDD(
      rdd => {
        rdd.foreachPartition(
          iter => {
            //获取Redis连接
            val jedisClient: Jedis = RedisUtil.getJedisClient
            //业务处理.遍历写入
            iter.foreach(
              log => {
                //获取redisKey,设计redisKey DAU:logDate
                val redisKey = s"DAU:${log.logDate}"
                jedisClient.sadd(redisKey, log.mid)
              }
            )
            //归还连接
            jedisClient.close()
          }
        )
      }
    )
  }
}
