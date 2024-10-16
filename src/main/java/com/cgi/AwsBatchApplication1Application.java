package com.cgi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import com.cgi.model.Employee;
import com.cgi.service.S3service;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@SpringBootApplication(scanBasePackages = "com.cgi")
@EnableBatchProcessing
public class AwsBatchApplication1Application {

	private static final Logger log = LoggerFactory.getLogger(AwsBatchApplication1Application.class);
	
	public static void main(String[] args) {
		log.debug("*** JOB is started");
		SpringApplication.run(AwsBatchApplication1Application.class, args);
		log.debug("*** JOB is completed..");
	}

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

//	@Autowired
//	public DataSource dataSource;

//	@Autowired
//	public S3service s3service;

//	@Value("${aws.s3.bucket}")
//	public String bucketName;

//	@Bean
//	public S3Client s3Client() {
//		return S3Client.builder().region(Region.US_EAST_2).build();
//	}
	
//	@Bean
//	public JdbcCursorItemReader<Employee> reader() {
//		JdbcCursorItemReader<Employee> reader = new JdbcCursorItemReader<Employee>();
//		reader.setDataSource(dataSource);
//		reader.setSql("select id, name, department from Employee");
//		reader.setRowMapper(new BeanPropertyRowMapper<Employee>(Employee.class));
//		return reader;
//	}

//	
//	@Bean
//	public FlatFileItemWriter<Employee> writer() {
//		log.debug("***  FlatFileItemWriter: Writer()...");
//		return new FlatFileItemWriterBuilder<Employee>().name("employeeWriter")
//				.resource(new FileSystemResource("employees.csv")).delimited().delimiter(",")
//				.names(new String[]{"id", "name", "department"}).build();
//	}
	
	
	@Bean
	public FlatFileItemReader<Employee> s3FileReader() throws IOException {
		log.info("*** s3FileReader..");
	    S3Client s3 = S3Client.builder().region(Region.AP_SOUTH_1).build();
	    String bucketName = "my-spring-bucket1";
	    String key = "employees1.csv";

	    // Get the file from S3
	    ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(GetObjectRequest.builder()
	            .bucket(bucketName)
	            .key(key)
	            .build());

	    // Set up FlatFileItemReader to read from the InputStream
	    FlatFileItemReader<Employee> itemReader = new FlatFileItemReader<>();
	    itemReader.setResource(new InputStreamResource(s3Object));  // Pass InputStream directly
	    itemReader.setLineMapper(new DefaultLineMapper() {
	        {
	            setLineTokenizer(new DelimitedLineTokenizer() {{
	                setNames("id", "name", "department");
	            }});
	            setFieldSetMapper(new BeanWrapperFieldSetMapper() {{
	                setTargetType(Employee.class);
	            }});
	        }
	    });

	    return itemReader;
	}


	
	@Bean
	public ItemWriter<Employee> s3FileWriter() {
		log.info("*** s3FileWriter..");
	    return employees -> {
	        S3Client s3 = S3Client.builder().region(Region.AP_SOUTH_1).build();
	        String targetBucket = "my-spring-bucket-output1";
	        String targetKey = "processed-employees_"+ System.currentTimeMillis()+".csv";

	        // Build the file content (CSV format)
	        StringBuilder csvData = new StringBuilder("id,name,department\n");
	        for (Employee employee : employees) {
	            csvData.append(employee.getId()).append(",")
	                   .append(employee.getName()).append(",")
	                   .append(employee.getDepartment()).append("\n");
	        }

	        // Put the processed file to S3
	        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
	                .bucket(targetBucket)
	                .key(targetKey)
	                .build();

	        s3.putObject(putObjectRequest, RequestBody.fromString(csvData.toString()));
	    };
	}


	
//	@Bean
//	public Step step1(ItemReader<Employee> reader, ItemWriter<Employee> writer) {
//		return stepBuilderFactory.get("step1").<Employee, Employee>chunk(10).reader(reader).writer(writer).build();
//	}

//	@Bean
//	public Step s3UploadStep() {
//		return stepBuilderFactory.get("S3UploadStep").tasklet(uploadToS3Tasklet()).build();
//	}

//	@Bean
//	public Tasklet uploadToS3Tasklet() {
//		// TODO Auto-generated method stub
//		log.debug("*** uploadToS3Tasklet: Uploading the file ..");
//		return (contribution, chunkContext) -> {
//			File file = new File("employees.csv");
//			s3service.uploadFileToS3(bucketName, "employees.csv", file);
//			return RepeatStatus.FINISHED;
//		};
//	}

	/*
	@Bean
	public Job exportEmployeeJob(Step step1, Step s3UploadStep) {
		return jobBuilderFactory.get("ExportEmployeeJob").incrementer(new RunIdIncrementer()).start(step1)
				.next(s3UploadStep).build();
	}
	*/
	@Bean
	public Job processFileJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) throws IOException {
	    return jobBuilderFactory.get("processFileJob")
	    		.incrementer(new RunIdIncrementer())
	            .start(step1(stepBuilderFactory))
	            .build();
	}
	
	@Bean
	public Step step1(StepBuilderFactory stepBuilderFactory) throws IOException {
	    return stepBuilderFactory.get("step1")
	            .<Employee, Employee>chunk(10)
	            .reader(s3FileReader()) // Read from source S3 bucket
	            .writer(s3FileWriter())  // Write to target S3 bucket
	            .build();
	}
}
