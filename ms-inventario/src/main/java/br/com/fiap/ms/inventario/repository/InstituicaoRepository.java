package br.com.fiap.ms.inventario.repository;

import br.com.fiap.ms.inventario.entity.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {
}
