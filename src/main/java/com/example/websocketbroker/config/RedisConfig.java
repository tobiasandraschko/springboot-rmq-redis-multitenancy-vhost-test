package com.example.websocketbroker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration class for setting up Redis-related beans in the Spring context.
 * This class defines the necessary beans for connecting to a Redis server
 * and serializing data for storage and retrieval.
 *
 * @author Tobias Andraschko
 */
@Configuration
public class RedisConfig {

  /**
   * Creates and configures an ObjectMapper bean for JSON serialization and
   * deserialization.
   *
   * @return a configured ObjectMapper instance
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  @Value("${spring.redis.host}")
  private String redisHost;

  @Value("${spring.redis.port}")
  private int redisPort;

  @Value("${spring.redis.password}")
  private String redisPassword;

  /**
   * Configures a LettuceConnectionFactory bean for connecting to a Redis
   * server. The connection is set up with the hostname, port, and password
   * retrieved from the application properties.
   *
   * @return a LettuceConnectionFactory configured for Redis
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(redisHost);
    config.setPort(redisPort);
    config.setPassword(redisPassword);
    return new LettuceConnectionFactory(config);
  }

  /**
   * Creates a RedisTemplate bean for performing operations on Redis. This
   * template is configured to use the provided LettuceConnectionFactory
   * and uses StringRedisSerializer for both keys and values.
   *
   * @param connectionFactory the LettuceConnectionFactory to be used by the
   *                         RedisTemplate
   * @return a configured RedisTemplate instance
   */
  @Bean
  public RedisTemplate<String, String> redisTemplate(
    LettuceConnectionFactory connectionFactory
  ) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    return template;
  }
}
