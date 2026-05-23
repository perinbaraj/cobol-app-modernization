package com.acme.customer.batch;

import com.acme.customer.batch.listener.BatchJobListener;
import com.acme.customer.batch.listener.StepExecutionListener;
import com.acme.customer.batch.model.CustomerRecord;
import com.acme.customer.batch.model.CustomerStagingRecord;
import com.acme.customer.batch.model.ValidationResult;
import com.acme.customer.batch.processor.CustomerLoadProcessor;
import com.acme.customer.batch.processor.CustomerValidationProcessor;
import com.acme.customer.batch.processor.CustomerReportProcessor;
import com.acme.customer.batch.reader.StagingTableReader;
import com.acme.customer.batch.reader.CustomerTableReader;
import com.acme.customer.batch.writer.CustomerTableWriter;
import com.acme.customer.batch.writer.ErrorFileWriter;
import com.acme.customer.batch.writer.ReportWriter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch Configuration for Customer Batch Processing.
 * 
 * <p>Converted from: CUSTBAT.jcl</p>
 * 
 * <p>This configuration defines a 3-step batch job that mirrors the original
 * mainframe JCL job structure:</p>
 * <ul>
 *   <li>STEP1 (CUSTLOAD): Load daily customer updates from staging</li>
 *   <li>STEP2 (CUSTVAL): Validate customer data integrity</li>
 *   <li>STEP3 (CUSTRPT): Generate daily customer report</li>
 * </ul>
 * 
 * <p>JCL COND code behavior is implemented using Spring Batch flow decisions
 * and step execution listeners.</p>
 *
 * @see <a href="samples/jcl/CUSTBAT.jcl">Original JCL</a>
 */
@Configuration
@EnableBatchProcessing
public class CustomerBatchConfig {

    /** Chunk size for batch processing - tuned for Azure SQL performance */
    private static final int CHUNK_SIZE = 1000;

    /** Skip limit before failing the step */
    private static final int SKIP_LIMIT = 100;

    @Value("${batch.customer.timeout-minutes:30}")
    private int timeoutMinutes;

    // =========================================================================
    // Job Definition
    // =========================================================================

    /**
     * Main batch job definition.
     * 
     * <p>Mirrors CUSTBAT JCL job with conditional step execution:</p>
     * <ul>
     *   <li>STEP2 runs only if STEP1 completes successfully (COND=(0,NE,STEP1))</li>
     *   <li>STEP3 runs only if STEP2 has RC &lt; 4 (COND=(4,LT,STEP2))</li>
     * </ul>
     */
    @Bean
    public Job customerBatchJob(
            JobRepository jobRepository,
            @Qualifier("customerLoadStep") Step customerLoadStep,
            @Qualifier("customerValidationStep") Step customerValidationStep,
            @Qualifier("customerReportStep") Step customerReportStep,
            BatchJobListener jobListener) {

        return new JobBuilder("customerBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                // STEP1: Always runs first
                .start(customerLoadStep)
                // STEP2: COND=(0,NE,STEP1) - run only if STEP1 succeeded
                .on("COMPLETED").to(customerValidationStep)
                .from(customerLoadStep).on("FAILED").fail()
                // STEP3: COND=(4,LT,STEP2) - run if STEP2 RC < 4
                .from(customerValidationStep).on("COMPLETED").to(customerReportStep)
                .from(customerValidationStep).on("COMPLETED_WITH_WARNINGS").to(customerReportStep)
                .from(customerValidationStep).on("FAILED").fail()
                // End job
                .from(customerReportStep).on("*").end()
                .end()
                .build();
    }

    // =========================================================================
    // STEP1: Load Customer Updates (CUSTLOAD)
    // JCL: //STEP1 EXEC PGM=CUSTLOAD
    // =========================================================================

    /**
     * STEP1: Load daily customer updates from staging table.
     * 
     * <p>Equivalent to JCL STEP1 EXEC PGM=CUSTLOAD:</p>
     * <ul>
     *   <li>INPUT: customer_updates_staging table (PROD.CUST.DAILY.UPDATES)</li>
     *   <li>OUTPUT: customers table (PROD.CUST.MASTER)</li>
     * </ul>
     */
    @Bean
    @Qualifier("customerLoadStep")
    public Step customerLoadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<CustomerStagingRecord> stagingReader,
            ItemProcessor<CustomerStagingRecord, CustomerRecord> loadProcessor,
            ItemWriter<CustomerRecord> customerWriter,
            StepExecutionListener stepListener) {

        return new StepBuilder("customerLoadStep", jobRepository)
                .<CustomerStagingRecord, CustomerRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(stagingReader)
                .processor(loadProcessor)
                .writer(customerWriter)
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .listener(stepListener)
                .build();
    }

    @Bean
    public ItemReader<CustomerStagingRecord> stagingReader(DataSource dataSource) {
        return new StagingTableReader(dataSource);
    }

    @Bean
    public ItemProcessor<CustomerStagingRecord, CustomerRecord> loadProcessor() {
        return new CustomerLoadProcessor();
    }

    @Bean
    public ItemWriter<CustomerRecord> customerWriter(DataSource dataSource) {
        return new CustomerTableWriter(dataSource);
    }

    // =========================================================================
    // STEP2: Validate Customer Data (CUSTVAL)
    // JCL: //STEP2 EXEC PGM=CUSTVAL,COND=(0,NE,STEP1)
    // =========================================================================

    /**
     * STEP2: Validate customer data integrity.
     * 
     * <p>Equivalent to JCL STEP2 EXEC PGM=CUSTVAL:</p>
     * <ul>
     *   <li>INPUT: customers table (PROD.CUST.MASTER)</li>
     *   <li>OUTPUT: Validation errors written to error log</li>
     *   <li>COND: Runs only if STEP1 return code = 0</li>
     * </ul>
     */
    @Bean
    @Qualifier("customerValidationStep")
    public Step customerValidationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<CustomerRecord> customerReader,
            ItemProcessor<CustomerRecord, ValidationResult> validationProcessor,
            ItemWriter<ValidationResult> errorWriter,
            StepExecutionListener stepListener) {

        return new StepBuilder("customerValidationStep", jobRepository)
                .<CustomerRecord, ValidationResult>chunk(CHUNK_SIZE, transactionManager)
                .reader(customerReader)
                .processor(validationProcessor)
                .writer(errorWriter)
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .listener(stepListener)
                .build();
    }

    @Bean
    public ItemReader<CustomerRecord> customerReader(DataSource dataSource) {
        return new CustomerTableReader(dataSource);
    }

    @Bean
    public ItemProcessor<CustomerRecord, ValidationResult> validationProcessor() {
        return new CustomerValidationProcessor();
    }

    @Bean
    public ItemWriter<ValidationResult> errorWriter(DataSource dataSource) {
        return new ErrorFileWriter(dataSource);
    }

    // =========================================================================
    // STEP3: Generate Daily Report (CUSTRPT)
    // JCL: //STEP3 EXEC PGM=CUSTRPT,COND=(4,LT,STEP2)
    // =========================================================================

    /**
     * STEP3: Generate daily customer report.
     * 
     * <p>Equivalent to JCL STEP3 EXEC PGM=CUSTRPT:</p>
     * <ul>
     *   <li>INPUT: customers table (PROD.CUST.MASTER)</li>
     *   <li>OUTPUT: PDF report (replaces SYSOUT print)</li>
     *   <li>COND: Runs only if STEP2 return code &lt; 4</li>
     * </ul>
     */
    @Bean
    @Qualifier("customerReportStep")
    public Step customerReportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<CustomerRecord> reportReader,
            ItemProcessor<CustomerRecord, CustomerRecord> reportProcessor,
            ItemWriter<CustomerRecord> reportWriter,
            StepExecutionListener stepListener) {

        return new StepBuilder("customerReportStep", jobRepository)
                .<CustomerRecord, CustomerRecord>chunk(CHUNK_SIZE, transactionManager)
                .reader(reportReader)
                .processor(reportProcessor)
                .writer(reportWriter)
                .listener(stepListener)
                .build();
    }

    @Bean
    @Qualifier("reportReader")
    public ItemReader<CustomerRecord> reportReader(DataSource dataSource) {
        return new CustomerTableReader(dataSource);
    }

    @Bean
    public ItemProcessor<CustomerRecord, CustomerRecord> reportProcessor() {
        return new CustomerReportProcessor();
    }

    @Bean
    public ItemWriter<CustomerRecord> reportWriter() {
        return new ReportWriter();
    }

    // =========================================================================
    // Listeners
    // =========================================================================

    /**
     * Job-level listener for logging and notifications.
     * Replaces JCL NOTIFY=&amp;SYSUID functionality.
     */
    @Bean
    public BatchJobListener batchJobListener() {
        return new BatchJobListener();
    }

    /**
     * Step-level listener for metrics and error handling.
     * Captures equivalent of JCL condition codes.
     */
    @Bean
    public StepExecutionListener stepExecutionListener() {
        return new StepExecutionListener();
    }
}
