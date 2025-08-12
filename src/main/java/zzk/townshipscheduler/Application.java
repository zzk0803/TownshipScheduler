package zzk.townshipscheduler;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import zzk.townshipscheduler.backend.dao.AppUserEntityRepository;
import zzk.townshipscheduler.backend.dao.PlayerEntityRepository;
import zzk.townshipscheduler.backend.dao.WarehouseEntityRepository;
import zzk.townshipscheduler.backend.persistence.AccountEntity;
import zzk.townshipscheduler.backend.persistence.PlayerEntity;
import zzk.townshipscheduler.backend.persistence.WarehouseEntity;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableAsync
@EnableRetry
@EnableScheduling
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationRunner applicationRunner(
            AppUserEntityRepository appUserEntityRepository,
            PlayerEntityRepository playerEntityRepository,
            WarehouseEntityRepository warehouseEntityRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate
    ) {

        return (applicationArguments) -> {
            boolean test = appUserEntityRepository.existsAppUserEntitiesByUsername("test");
            if (!test) {
                transactionTemplate.executeWithoutResult(transactionStatus -> {
                    AccountEntity accountEntity = new AccountEntity();
                    accountEntity.setName("test");
                    accountEntity.setUsername("test");
                    accountEntity.setHashedPassword(passwordEncoder.encode("test"));

                    PlayerEntity playerEntity = new PlayerEntity();
                    accountEntity.attachePlayerEntity(playerEntity);

                    WarehouseEntity warehouseEntity = new WarehouseEntity();
                    playerEntity.attacheWarehouseEntity(warehouseEntity);

                    appUserEntityRepository.save(accountEntity);
                    playerEntityRepository.save(playerEntity);
                    warehouseEntityRepository.save(warehouseEntity);
                });

            }
        };
    }

    @Bean("townshipExecutorService")
    public ExecutorService townshipExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplateBuilder retryTemplateBuilder = new RetryTemplateBuilder();

        return retryTemplateBuilder.fixedBackoff(Duration.ofSeconds(3))
                .maxAttempts(3)
                .retryOn(Exception.class)
                .build();
    }

    @Bean("townshipTaskScheduler")
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskSchedulerBuilder taskSchedulerBuilder = new SimpleAsyncTaskSchedulerBuilder();
        return taskSchedulerBuilder
                .virtualThreads(true)
                .build();
    }

}
