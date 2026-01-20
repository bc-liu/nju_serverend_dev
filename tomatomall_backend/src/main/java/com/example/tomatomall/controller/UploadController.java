package com.example.tomatomall.controller;


import com.example.tomatomall.utils.OssUtil;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class UploadController {

    @Autowired
    OssUtil ossUtil ;

    @PostMapping
    public Response<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String url = ossUtil.upload(filename, file.getInputStream());
            return Response.buildSuccess(url);
        } catch (Exception e) {
            throw new RuntimeException("上传失败");
        }
    }
}
