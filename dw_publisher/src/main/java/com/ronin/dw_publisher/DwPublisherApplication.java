package com.ronin.dw_publisher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//扫描com.ronin.dw_publisher.mapper下的所有代码
//controller 先调用service ,service调用mapper
@MapperScan(basePackages = "com.ronin.dw_publisher.mapper")
public class DwPublisherApplication {

	public static void main(String[] args) {
		SpringApplication.run(DwPublisherApplication.class, args);
	}

}
