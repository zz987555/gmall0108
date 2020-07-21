package com.atguigu.gmall.manage;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void contextLoads() throws IOException, MyException {
        //获得config
        String tracker = GmallManageWebApplicationTests.class.getResource("/tracker.conf").getPath();
        //设置全局Client配置，每次新建Client都自动包含配置
        ClientGlobal.init(tracker);

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer trackerServer = trackerClient.getConnection();

        StorageClient storageClient = new StorageClient(trackerServer,null);

        String[] uploadInfos = storageClient.upload_file("d:/a.jpg", "jpg", null);

        String url = "http://192.168.31.2";

        for (String uploadInfo : uploadInfos) {
//            System.out.println(uploadInfo);

            url += "/"+uploadInfo;
        }
        System.out.println(url);

    }

}
