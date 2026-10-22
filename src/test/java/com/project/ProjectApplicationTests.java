package com.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "debug=false", "logging.level.root=WARN", "logging.level.org.hibernate.SQL=WARN",
    "app.conditions.publication-active=false",
    "spring.datasource.url=jdbc:h2:mem:smoke;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never", "spring.jpa.show-sql=false"
})
class ProjectApplicationTests {

	@Test
	void contextLoads() {
	}

}
