package com.example.simuuser.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

@Configuration
public class AnalysisResultSchemaMigration {

    @Bean
    public ApplicationRunner widenAnalysisResultColumns(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return args -> {
            if (!isMysqlCompatible(dataSource)) {
                return;
            }

            List<String> statements = List.of(
                    "ALTER TABLE market_analysis_results MODIFY COLUMN result_json LONGTEXT NOT NULL",
                    "ALTER TABLE cost_analysis_results MODIFY COLUMN revenue_models LONGTEXT NOT NULL",
                    "ALTER TABLE cost_analysis_results MODIFY COLUMN form_json LONGTEXT NOT NULL",
                    "ALTER TABLE cost_analysis_results MODIFY COLUMN result_json LONGTEXT NOT NULL",
                    "ALTER TABLE feedback_analysis_results MODIFY COLUMN source_content LONGTEXT NULL",
                    "ALTER TABLE feedback_analysis_results MODIFY COLUMN result_json LONGTEXT NOT NULL",
                    "ALTER TABLE ai_simulation_results MODIFY COLUMN overall_reaction LONGTEXT NULL",
                    "ALTER TABLE ai_simulation_results MODIFY COLUMN result_json LONGTEXT NOT NULL",
                    "ALTER TABLE scenario_comparison_results MODIFY COLUMN result_json LONGTEXT NOT NULL",
                    "ALTER TABLE documents MODIFY COLUMN content LONGTEXT NULL",
                    "ALTER TABLE documents MODIFY COLUMN description LONGTEXT NULL"
            );

            for (String statement : statements) {
                try {
                    jdbcTemplate.execute(statement);
                } catch (Exception ignored) {
                    // Some deployments may not have every analysis table yet; Hibernate can create them later.
                }
            }
        };
    }

    private boolean isMysqlCompatible(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            return productName != null
                    && (productName.toLowerCase().contains("mysql")
                    || productName.toLowerCase().contains("mariadb"));
        } catch (Exception ignored) {
            return false;
        }
    }
}
