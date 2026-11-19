package br.com.fiap.ms.comodato.controller;

import br.com.fiap.ms.comodato.dto.request.ComodatoRequestDTO;
import br.com.fiap.ms.comodato.dto.response.ComodatoResponseDTO;
import br.com.fiap.ms.comodato.service.ComodatoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comodatos")
public class ComodatoController {

    private final ComodatoService service;

    public ComodatoController(ComodatoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ComodatoResponseDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComodatoResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ComodatoResponseDTO> create(@Valid @RequestBody ComodatoRequestDTO dto) {
        ComodatoResponseDTO response = service.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComodatoResponseDTO> update(@PathVariable Long id,
                                                     @Valid @RequestBody ComodatoRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PatchMapping("/{id}/retirada")
    public ResponseEntity<ComodatoResponseDTO> retirada(@PathVariable Long id) {
        return ResponseEntity.ok(service.retirar(id));
    }

    @PatchMapping("/{id}/devolucao")
    public ResponseEntity<ComodatoResponseDTO> devolucao(@PathVariable Long id) {
        return ResponseEntity.ok(service.devolver(id));
    }

    @PatchMapping("/{id}/cancelamento")
    public ResponseEntity<ComodatoResponseDTO> cancelamento(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
