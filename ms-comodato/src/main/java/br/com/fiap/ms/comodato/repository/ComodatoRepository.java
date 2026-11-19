package br.com.fiap.ms.comodato.repository;

import br.com.fiap.ms.comodato.entity.Comodato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComodatoRepository extends JpaRepository<Comodato, Long> {
}
