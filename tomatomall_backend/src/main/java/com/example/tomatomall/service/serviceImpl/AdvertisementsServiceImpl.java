package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.Repository.AdvertisementsRepository;
import com.example.tomatomall.Repository.ProductRepository;
import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.Advertisements;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.service.AdvertisementsService;
import com.example.tomatomall.vo.AdvertisementsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdvertisementsServiceImpl implements AdvertisementsService {

    @Autowired
    AdvertisementsRepository advertisementsRepository;

    @Autowired
    ProductRepository productRepository;

    public List<AdvertisementsVO> getAllAdvertisements(){

        List<Advertisements> advertisementsVOList = advertisementsRepository.findAll();

        List<AdvertisementsVO> retVOs = new ArrayList<>();
        for (Advertisements advertisements : advertisementsVOList) {
            retVOs.add(advertisements.toVO());
        }

        List<AdvertisementsVO> reverseRetVOs = new ArrayList<>(retVOs.size());
        for (int i = retVOs.size() - 1; i >= 0; i--) {
            reverseRetVOs.add(retVOs.get(i));
        }
        return reverseRetVOs;
    }
    public void updateAdvertisements(Integer id,
                                     String title,
                                     String content,
                                     String imgUrl,
                                     Integer productId){

        if(id == null || productId == null){
            return;
        }

        Advertisements advertisements = advertisementsRepository.findById(id).orElseThrow(TomatoMallException::advertisementsNotFound);

        if(title != null && !title.isEmpty()){
            advertisements.setTitle(title);
        }
        if(content != null && !content.isEmpty()){
            advertisements.setContent(content);
        }
        if(imgUrl != null && !imgUrl.isEmpty()){
            advertisements.setImageUrl(imgUrl);
        }

        advertisements.setProductId(productId);
        advertisementsRepository.save(advertisements);
    }
    public AdvertisementsVO createAdvertisements(AdvertisementsVO advertisements){
        Advertisements advertisementsPO = advertisements.toPO();
        Advertisements retAdvertisements = advertisementsRepository.save(advertisementsPO);
        return retAdvertisements.toVO();
    }
    public void deleteAdvertisements(Integer id){
        Advertisements advertisements = advertisementsRepository.findById(id).orElseThrow(TomatoMallException::advertisementsNotFound);
        advertisementsRepository.delete(advertisements);
    }
}
