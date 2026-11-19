package br.com.fiap.ms.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_equipamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_tag", nullable = false, unique = true, length = 50)
    private String codigoTag;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, length = 80)
    private String condicao;

    @Column(name = "instituicao_id", nullable = false)
    private Long instituicaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusEquipamento status;
}
