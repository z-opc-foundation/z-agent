package com.zifang.z.agent.center.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.zifang.z.agent.center",
        // FEATURE013 T3/T4: 让 z-agent-engine 的 MysqlAgentTracer / MysqlAgentMetrics /
        // AgentTraceController 被主启动器扫描到
        "com.zifang.z.agent.engine"
})
@MapperScan(basePackages = {"com.zifang.z.agent.center"})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
