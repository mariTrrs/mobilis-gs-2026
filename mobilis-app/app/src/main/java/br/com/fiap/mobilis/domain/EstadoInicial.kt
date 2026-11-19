package br.com.fiap.mobilis.domain

object EstadoInicial {
    const val SENHA_DEMO = "Mobilis@2026"
    private const val EMAIL_ADMIN = "admin@mobilis.app"

    fun restaurarSenhaAdmin(estado: MobilisState): MobilisState {
        if (estado.versao >= 3) return estado
        val hash = Seguranca.hashSenha(SENHA_DEMO)
        return estado.copy(
            versao = 3,
            usuarios = estado.usuarios.map { usuario ->
                if (usuario.perfil == Perfil.ADMIN && Validacao.emailNormalizado(usuario.email) == EMAIL_ADMIN) {
                    usuario.copy(senhaHash = hash)
                } else {
                    usuario
                }
            }
        )
    }

    fun criar(agora: Long = System.currentTimeMillis()): MobilisState {
        val senha = Seguranca.hashSenha(SENHA_DEMO)
        val hoje = Datas.hoje()
        val ha40 = agora - 40L * 86_400_000L
        val instituicoes = listOf(
            Instituicao(1, "ONG Caminhar", "11222333000181", "Rua da Consolação, 100, São Paulo", "01302000", -23.5505, -46.6333, "Segunda a sexta, 9h às 17h", "Carlos Mendes", "gestor@mobilis.app", "11987654321", StatusInstituicao.APROVADA),
            Instituicao(2, "Hospital Comunitário Luz", "04567890000179", "Rua da Luz, 250, São Paulo", "01120000", -23.70, -46.78, "Todos os dias, 8h às 18h", "Helena Duarte", "hospital@mobilis.app", "11912345678", StatusInstituicao.APROVADA),
            Instituicao(3, "Igreja Esperança", "33444555000181", "Rua Esperança, 40, São Paulo", "03015000", -23.53, -46.62, "Terça e quinta, 14h às 17h", "João Ribeiro", "igreja@mobilis.app", "11999887766", StatusInstituicao.PENDENTE)
        )
        val usuarios = listOf(
            Usuario(1, Perfil.SOLICITANTE, null, "Ana Costa", "52998224725", "ana.costa@mobilis.app", "11911112222", "01310000", "", senha, null, true, true, true, true),
            Usuario(2, Perfil.SOLICITANTE, null, "Bruno Lima", "39053344705", "bruno.lima@mobilis.app", "11933334444", "01310000", "", senha, null, true, true, true, true),
            Usuario(3, Perfil.INSTITUICAO, PapelInstituicao.GESTOR, "Carlos Mendes", "11222333000181", "gestor@mobilis.app", "11987654321", "01302000", "Rua da Consolação, 100", senha, 1, true, true, true, true),
            Usuario(4, Perfil.INSTITUICAO, PapelInstituicao.OPERADOR, "Lúcia Ferreira", "", "operador@mobilis.app", "11955556666", "", "", senha, 1, true, true, true, true),
            Usuario(5, Perfil.INSTITUICAO, PapelInstituicao.GESTOR, "Helena Duarte", "04567890000179", "hospital@mobilis.app", "11912345678", "01120000", "Rua da Luz, 250", senha, 2, true, true, true, true),
            Usuario(6, Perfil.INSTITUICAO, PapelInstituicao.GESTOR, "João Ribeiro", "33444555000181", "igreja@mobilis.app", "11999887766", "03015000", "Rua Esperança, 40", senha, 3, true, true, true, true),
            Usuario(7, Perfil.ADMIN, null, "Admin Mobilis", "", "admin@mobilis.app", "", "", "", senha, null, true, true, true, true)
        )
        val tipos = listOf(
            TipoEquipamento(1, "Cadeira de rodas", "Para quem não caminha sozinho neste período.", "Confira se o assento e os apoios cabem no dia a dia.", true),
            TipoEquipamento(2, "Cadeira de banho", "Para quem não fica em pé com segurança no banho.", "A cadeira fica no banheiro e volta limpa e seca.", true),
            TipoEquipamento(3, "Andador", "Para quem precisa de mais equilíbrio ao caminhar.", "As quatro pontas encostam no chão antes do passo.", true),
            TipoEquipamento(4, "Muleta", "Para tirar o peso de uma perna depois de uma lesão.", "O apoio fica na altura do punho.", true),
            TipoEquipamento(5, "Bengala", "Apoio leve para quem já caminha.", "Use do lado oposto à perna mais fraca.", true),
            TipoEquipamento(6, "Cama hospitalar", "Para cuidado no quarto, quando a equipe orientar.", "A retirada é na instituição, com a família.", true)
        )
        val ambas = listOf(Modalidade.EMPRESTIMO, Modalidade.DOACAO)
        val politicasTipo = instituicoes.filter { it.status == StatusInstituicao.APROVADA }.flatMap { instituicao ->
            tipos.map { tipo ->
                PoliticaTipo(
                    id = instituicao.id * 100 + tipo.id,
                    instituicaoId = instituicao.id,
                    tipoId = tipo.id,
                    modalidades = ambas,
                    prazoDias = 30,
                    exigeComprovanteDoacao = true
                )
            }
        }
        fun equipamento(
            id: Int,
            instituicaoId: Int,
            tipoId: Int,
            conservacao: Conservacao,
            status: StatusEquipamento,
            pedidoId: Int? = null,
            desde: Long = agora,
            modalidades: List<Modalidade> = ambas,
            obs: String = ""
        ) = Equipamento(
            id = id,
            instituicaoId = instituicaoId,
            tipoId = tipoId,
            codigo = "EQ-%04d".format(id),
            conservacao = conservacao,
            observacoes = obs,
            modalidades = modalidades,
            fotos = emptyList(),
            status = status,
            atualizadoEm = agora,
            disponivelDesde = desde,
            pedidoAtualId = pedidoId
        )
        val equipamentos = listOf(
            equipamento(1, 1, 1, Conservacao.OTIMO, StatusEquipamento.DISPONIVEL, obs = "Dobrável, uso interno e calçada."),
            equipamento(2, 1, 1, Conservacao.BOM, StatusEquipamento.EMPRESTADO, pedidoId = 1),
            equipamento(3, 1, 3, Conservacao.BOM, StatusEquipamento.DISPONIVEL, desde = ha40, obs = "Parado no estoque há mais de 30 dias."),
            equipamento(4, 1, 3, Conservacao.REGULAR, StatusEquipamento.EMPRESTADO, pedidoId = 2),
            equipamento(5, 1, 4, Conservacao.BOM, StatusEquipamento.RESERVADO, pedidoId = 4),
            equipamento(6, 1, 5, Conservacao.OTIMO, StatusEquipamento.DOADO, pedidoId = null),
            equipamento(7, 1, 2, Conservacao.BOM, StatusEquipamento.HIGIENIZACAO, obs = "Aguardando conclusão da limpeza."),
            equipamento(8, 1, 6, Conservacao.REGULAR, StatusEquipamento.BAIXADO, obs = "Estrutura torta, sem conserto."),
            equipamento(9, 2, 2, Conservacao.OTIMO, StatusEquipamento.DISPONIVEL, obs = "Cadeira de banho do hospital.")
        )
        val pedidos = listOf(
            Pedido(1, 1, 1, 2, 1, Modalidade.EMPRESTIMO, StatusPedido.RETIRADO, "Recuperação de cirurgia no joelho.", null, null, agora - 5L * 86_400_000L, Datas.maisDias(-2, hoje), Datas.maisDias(5, hoje), true),
            Pedido(2, 1, 1, 4, 3, Modalidade.EMPRESTIMO, StatusPedido.RETIRADO, "Apoio para caminhar dentro de casa.", null, null, agora - 20L * 86_400_000L, Datas.maisDias(-20, hoje), Datas.maisDias(-3, hoje), true),
            Pedido(3, 1, 1, 6, 5, Modalidade.DOACAO, StatusPedido.DOADO, "Uso permanente depois da alta.", "laudo.txt", null, agora - 15L * 86_400_000L, Datas.maisDias(-16, hoje), null, true, termoDoacaoAceito = true),
            Pedido(4, 2, 1, 5, 4, Modalidade.EMPRESTIMO, StatusPedido.AGUARDANDO_ANALISE, "Entorse no tornozelo e dificuldade para sair de casa.", null, null, agora, Datas.maisDias(3, hoje), null, false)
        )
        val filas = listOf(EntradaFila(1, 2, 1, 6, agora))
        val manutencoes = listOf(
            Manutencao(1, 7, TipoManutencao.HIGIENIZACAO, Datas.exibir(hoje), "Lúcia Ferreira", "Limpeza iniciada depois do último uso.", agora)
        )
        val movimentacoes = listOf(
            Movimentacao(1, 2, "Retirada confirmada", "Reservado", "Emprestado", agora, "Lúcia Ferreira"),
            Movimentacao(2, 4, "Retirada confirmada", "Reservado", "Emprestado", agora, "Lúcia Ferreira"),
            Movimentacao(3, 6, "Doação concluída", "Reservado", "Doado", agora, "Lúcia Ferreira"),
            Movimentacao(4, 8, "Baixa", "Disponível", "Baixado", agora, "Lúcia Ferreira")
        )
        val auditoria = listOf(
            RegistroAuditoria(1, 7, "Admin Mobilis", null, "Aprovar instituição", "Instituição", 1, agora, "Pendente", "Aprovada"),
            RegistroAuditoria(2, 3, "Carlos Mendes", 1, "Cadastrar equipamento", "Equipamento", 1, agora, "", "EQ-0001 disponível")
        )
        val indicacoes = listOf(
            Indicacao(1, "UBS Sé", "Ana Costa", "52998224725", 3, "Dra. Paula Nunes", "CRM 123456", "PENDENTE", agora)
        )
        val convites = listOf(
            Convite(1, 1, "convite@mobilis.app", "246810", "PENDENTE", agora)
        )
        return MobilisState(
            versao = 3,
            usuarios = usuarios,
            instituicoes = instituicoes,
            tipos = tipos,
            politicasTipo = politicasTipo,
            politicasItem = listOf(
                PoliticaItem(1, 1, listOf(Modalidade.EMPRESTIMO), 21, false)
            ),
            equipamentos = equipamentos,
            pedidos = pedidos,
            filas = filas,
            manutencoes = manutencoes,
            movimentacoes = movimentacoes,
            convites = convites,
            auditoria = auditoria,
            indicacoes = indicacoes,
            proximoId = 1000
        )
    }
}
