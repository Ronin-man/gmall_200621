package com.ronin.app;


import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.atguigu.Constants.GmallConstants;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ronin.utils.MyKafkaSender;


import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author ronin
 * @create 2020-11-06 16:57
 */
public class CanalClient {
    public static void main(String[] args) {
        //获取Canal连接对象
        CanalConnector canalConnector =  CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop102", 11111),
                "example",
                "",
                "");

        //抓取数据并且解析message,
        while (true){
            //获取连接
            canalConnector.connect();
            //指定消费的数据表
            canalConnector.subscribe("gmall2020.*");
            //抓取数据.参数100,是最大抓取100个,没有100,时间到达了,也会抓取
            Message message = canalConnector.get(100);
            //判断message是否为空,如果为空就不解析,message里面有多行数据,是个集合,所以需要遍历
            if (message.getEntries().size()<=0){
                System.out.println("当前没有数据！休息一会！");
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else {
                //message不为空,获取message中的entries
                List<CanalEntry.Entry> entries = message.getEntries();
                //遍历entries
                for (CanalEntry.Entry entry : entries) {
                    //获取entry的类型
                    CanalEntry.EntryType entryType = entry.getEntryType();
                    //判断entry的类型是否为ROWDATA,只需要解析该类型的
                    if(CanalEntry.EntryType.ROWDATA.equals(entryType)){
                        //获取表名
                        String tableName = entry.getHeader().getTableName();
                        //获取序列化数据
                        ByteString storeValue = entry.getStoreValue();
                        try {
                            //反序列化数据
                            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                            //获取事件(event)类型
                            CanalEntry.EventType eventType = rowChange.getEventType();
                            //获取rowDatasList
                            List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                            //通过tableName,event类型判断,所需要的类型,遍历rowDatasList,获取数据
                            //封装方法
                            handler(tableName, eventType, rowDatasList);
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 通过tableName,event类型判断,所需要的类型,遍历rowDatasList,获取数据
     * @param tableName 表名
     * @param eventType 操作类型
     * @param rowDatasList 每行的数据
     */
    private static void handler(String tableName, CanalEntry.EventType eventType, List<CanalEntry.RowData> rowDatasList) {
        //处理订单表,对订单表而言,计算GMV,只需要每日新增操作数据,即INSERT类型
        if ("order_info".equals(tableName)&& CanalEntry.EventType.INSERT.equals(eventType)){
            //遍历rowDatasList
            for (CanalEntry.RowData rowData : rowDatasList) {
                //封装一个json对象保存数据,用于存放多个列的数据
                JSONObject jsonObject = new JSONObject();
                //由于有多个列,则遍历多个列,
                for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                    //获取类名,获取值,封装到json对象中
                    jsonObject.put(column.getName(),column.getValue());
                }
                System.out.println(jsonObject.toString());
                //发送数据到kafka中
                MyKafkaSender.send(GmallConstants.KAFKA_GMALL_ORDER_INFO,jsonObject.toString());
            }
        }
    }
}
