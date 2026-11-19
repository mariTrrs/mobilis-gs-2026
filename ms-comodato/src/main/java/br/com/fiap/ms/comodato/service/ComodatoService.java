package br.com.fiap.ms.comodato.service;

import br.com.fiap.ms.comodato.client.InventarioIntegrationService;
import br.com.fiap.ms.comodato.client.dto.EquipamentoClientResponseDTO;
import br.com.fiap.ms.comodato.dto.request.ComodatoRequestDTO;
import br.com.fiap.ms.comodato.dto.response.ComodatoResponseDTO;
import br.com.fiap.ms.comodato.entity.Comodato;
import br.com.fiap.ms.comodato.entity.StatusComodato;
import br.com.fiap.ms.comodato.exceptions.BusinessException;
import br.com.fiap.ms.comodato.exceptions.DatabaseException;
import br.com.fiap.ms.comodato.exceptions.ResourceNotFoundException;
import br.com.fiap.ms.comodato.repository.ComodatoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComodatoService {

    private static final String STATUS_DISPONIVEL = "DISPONIVEL";
    private static final String STATUS_RESERVADO = "RESERVADO";
    private static final String STATUS_EMPRESTADO = "EMPRESTADO";

    private final ComodatoRepository repository;
    private final InventarioIntegrationService inventarioIntegrationService;

    public ComodatoService(ComodatoRepository repository,
                           InventarioIntegrationService inventarioIntegrationService) {
        this.repository = repository;
        this.inventarioIntegrationService = inventarioIntegrationService;
    }

    @Transactional(readOnly = true)
    public List<ComodatoResponseDTO> findAll() {
        return repository.findAll().stream().map(ComodatoResponseDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public ComodatoResponseDTO findById(Long id) {
        Comodato entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comodato não encontrado: " + id));
        return new ComodatoResponseDTO(entity);
    }

    @Transactional
    public ComodatoResponseDTO create(ComodatoRequestDTO dto) {
        if (dto.getPrazoDevolucao().isBefore(dto.getDataReserva())) {
            throw new BusinessException("Prazo de devolução deve ser igual ou posterior à data da reserva");
        }

        EquipamentoClientResponseDTO equipamento =
                inventarioIntegrationService.buscarEquipamento(dto.getEquipamentoId());
        if (!STATUS_DISPONIVEL.equals(equipamento.getStatus())) {
            throw new BusinessException("Equipamento não está disponível para comodato");
        }

        inventarioIntegrationService.atualizarStatus(dto.getEquipamentoId(), STATUS_RESERVADO);

        try {
            Comodato entity = new Comodato();
            entity.setNomeSolicitante(dto.getNomeSolicitante());
            entity.setDocumentoSolicitante(dto.getDocumentoSolicitante());
            entity.setEquipamentoId(dto.getEquipamentoId());
            entity.setDataReserva(dto.getDataReserva());
            entity.setPrazoDevolucao(dto.getPrazoDevolucao());
            entity.setConsentimentoLgpd(dto.getConsentimentoLgpd());
            entity.setStatus(StatusComodato.SOLICITADO);
            entity = repository.save(entity);
            return new ComodatoResponseDTO(entity);
        } catch (DataIntegrityViolationException e) {
            inventarioIntegrationService.atualizarStatus(dto.getEquipamentoId(), STATUS_DISPONIVEL);
            throw new DatabaseException("Erro de integridade ao criar comodato");
        } catch (RuntimeException e) {
            inventarioIntegrationService.atualizarStatus(dto.getEquipamentoId(), STATUS_DISPONIVEL);
            throw e;
        }
    }

    @Transactional
    public ComodatoResponseDTO update(Long id, ComodatoRequestDTO dto) {
        Comodato entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comodato não encontrado: " + id));
        if (entity.getStatus() != StatusComodato.SOLICITADO) {
            throw new BusinessException("Somente comodatos solicitados podem ser atualizados");
        }
        if (dto.getPrazoDevolucao().isBefore(dto.getDataReserva())) {
            throw new BusinessException("Prazo de devolução deve ser igual ou posterior à data da reserva");
        }
        entity.setNomeSolicitante(dto.getNomeSolicitante());
        entity.setDocumentoSolicitante(dto.getDocumentoSolicitante());
        entity.setDataReserva(dto.getDataReserva());
        entity.setPrazoDevolucao(dto.getPrazoDevolucao());
        entity.setConsentimentoLgpd(dto.getConsentimentoLgpd());
        entity = repository.save(entity);
        return new ComodatoResponseDTO(entity);
    }

    @Transactional
    public ComodatoResponseDTO retirar(Long id) {
        Comodato entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comodato não encontrado: " + id));
        if (entity.getStatus() != StatusComodato.SOLICITADO) {
            throw new BusinessException("Somente comodatos solicitados podem ser retirados");
        }
        inventarioIntegrationService.atualizarStatus(entity.getEquipamentoId(), STATUS_EMPRESTADO);
        entity.setStatus(StatusComodato.RETIRADO);
        entity = repository.save(entity);
        return new ComodatoResponseDTO(entity);
    }

    @Transactional
    public ComodatoResponseDTO devolver(Long id) {
        Comodato entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comodato não encontrado: " + id));
        if (entity.getStatus() != StatusComodato.RETIRADO) {
            throw new BusinessException("Somente comodatos retirados podem ser devolvidos");
        }
        inventarioIntegrationService.atualizarStatus(entity.getEquipamentoId(), STATUS_DISPONIVEL);
        entity.setStatus(StatusComodato.DEVOLVIDO);
        entity = repository.save(entity);
        return new ComodatoResponseDTO(entity);
    }

    @Transactional
    public ComodatoResponseDTO cancelar(Long id) {
        Comodato entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comodato não encontrado: " + id));
        if (entity.getStatus() != StatusComodato.SOLICITADO) {
            throw new BusinessException("Somente comodatos solicitados podem ser cancelados");
        }
        inventarioIntegrationService.atualizarStatus(entity.getEquipamentoId(), STATUS_DISPONIVEL);
        entity.setStatus(StatusComodato.CANCELADO);
        entity = repository.save(entity);
        return new ComodatoResponseDTO(entity);
    }

    @Transactional
    public void delete(Long id) {
        Comodato entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comodato não encontrado: " + id));
        if (entity.getStatus() == StatusComodato.SOLICITADO || entity.getStatus() == StatusComodato.RETIRADO) {
            throw new BusinessException("Finalize ou cancele o comodato antes de excluir");
        }
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Erro de integridade ao excluir comodato");
        }
    }
}
