package com.mcpgateway.config;

import com.mcpgateway.model.*;
import com.mcpgateway.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeData(
            OrganizationRepository orgRepo,
            UserRepository userRepo,
            McpServerRepository serverRepo,
            ToolSchemaRepository toolSchemaRepo,
            PasswordEncoder passwordEncoder) {
        
        return args -> {
            // Create sample organizations
            if (orgRepo.count() == 0) {
                Organization org1 = new Organization();
                org1.setName("Acme Corporation");
                orgRepo.save(org1);

                Organization org2 = new Organization();
                org2.setName("TechStart Inc");
                orgRepo.save(org2);

                Organization org3 = new Organization();
                org3.setName("Global Enterprises");
                orgRepo.save(org3);
            }

            // Create sample users with passwords and roles
            if (userRepo.count() == 0) {
                // Admin user - full platform access
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setOrganizationId(1L);
                admin.setRoles(Set.of(Role.ADMIN));
                userRepo.save(admin);

                // Org Admin user - manages organization 1
                User orgAdmin = new User();
                orgAdmin.setUsername("orgadmin");
                orgAdmin.setPasswordHash(passwordEncoder.encode("orgadmin123"));
                orgAdmin.setOrganizationId(1L);
                orgAdmin.setRoles(Set.of(Role.ORG_ADMIN));
                userRepo.save(orgAdmin);

                // Regular users
                User user1 = new User();
                user1.setUsername("john.doe");
                user1.setPasswordHash(passwordEncoder.encode("password123"));
                user1.setOrganizationId(1L);
                user1.setRoles(Set.of(Role.USER));
                userRepo.save(user1);

                User user2 = new User();
                user2.setUsername("jane.smith");
                user2.setPasswordHash(passwordEncoder.encode("password123"));
                user2.setOrganizationId(1L);
                user2.setRoles(Set.of(Role.USER));
                userRepo.save(user2);

                User user3 = new User();
                user3.setUsername("bob.wilson");
                user3.setPasswordHash(passwordEncoder.encode("password123"));
                user3.setOrganizationId(2L);
                user3.setRoles(Set.of(Role.USER));
                userRepo.save(user3);
            }

            // Create sample MCP servers
            if (serverRepo.count() == 0) {
                McpServer server1 = new McpServer();
                server1.setName("Primary MCP Server");
                server1.setBaseUrl("http://localhost:3000");
                server1.setOrganizationId(1L);
                serverRepo.save(server1);

                McpServer server2 = new McpServer();
                server2.setName("Backup MCP Server");
                server2.setBaseUrl("http://localhost:3001");
                server2.setOrganizationId(1L);
                serverRepo.save(server2);
            }

            // Create sample tool schemas
            if (toolSchemaRepo.count() == 0) {
                ToolSchema schema1 = new ToolSchema(
                    "translate",
                    "Translator Tool",
                    "Translate text between languages",
                    "{\"title\":\"Translate\",\"fields\":[{\"name\":\"text\",\"type\":\"textarea\",\"label\":\"Text to translate\"},{\"name\":\"targetLang\",\"type\":\"text\",\"label\":\"Target Language\"}]}"
                );
                schema1.setOrganizationId(null); // Global schema
                toolSchemaRepo.save(schema1);

                ToolSchema schema2 = new ToolSchema(
                    "summarize",
                    "Summarizer Tool",
                    "Summarize long text content",
                    "{\"title\":\"Summarize\",\"fields\":[{\"name\":\"text\",\"type\":\"textarea\",\"label\":\"Text to summarize\"},{\"name\":\"length\",\"type\":\"text\",\"label\":\"Summary length (short/medium/long)\"}]}"
                );
                schema2.setOrganizationId(null); // Global schema
                toolSchemaRepo.save(schema2);

                ToolSchema schema3 = new ToolSchema(
                    "sentiment",
                    "Sentiment Analysis",
                    "Analyze sentiment of text",
                    "{\"title\":\"Sentiment Analysis\",\"fields\":[{\"name\":\"text\",\"type\":\"textarea\",\"label\":\"Text to analyze\"}]}"
                );
                schema3.setOrganizationId(null); // Global schema
                toolSchemaRepo.save(schema3);
            }
        };
    }
}

