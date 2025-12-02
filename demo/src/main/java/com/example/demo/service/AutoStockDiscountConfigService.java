package com.example.demo.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.AutoStockDiscountConfig;
import com.example.demo.repository.AutoStockDiscountConfigRepository;

@Service
public class AutoStockDiscountConfigService {
    
    @Autowired
    private AutoStockDiscountConfigRepository configRepo;
    
    public Optional<AutoStockDiscountConfig> getActiveConfig() {
        return configRepo.findByActiveTrue();
    }
    
    @Transactional
    public AutoStockDiscountConfig saveConfig(AutoStockDiscountConfig config) {
        if (config.getActive() != null && config.getActive()) {
            Optional<AutoStockDiscountConfig> oldActive = configRepo.findByActiveTrue();
            if (oldActive.isPresent() && !oldActive.get().getConfigId().equals(config.getConfigId())) {
                oldActive.get().setActive(false);
                configRepo.save(oldActive.get());
            }
        }
        return configRepo.save(config);
    }
    
    @Transactional
    public AutoStockDiscountConfig createConfig(Integer minStockQuantity, Integer discountPercent, 
                                                 LocalDate startDate, LocalDate endDate) {
        Optional<AutoStockDiscountConfig> oldConfig = configRepo.findByActiveTrue();
        if (oldConfig.isPresent()) {
            oldConfig.get().setActive(false);
            configRepo.save(oldConfig.get());
        }
        
        AutoStockDiscountConfig newConfig = new AutoStockDiscountConfig();
        newConfig.setMinStockQuantity(minStockQuantity);
        newConfig.setDiscountPercent(discountPercent != null ? discountPercent : 10);
        newConfig.setStartDate(startDate);
        newConfig.setEndDate(endDate);
        newConfig.setActive(true);
        
        return configRepo.save(newConfig);
    }
    
    public java.util.List<AutoStockDiscountConfig> getAllConfigs() {
        return configRepo.findAll();
    }
    
    @Transactional
    public void deleteConfig(Integer configId) {
        configRepo.deleteById(configId);
    }
}

