package com.example.simuuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 아래 두 줄은 DB 없이 실행하기 위해 임시로 제외한 자동 구성입니다.
// 나중에 DB 연결이 필요하면 삭제하거나 주석 처리하면 됩니다.
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(
		// DB 없이 실행하기 위해 DataSource 및 JPA 자동 구성을 제외
		// 나중에 DB 연결이 필요하면 아래 exclude를 제거하면 됩니다.
		exclude = {
				DataSourceAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class
		})
public class SimuuserApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimuuserApplication.class, args);
	}

}
