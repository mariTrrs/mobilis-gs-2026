package br.com.fiap.mobilis.domain

object Consultas {
    fun politicaEfetiva(estado: MobilisState, equipamento: Equipamento): PoliticaEfetiva {
        val doItem = estado.politicasItem.find { it.equipamentoId == equipamento.id }
        if (doItem != null) {
            return PoliticaEfetiva(doItem.modalidades, doItem.prazoDias, doItem.exigeComprovanteDoacao, "item")
        }
        val doTipo = estado.politicasTipo.find {
            it.instituicaoId == equipamento.instituicaoId && it.tipoId == equipamento.tipoId
        }
        val modalidades = equipamento.modalidades.ifEmpty {
            doTipo?.modalidades ?: listOf(Modalidade.EMPRESTIMO, Modalidade.DOACAO)
        }
        return PoliticaEfetiva(
            modalidades = modalidades,
            prazoDias = doTipo?.prazoDias ?: 30,
            exigeComprovanteDoacao = doTipo?.exigeComprovanteDoacao ?: true,
            origem = if (doTipo != null) "tipo" else "padrao"
        )
    }

    fun buscar(
        estado: MobilisState,
        solicitanteId: Int,
        tipoId: Int?,
        modalidade: Modalidade?,
        raioKm: Double,
        origemLat: Double,
        origemLng: Double
    ): ResultadoPesquisa {
        val aprovadas = estado.instituicoes.filter { it.status == StatusInstituicao.APROVADA }
        val tiposAtivos = estado.tipos.filter { it.ativo }

        fun distancia(instituicao: Instituicao) =
            Geo.distanciaKm(origemLat, origemLng, instituicao.latitude, instituicao.longitude)

        fun combina(equipamento: Equipamento, ignorarRaio: Boolean): ResultadoBusca? {
            if (equipamento.status != StatusEquipamento.DISPONIVEL) return null
            val tipo = tiposAtivos.find { it.id == equipamento.tipoId } ?: return null
            if (tipoId != null && tipo.id != tipoId) return null
            val instituicao = aprovadas.find { it.id == equipamento.instituicaoId } ?: return null
            val politica = politicaEfetiva(estado, equipamento)
            if (modalidade != null && modalidade !in politica.modalidades) return null
            val km = distancia(instituicao)
            if (!ignorarRaio && km > raioKm) return null
            return ResultadoBusca(equipamento, instituicao, tipo, km, politica)
        }

        val itens = estado.equipamentos.mapNotNull { combina(it, false) }.sortedBy { it.distanciaKm }
        val existemFora = estado.equipamentos.any { equipamento ->
            val dentro = combina(equipamento, false)
            val geral = combina(equipamento, true)
            dentro == null && geral != null
        }
        val tipo = tiposAtivos.find { it.id == tipoId }
        val filas = if (tipo == null || itens.isNotEmpty()) {
            emptyList()
        } else {
            aprovadas.map { it to distancia(it) }
                .filter { (_, km) -> km <= raioKm }
                .map { (instituicao, km) ->
                    OpcaoFila(
                        instituicao = instituicao,
                        tipo = tipo,
                        distanciaKm = km,
                        jaNaFila = estado.filas.any {
                            it.solicitanteId == solicitanteId &&
                                it.instituicaoId == instituicao.id &&
                                it.tipoId == tipo.id &&
                                it.status == "AGUARDANDO"
                        }
                    )
                }
                .sortedBy { it.distanciaKm }
        }
        return ResultadoPesquisa(itens, filas, existemFora)
    }

    fun painel(estado: MobilisState, instituicaoId: Int, tipoId: Int? = null, modalidade: Modalidade? = null): PainelGestao {
        val limite = estado.instituicoes.find { it.id == instituicaoId }?.limiteOciosidadeDias ?: 30
        val equipamentos = estado.equipamentos.filter {
            it.instituicaoId == instituicaoId && (tipoId == null || it.tipoId == tipoId)
        }
        val pedidos = estado.pedidos.filter {
            it.instituicaoId == instituicaoId &&
                (tipoId == null || it.tipoId == tipoId) &&
                (modalidade == null || it.modalidade == modalidade)
        }
        val porStatus = StatusEquipamento.entries.map { status ->
            Contagem(status.rotulo, equipamentos.count { it.status == status })
        }.filter { it.quantidade > 0 }
        val porTipo = estado.tipos.map { tipo ->
            Contagem(tipo.nome, equipamentos.count { it.tipoId == tipo.id && it.status != StatusEquipamento.BAIXADO })
        }.filter { it.quantidade > 0 }
        val ativos = pedidos.filter { it.status !in setOf(StatusPedido.CANCELADO, StatusPedido.RECUSADO) }
        val porModalidade = Modalidade.entries.map { modalidade ->
            Contagem(modalidade.rotulo, ativos.count { it.modalidade == modalidade })
        }
        val emprestimosAtivos = pedidos.filter {
            it.status == StatusPedido.RETIRADO && it.modalidade == Modalidade.EMPRESTIMO
        }
        val proximos = emprestimosAtivos.filter { Datas.venceEmAte(it.prazoDevolucao, 7) }
        val atrasos = emprestimosAtivos.filter { Datas.vencida(it.prazoDevolucao) }
        val ociosos = equipamentos.filter {
            it.status == StatusEquipamento.DISPONIVEL && Datas.diasDesde(it.disponivelDesde) >= limite
        }
        val previsoes = estado.tipos.mapNotNull { tipo ->
            if (tipoId != null && tipo.id != tipoId) return@mapNotNull null
            val fila = estado.filas.count { it.instituicaoId == instituicaoId && it.tipoId == tipo.id && it.status == "AGUARDANDO" }
            val disponiveis = equipamentos.count { it.tipoId == tipo.id && it.status == StatusEquipamento.DISPONIVEL }
            val historico = estado.pedidos.count { it.instituicaoId == instituicaoId && it.tipoId == tipo.id }
            if (fila == 0 && disponiveis == 0 && historico == 0) return@mapNotNull null
            if (historico < 3) {
                return@mapNotNull PrevisaoDemanda(tipo.nome, fila, disponiveis, "Histórico insuficiente para calcular uma previsão.", false)
            }
            val criterio = when {
                fila > disponiveis -> "A fila está maior que o estoque disponível."
                fila == 0 && disponiveis > 0 -> "Há equipamento parado e nenhuma pessoa na fila."
                else -> "A fila e o estoque estão equilibrados."
            }
            PrevisaoDemanda(tipo.nome, fila, disponiveis, criterio, true)
        }
        val encerramentos = pedidos.filter { it.naoPrecisaMais }
        return PainelGestao(porStatus, porTipo, porModalidade, proximos, atrasos, ociosos, previsoes, encerramentos)
    }

    fun relatorio(estado: MobilisState, instituicaoId: Int, tipoId: Int? = null, modalidade: Modalidade? = null): String {
        val instituicao = estado.instituicoes.find { it.id == instituicaoId }
        val painel = painel(estado, instituicaoId, tipoId, modalidade)
        return buildString {
            appendLine("Relatório Mobilis — ${instituicao?.razaoSocial ?: "Instituição"}")
            appendLine("Gerado em ${Datas.exibirEpoch(System.currentTimeMillis())}")
            appendLine()
            appendLine("Equipamentos por situação")
            painel.porStatus.forEach { appendLine("- ${it.rotulo}: ${it.quantidade}") }
            appendLine()
            appendLine("Equipamentos por tipo")
            painel.porTipo.forEach { appendLine("- ${it.rotulo}: ${it.quantidade}") }
            appendLine()
            appendLine("Pedidos por modalidade")
            painel.porModalidade.forEach { appendLine("- ${it.rotulo}: ${it.quantidade}") }
            appendLine()
            appendLine("Empréstimos próximos do vencimento: ${painel.proximos.size}")
            appendLine("Atrasos: ${painel.atrasos.size}")
            appendLine("Ociosos acima do limite da instituição: ${painel.ociosos.size}")
            appendLine()
            appendLine("Previsão de demanda")
            painel.previsoes.forEach {
                appendLine("- ${it.tipo}: fila ${it.fila}, disponíveis ${it.disponiveis}. ${it.criterio}")
            }
        }.trim()
    }

    fun relatorioCsv(estado: MobilisState, instituicaoId: Int, tipoId: Int? = null, modalidade: Modalidade? = null): String {
        val linhas = estado.pedidos.filter {
            it.instituicaoId == instituicaoId &&
                (tipoId == null || it.tipoId == tipoId) &&
                (modalidade == null || it.modalidade == modalidade)
        }
        return buildString {
            appendLine("equipamento;tipo;modalidade;status")
            linhas.forEach { pedido ->
                val codigo = estado.equipamentos.find { it.id == pedido.equipamentoId }?.codigo ?: ""
                appendLine("$codigo;${Consultas.nomeTipo(estado, pedido.tipoId)};${pedido.modalidade.rotulo};${pedido.status.rotulo}")
            }
        }.trim()
    }

    fun exportarPessoa(estado: MobilisState, usuarioId: Int): String {
        val usuario = estado.usuarios.find { it.id == usuarioId } ?: return "Sem dados."
        val pedidos = estado.pedidos.filter { it.solicitanteId == usuarioId }
        val filas = estado.filas.filter { it.solicitanteId == usuarioId }
        return buildString {
            appendLine("Meus dados — Mobilis")
            appendLine("Nome: ${usuario.nome}")
            appendLine("E-mail: ${usuario.email}")
            appendLine("Documento: ${usuario.documento}")
            appendLine("Telefone: ${usuario.telefone}")
            appendLine("CEP: ${usuario.cep}")
            appendLine("Perfil: ${usuario.perfil.rotulo}")
            appendLine("Termo: ${usuario.versaoTermo.ifBlank { if (usuario.aceitouTermoGeral) Regras.VERSAO_TERMO else "sem aceite" }}")
            appendLine("Notificação push: ${if (usuario.notificarPush) "sim" else "não"}")
            appendLine("Notificação e-mail: ${if (usuario.notificarEmail) "sim" else "não"}")
            appendLine()
            appendLine("Pedidos")
            if (pedidos.isEmpty()) appendLine("- Nenhum")
            pedidos.forEach {
                val tipo = estado.tipos.find { tipo -> tipo.id == it.tipoId }?.nome ?: "Equipamento"
                appendLine("- #$it.id $tipo, ${it.modalidade.rotulo}, ${it.status.rotulo}")
            }
            appendLine()
            appendLine("Filas")
            if (filas.isEmpty()) appendLine("- Nenhuma")
            filas.forEach {
                val tipo = estado.tipos.find { tipo -> tipo.id == it.tipoId }?.nome ?: "Equipamento"
                appendLine("- $tipo")
            }
        }.trim()
    }

    fun bloqueiosExclusao(estado: MobilisState, usuarioId: Int): List<String> {
        val abertos = setOf(StatusPedido.AGUARDANDO_ANALISE, StatusPedido.APROVADO, StatusPedido.RETIRADO)
        return estado.pedidos
            .filter { it.solicitanteId == usuarioId && it.status in abertos }
            .map { pedido ->
                val tipo = estado.tipos.find { it.id == pedido.tipoId }?.nome ?: "equipamento"
                "Pedido de $tipo está ${pedido.status.rotulo.lowercase()}."
            }
    }

    fun auditoria(
        estado: MobilisState,
        ator: Usuario,
        texto: String,
        acao: String
    ): List<RegistroAuditoria> {
        val base = when {
            ator.perfil == Perfil.ADMIN -> estado.auditoria
            ator.perfil == Perfil.INSTITUICAO && ator.papel == PapelInstituicao.GESTOR ->
                estado.auditoria.filter { it.instituicaoId == ator.instituicaoId }
            else -> erro("Seu perfil não consulta auditoria.")
        }
        val busca = texto.trim().lowercase()
        return base.filter { registro ->
            (acao.isBlank() || registro.acao.equals(acao, true)) &&
                (busca.isEmpty() ||
                    registro.usuarioNome.lowercase().contains(busca) ||
                    registro.acao.lowercase().contains(busca) ||
                    registro.entidade.lowercase().contains(busca) ||
                    registro.antes.lowercase().contains(busca) ||
                    registro.depois.lowercase().contains(busca))
        }.sortedByDescending { it.data }
    }

    fun nomeTipo(estado: MobilisState, tipoId: Int): String =
        estado.tipos.find { it.id == tipoId }?.nome ?: "Equipamento"

    fun nomePessoa(estado: MobilisState, usuarioId: Int): String =
        estado.usuarios.find { it.id == usuarioId }?.nome ?: "Pessoa"
}
