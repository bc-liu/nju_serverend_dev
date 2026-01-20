package com.example.tomatomall.service;

import com.example.tomatomall.vo.AdvertisementsVO;

import java.util.List;

public interface AdvertisementsService {
    List<AdvertisementsVO> getAllAdvertisements();
    void updateAdvertisements(Integer id, String title, String content, String imgUrl, Integer productId);
    AdvertisementsVO createAdvertisements(AdvertisementsVO advertisements);
    void deleteAdvertisements(Integer id);
}
