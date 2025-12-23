package com.swu.bookkeeping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling //启动定时任务
public class BookkeepingApplication {

	public static void main(String[] args) {

		SpringApplication.run(BookkeepingApplication.class, args);
        System.out.println("启动成功");

	}

}       
