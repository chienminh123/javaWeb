package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Provider;
import com.example.demo.repository.ProviderRepository;

@Service
@Transactional
public class ProviderService {
    @Autowired 
    private ProviderRepository repo;

    public List<Provider> findAll() { return repo.findAll(); }
    public Provider save(Provider p) { return repo.save(p); }
}
