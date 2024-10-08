package com.cgi.service;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3service {

	@Autowired
	private S3Client s3client;

	public void uploadFileToS3(String bucketName, String key, File file) {
		s3client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(), RequestBody.fromFile(file));
	}
}
