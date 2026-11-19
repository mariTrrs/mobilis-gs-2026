package br.com.fiap.ms.inventario.service;

import br.com.fiap.ms.inventario.dto.request.EquipamentoRequestDTO;
import br.com.fiap.ms.inventario.dto.request.EquipamentoStatusRequestDTO;
import br.com.fiap.ms.inventario.dto.response.EquipamentoResponseDTO;
import br.com.fiap.ms.inventario.entity.Equipamento;
import br.com.fiap.ms.inventario.exceptions.BusinessException;
import br.com.fiap.ms.inventario.exceptions.DatabaseException;
import br.com.fiap.ms.inventario.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.inventario.repository.EquipamentoRepository;
import br.com.fiap.ms.inventario.repository.InstituicaoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipamentoService {

    private final EquipamentoRepository repository;
    private final InstituicaoRepository instituicaoRepository;

    public EquipamentoService(EquipamentoRepository repository, InstituicaoRepository instituicaoRepository) {
        this.repository = repository;
        this.instituicaoRepository = instituicaoRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipamentoResponseDTO> findAll() {
        return repository.findAll().stream().map(EquipamentoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public EquipamentoResponseDTO findById(Long id) {
        Equipamento entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
        return new EquipamentoResponseDTO(entity);
    }

    @Transactional
    public EquipamentoResponseDTO create(EquipamentoRequestDTO dto) {
        if (!instituicaoRepository.existsById(dto.getInstituicaoId())) {
            throw new ResourceNotFoundException("Instituição não encontrada: " + dto.getInstituicaoId());
        }
        try {
            Equipamento entity = new Equipamento();
            copyToEntity(dto, entity);
            entity = repository.save(entity);
            return new EquipamentoResponseDTO(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Erro de integridade ao criar equipamento");
        }
    }

    @Transactional
    public EquipamentoResponseDTO update(Long id, EquipamentoRequestDTO dto) {
        Equipamento entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
        if (!instituicaoRepository.existsById(dto.getInstituicaoId())) {
            throw new ResourceNotFoundException("Instituição não encontrada: " + dto.getInstituicaoId());
        }
        try {
            copyToEntity(dto, entity);
            entity = repository.save(entity);
            return new EquipamentoResponseDTO(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Erro de integridade ao atualizar equipamento");
        }
    }

    @Transactional
    public EquipamentoResponseDTO updateStatus(Long id, EquipamentoStatusRequestDTO dto) {
        Equipamento entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
        if (dto.getStatus() == null) {
            throw new BusinessException("Status do equipamento é obrigatório");
        }
        entity.setStatus(dto.getStatus());
        entity = repository.save(entity);
        return new EquipamentoResponseDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Equipamento não encontrado: " + id);
        }
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Erro de integridade ao excluir equipamento");
        }
    }

    private void copyToEntity(EquipamentoRequestDTO dto, Equipamento entity) {
        entity.setCodigoTag(dto.getCodigoTag());
        entity.setTipo(dto.getTipo());
        entity.setDescricao(dto.getDescricao());
        entity.setCondicao(dto.getCondicao());
        entity.setInstituicaoId(dto.getInstituicaoId());
        entity.setStatus(dto.getStatus());
    }
}
