package com.atguigu.gmall.manage.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class PmsUploadUtil {


    public static String uploadImage(MultipartFile multipartFile) {

        String imgUrl = "http://192.168.31.2";

        //获得config
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();
        //设置全局Client配置，每次新建Client都自动包含配置
        try {
            ClientGlobal.init(tracker);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer trackerServer = null;
        try {
            trackerServer = trackerClient.getConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        StorageClient storageClient = new StorageClient(trackerServer, null);


        try {
            byte[] bytes = multipartFile.getBytes();

            String originalFilename = multipartFile.getOriginalFilename();
            System.out.println(originalFilename);
            int i = originalFilename.lastIndexOf(".");
            String extName = originalFilename.substring(i + 1);

            String[] uploadInfos = storageClient.upload_file(bytes, extName, null);

            for (String uploadInfo : uploadInfos) {
//            System.out.println(uploadInfo);

                imgUrl += "/" + uploadInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgUrl;
    }
}
