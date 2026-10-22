package com.zifang.z.agent.center.starter.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zifang.z.boot.datasource.starter.ModuleDataSourceTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * z-agent-center <b>独立部署</b>的数据源 + SqlSessionFactory 装配。
 *
 * <p><b>为什么必须有这个类（2026-10-22 修）</b>：z-agent-center 拆出来独立部署后，
 * 容器启动即失败，日志根因是
 * <pre>
 *   Field instanceMapper in com.zifang.z.agent.center.core.skill.auth.AgentAuthInterceptor
 *     required a bean named 'sqlSessionFactoryConfig' that could not be found.
 * </pre>
 * 那个 bean 一直由 <b>z-team-starter 的 {@code TeamSqlSessionFactoryConfig}</b> 提供
 * （它复用 {@code @Primary} dataSource，并额外暴露名为 {@code sqlSessionFactoryConfig}
 * 的别名）。z-agent-center 独立进程里没有 z-team-starter ⇒ 这个 bean 无人提供 ⇒ 启动必崩。
 *
 * <p>日志里<b>完全没有 DataSource / Hikari / MyBatis 数据源装配痕迹</b>，可反证不是"连不上库"
 * 而是"根本没装配数据源"：{@code z-boot-datasource-starter} 只提供
 * {@code ModuleDataSourceTemplate} 等<b>辅助类</b>，<b>不带 spring.factories</b>，
 * 不会自动创建任何 DataSource —— 每个独立服务必须自持一份装配。
 *
 * <p><b>本类只存在于 starter 模块</b>（不是 core/web），因此<b>不会</b>被主壳 z-opc 捡到：
 * 主壳只依赖 z-agent-center-core / -web，不依赖 center-starter（实测线上主壳 fat jar 的
 * BOOT-INF/lib 里没有 z-agent-center-starter）。这样主壳继续用 z-team-starter 的
 * {@code TeamSqlSessionFactoryConfig}，两边<b>不会</b>出现第二个 DataSource。
 *
 * <p><b>与主壳的语义必须对齐</b>：独立实例与主壳内嵌副本读写<b>同一张表</b>
 * （同一个 RDS {@code oc} 库），所以逻辑删除、下划线转驼峰、分页这些
 * 会改变 SQL 语义的开关必须一致，否则两个运行时会用不同语义改同一批数据：
 * <ul>
 *   <li>逻辑删除：{@code deleted} 字段 / 删除值 1 / 未删值 0（对齐 z-team-starter
 *       {@code application.yaml} 的 {@code mybatis-plus.global-config.db-config}）</li>
 *   <li>下划线转驼峰：{@code map-underscore-to-camel-case=true}</li>
 *   <li>分页：必须显式注册 {@code PaginationInnerInterceptor}，
 *       否则 {@code selectPage} 会把全部行塞进一页且 total 恒 0</li>
 * </ul>
 *
 * <p><b>配置键</b>：{@code z.base.db.config.*}（缺省逐项回退 {@code z.base.db.default.*}）。
 * 部署清单里喂的是 {@code Z_BASE_DB_CONFIG_HOST/PORT/DATABASE/USERNAME/PASSWORD}，
 * Spring 的 relaxed binding 会把它们映射到 {@code z.base.db.config.*}。
 * 模块名取 {@code config} 是<b>为了与既有部署清单逐字对齐</b>（env 前缀就是
 * {@code Z_BASE_DB_CONFIG_*}），不是笔误。
 *
 * @author zifang
 * @date 2026/10/22
 */
@Configuration
public class AgentCenterDataSourceConfig extends ModuleDataSourceTemplate {

    /**
     * 主数据源。
     *
     * <p>bean 名固定为 {@code dataSource}：与 z-team-starter 的 {@code PrimaryDataSourceConfig}
     * 同名同义（"这个进程的主库"），也让 {@code @Qualifier("dataSource")} 的注入点两边都成立。
     */
    @Bean(name = "dataSource", destroyMethod = "close")
    @Primary
    public DataSource dataSource(Environment env) {
        return buildDataSource(env, "config");
    }

    /**
     * 主 SqlSessionFactory。
     *
     * <p>bean 名固定为 {@code sqlSessionFactory}：MyBatis-Plus 的 {@code ServiceImpl}
     * 默认按<b>名字</b>找这个 bean；bean 名不是 {@code sqlSessionFactory} 时
     * 所有 {@code ServiceImpl} 子类都会在运行期报
     * {@code Field baseMapper ... required a bean named 'sqlSessionFactory'}。
     */
    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource)
            throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/**/*.xml"));

        // 与主壳一致：列名 x_y 与实体属性 xY 的映射。
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);

        // 与主壳一致：逻辑删除走 deleted 列（值 1 = 已删，0 = 未删）。
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setDbConfig(new GlobalConfig.DbConfig()
                .setLogicDeleteField("deleted")
                .setLogicDeleteValue("1")
                .setLogicNotDeleteValue("0"));
        factoryBean.setGlobalConfig(globalConfig);

        // 分页拦截器必须显式挂到 factory 上：独立进程里没有 MybatisPlusAutoConfiguration
        // 代劳（z-team-starter 是把 MybatisPlusAutoConfiguration exclude 掉后自持装配的）。
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        factoryBean.setPlugins(interceptor);

        return factoryBean.getObject();
    }

    /**
     * z-agent-center 自己的 mapper 绑定到这个别名 bean。
     *
     * <p>必须存在的原因：{@code AgentCenterMapperConfig} 与 {@code BotMapperConfig} 的
     * {@code @MapperScan(sqlSessionFactoryRef = "sqlSessionFactoryConfig")} 是<b>按名字</b>
     * 向宿主索要 bean 的 —— 这正是本次启动失败的直接原因。
     *
     * <p>这里返回<b>同一个</b> SqlSessionFactory 实例（不是新建），保证独立实例与主壳
     * 内嵌副本的 MyBatis 配置（驼峰映射 / 逻辑删除 / 分页）完全同源。
     */
    @Bean(name = "sqlSessionFactoryConfig")
    public SqlSessionFactory sqlSessionFactoryConfig(
            @Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return sqlSessionFactory;
    }
}
