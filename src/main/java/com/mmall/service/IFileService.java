package com.mmall.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Created by mlin4 on 2019/2/21.
 */
public interface IFileService {

    String upload(MultipartFile file, String path);
}
