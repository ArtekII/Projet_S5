
package com.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.entity.Etape;
import com.project.repository.EtapeRepository;

@Service
@Transactional(readOnly = true)
public class EtapeService {

    private final EtapeRepository etapeRepository;

    public EtapeService(EtapeRepository etapeRepository) {
        this.etapeRepository = etapeRepository;
    }

    /** Catalogue des etapes du processus de recrutement. */
    public List<Etape> getEtapes() {
        return etapeRepository.findAllByOrderByIdAsc();
    }
}
