package br.com.fiap.ms.inventario.repository;

import br.com.fiap.ms.inventario.entity.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {
}
