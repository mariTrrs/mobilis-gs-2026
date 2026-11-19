package br.com.fiap.ms.inventario.dto.response;

import br.com.fiap.ms.inventario.entity.Instituicao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstituicaoResponseDTO {

    private Long id;
    private String nome;
    private String cnpj;
    private String endereco;
    private String bairro;
    private String cidade;
    private String telefone;
    private Double latitude;
    private Double longitude;

    public InstituicaoResponseDTO(Instituicao entity) {
        this.id = entity.getId();
        this.nome = entity.getNome();
        this.cnpj = entity.getCnpj();
        this.endereco = entity.getEndereco();
        this.bairro = entity.getBairro();
        this.cidade = entity.getCidade();
        this.telefone = entity.getTelefone();
        this.latitude = entity.getLatitude();
        this.longitude = entity.getLongitude();
    }
}
