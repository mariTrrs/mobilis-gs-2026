package br.com.fiap.ms.inventario.service;

import br.com.fiap.ms.inventario.dto.request.InstituicaoRequestDTO;
import br.com.fiap.ms.inventario.dto.response.InstituicaoResponseDTO;
import br.com.fiap.ms.inventario.entity.Instituicao;
import br.com.fiap.ms.inventario.exceptions.DatabaseException;
import br.com.fiap.ms.inventario.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.inventario.repository.InstituicaoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstituicaoService {

    private final InstituicaoRepository repository;

    public InstituicaoService(InstituicaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InstituicaoResponseDTO> findAll() {
        return repository.findAll().stream().map(InstituicaoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public InstituicaoResponseDTO findById(Long id) {
        Instituicao entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada: " + id));
        return new InstituicaoResponseDTO(entity);
    }

    @Transactional
    public InstituicaoResponseDTO create(InstituicaoRequestDTO dto) {
        try {
            Instituicao entity = new Instituicao();
            copyToEntity(dto, entity);
            entity = repository.save(entity);
            return new InstituicaoResponseDTO(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Erro de integridade ao criar instituição");
        }
    }

    @Transactional
    public InstituicaoResponseDTO update(Long id, InstituicaoRequestDTO dto) {
        Instituicao entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada: " + id));
        try {
            copyToEntity(dto, entity);
            entity = repository.save(entity);
            return new InstituicaoResponseDTO(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Erro de integridade ao atualizar instituição");
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Instituição não encontrada: " + id);
        }
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Não é possível excluir instituição com equipamentos vinculados");
        }
    }

    private void copyToEntity(InstituicaoRequestDTO dto, Instituicao entity) {
        entity.setNome(dto.getNome());
        entity.setCnpj(dto.getCnpj());
        entity.setEndereco(dto.getEndereco());
        entity.setBairro(dto.getBairro());
        entity.setCidade(dto.getCidade());
        entity.setTelefone(dto.getTelefone());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
    }
}
