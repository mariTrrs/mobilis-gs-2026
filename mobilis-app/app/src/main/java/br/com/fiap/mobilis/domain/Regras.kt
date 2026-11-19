package br.com.fiap.mobilis.domain

object Regras {
    private const val MINUTOS_15 = 15L * 60 * 1000
    private const val DIAS_CONVITE = 7L * 24 * 60 * 60 * 1000
    private const val LIMITE_PEDIDOS = 3
    private const val DIAS_ATRASO_GRAVE = 7
    const val VERSAO_TERMO = "2026.1"

    fun autenticar(estado: MobilisState, email: String, senha: String, agora: Long = System.currentTimeMillis()): ResultadoLogin {
        val normalizado = Validacao.emailNormalizado(email)
        val usuario = estado.usuarios.find { it.email.equals(normalizado, true) }
            ?: return ResultadoLogin(estado, mensagemErro = "E-mail ou senha incorretos.")
        if (usuario.bloqueadoAte > agora) {
            return ResultadoLogin(estado, mensagemErro = "Login bloqueado por 15 minutos depois de cinco tentativas incorretas.")
        }
        if (!usuario.ativo) return ResultadoLogin(estado, mensagemErro = "Esta conta está desativada.")
        if (!usuario.emailConfirmado) return ResultadoLogin(estado, mensagemErro = "Confirme o código antes de entrar.")
        val instituicao = estado.instituicoes.find { it.id == usuario.instituicaoId }
        if (usuario.perfil == Perfil.INSTITUICAO && instituicao?.status == StatusInstituicao.REPROVADA) {
            return ResultadoLogin(estado, mensagemErro = "A instituição foi reprovada. Motivo: ${instituicao.motivoReprovacao ?: "não informado"}.")
        }
        if (usuario.senhaHash != Seguranca.hashSenha(senha)) {
            val tentativas = usuario.tentativasFalhas + 1
            val bloqueadoAte = if (tentativas >= 5) agora + MINUTOS_15 else 0L
            val atualizado = usuario.copy(tentativasFalhas = if (tentativas >= 5) 0 else tentativas, bloqueadoAte = bloqueadoAte)
            val novo = estado.copy(usuarios = estado.usuarios.map { if (it.id == usuario.id) atualizado else it })
            val mensagem = if (tentativas >= 5) {
                "Login bloqueado por 15 minutos depois de cinco tentativas incorretas."
            } else {
                "E-mail ou senha incorretos."
            }
            return ResultadoLogin(novo, mensagemErro = mensagem)
        }
        val liberado = usuario.copy(tentativasFalhas = 0, bloqueadoAte = 0)
        val novo = estado.copy(usuarios = estado.usuarios.map { if (it.id == usuario.id) liberado else it })
        return ResultadoLogin(novo, usuario = liberado)
    }

    fun iniciarCadastroSolicitante(
        estado: MobilisState,
        dados: NovoSolicitante,
        codigo: String,
        agora: Long = System.currentTimeMillis(),
        termoGeral: Boolean,
        termoSaude: Boolean
    ): MobilisState {
        exigirTermos(termoGeral, termoSaude)
        validarSolicitante(dados)
        garantirEmailLivre(estado, dados.email)
        if (estado.usuarios.any { it.documento == dados.cpf && it.perfil == Perfil.SOLICITANTE }) {
            erro("Já existe cadastro com este CPF.")
        }
        val pendente = CadastroPendente(dados.email, codigo, agora + MINUTOS_15, termoGeral, termoSaude, dados, null)
        return estado.copy(cadastrosPendentes = estado.cadastrosPendentes.filter { it.email != dados.email } + pendente)
    }

    fun iniciarCadastroInstituicao(
        estado: MobilisState,
        dados: NovaInstituicao,
        codigo: String,
        agora: Long = System.currentTimeMillis(),
        termoGeral: Boolean,
        termoSaude: Boolean
    ): MobilisState {
        exigirTermos(termoGeral, termoSaude)
        validarInstituicao(dados)
        garantirEmailLivre(estado, dados.email)
        if (estado.instituicoes.any { it.cnpj == dados.cnpj }) erro("Já existe instituição com este CNPJ.")
        val pendente = CadastroPendente(dados.email, codigo, agora + MINUTOS_15, termoGeral, termoSaude, null, dados)
        return estado.copy(cadastrosPendentes = estado.cadastrosPendentes.filter { it.email != dados.email } + pendente)
    }

    fun confirmarCadastro(estado: MobilisState, email: String, codigo: String, agora: Long = System.currentTimeMillis()): MobilisState {
        val pendente = estado.cadastrosPendentes.find { it.email == Validacao.emailNormalizado(email) }
            ?: erro("Não há cadastro esperando confirmação para este e-mail.")
        if (pendente.expiraEm < agora) erro("O código expirou. Peça outro.")
        if (pendente.codigo != codigo.trim()) erro("Código incorreto.")
        if (!pendente.aceitouTermoGeral || !pendente.aceitouDadosSaude) erro("Aceite os dois termos para concluir o cadastro.")
        var atual = estado.copy(cadastrosPendentes = estado.cadastrosPendentes.filter { it.email != pendente.email })
        pendente.solicitante?.let { dados ->
            val (base, id) = atual.alocar()
            val usuario = Usuario(
                id = id,
                perfil = Perfil.SOLICITANTE,
                nome = dados.nome,
                documento = dados.cpf,
                email = dados.email,
                telefone = dados.telefone,
                cep = dados.cep,
                senhaHash = dados.senhaHash,
                emailConfirmado = true,
                aceitouTermoGeral = true,
                aceitouDadosSaude = true,
                versaoTermo = VERSAO_TERMO,
                termoAceitoEm = System.currentTimeMillis()
            )
            return base.copy(usuarios = base.usuarios + usuario)
        }
        val dados = pendente.instituicao ?: erro("Cadastro incompleto.")
        val (comInstituicao, instituicaoId) = atual.alocar()
        val coordenada = Geo.coordenadaDoCep(dados.cep)
        val instituicao = Instituicao(
            id = instituicaoId,
            razaoSocial = dados.razaoSocial,
            cnpj = dados.cnpj,
            endereco = dados.endereco,
            cep = dados.cep,
            latitude = coordenada.first,
            longitude = coordenada.second,
            horarioRetirada = dados.horarioRetirada,
            responsavelNome = dados.responsavelNome,
            responsavelEmail = dados.email,
            telefone = dados.telefone,
            status = StatusInstituicao.PENDENTE
        )
        val (comUsuario, usuarioId) = comInstituicao.alocar()
        val gestor = Usuario(
            id = usuarioId,
            perfil = Perfil.INSTITUICAO,
            papel = PapelInstituicao.GESTOR,
            nome = dados.responsavelNome,
            documento = dados.cnpj,
            email = dados.email,
            telefone = dados.telefone,
            cep = dados.cep,
            endereco = dados.endereco,
            senhaHash = dados.senhaHash,
            instituicaoId = instituicaoId,
            emailConfirmado = true,
            aceitouTermoGeral = true,
            aceitouDadosSaude = true,
            versaoTermo = VERSAO_TERMO,
            termoAceitoEm = System.currentTimeMillis()
        )
        return comUsuario.copy(
            instituicoes = comUsuario.instituicoes + instituicao,
            usuarios = comUsuario.usuarios + gestor
        )
    }

    fun pedirRecuperacao(estado: MobilisState, email: String, codigo: String, agora: Long = System.currentTimeMillis()): MobilisState {
        val normalizado = Validacao.emailNormalizado(email)
        estado.usuarios.find { it.email == normalizado && it.ativo } ?: erro("Não encontramos uma conta com este e-mail.")
        val codigoAcesso = CodigoAcesso(normalizado, codigo, agora + MINUTOS_15)
        return estado.copy(recuperacoes = estado.recuperacoes.filter { it.email != normalizado } + codigoAcesso)
    }

    fun redefinirSenha(estado: MobilisState, email: String, codigo: String, novaSenha: String, agora: Long = System.currentTimeMillis()): MobilisState {
        Validacao.senha(novaSenha)?.let { erro(it) }
        val normalizado = Validacao.emailNormalizado(email)
        val recuperacao = estado.recuperacoes.find { it.email == normalizado } ?: erro("Peça o código de recuperação antes.")
        if (recuperacao.expiraEm < agora) erro("O código expirou. Peça outro.")
        if (recuperacao.codigo != codigo.trim()) erro("Código incorreto.")
        return estado.copy(
            usuarios = estado.usuarios.map {
                if (it.email == normalizado) it.copy(senhaHash = Seguranca.hashSenha(novaSenha)) else it
            },
            recuperacoes = estado.recuperacoes.filter { it.email != normalizado }
        )
    }

    fun aprovarInstituicao(estado: MobilisState, atorId: Int, instituicaoId: Int): MobilisState {
        val ator = exigir(estado, atorId).also { it.administrar() }
        val instituicao = exigirInstituicao(estado, instituicaoId)
        if (instituicao.status == StatusInstituicao.APROVADA) erro("Esta instituição já está aprovada.")
        val depois = instituicao.copy(status = StatusInstituicao.APROVADA, motivoReprovacao = null)
        val gestor = estado.usuarios.find { it.instituicaoId == instituicaoId && it.papel == PapelInstituicao.GESTOR && it.ativo }
        val auditado = estado.copy(instituicoes = substituir(estado.instituicoes, depois)).auditar(
            ator, "Aprovar instituição", "Instituição", instituicaoId, null, instituicao.status.rotulo, depois.status.rotulo
        )
        return if (gestor == null) auditado else auditado.avisar(
            gestor.id,
            "conta",
            "Instituição aprovada",
            "${instituicao.razaoSocial} foi aprovada. Saia e entre de novo para abrir o painel.",
            "aprovacao-$instituicaoId"
        )
    }

    fun reprovarInstituicao(estado: MobilisState, atorId: Int, instituicaoId: Int, motivo: String): MobilisState {
        val ator = exigir(estado, atorId).also { it.administrar() }
        Validacao.motivo(motivo, "o motivo da reprovação")?.let { erro(it) }
        val instituicao = exigirInstituicao(estado, instituicaoId)
        val depois = instituicao.copy(status = StatusInstituicao.REPROVADA, motivoReprovacao = motivo.trim())
        val gestor = estado.usuarios.find { it.instituicaoId == instituicaoId && it.papel == PapelInstituicao.GESTOR && it.ativo }
        val auditado = estado.copy(instituicoes = substituir(estado.instituicoes, depois)).auditar(
            ator, "Reprovar instituição", "Instituição", instituicaoId, null, instituicao.status.rotulo, "${depois.status.rotulo}: ${depois.motivoReprovacao}"
        )
        return if (gestor == null) auditado else auditado.avisar(
            gestor.id,
            "conta",
            "Instituição reprovada",
            "Motivo: ${motivo.trim()}",
            "reprovacao-$instituicaoId"
        )
    }

    fun salvarTipo(estado: MobilisState, atorId: Int, id: Int, nome: String, descricao: String, orientacao: String): MobilisState {
        val ator = exigir(estado, atorId).also { it.administrar() }
        val nomeLimpo = nome.trim()
        if (nomeLimpo.isEmpty()) erro("Informe o nome do tipo.")
        val duplicado = estado.tipos.any { it.id != id && it.nome.equals(nomeLimpo, true) }
        if (duplicado) erro("Já existe um tipo com este nome.")
        if (id == 0) {
            val (base, novoId) = estado.alocar()
            val tipo = TipoEquipamento(novoId, nomeLimpo, descricao.trim(), orientacao.trim(), true)
            return base.copy(tipos = base.tipos + tipo).auditar(ator, "Criar tipo", "Tipo", novoId, null, "", tipo.nome)
        }
        val atual = estado.tipos.find { it.id == id } ?: erro("Tipo não encontrado.")
        val tipo = atual.copy(nome = nomeLimpo, descricao = descricao.trim(), orientacao = orientacao.trim())
        return estado.copy(tipos = estado.tipos.map { if (it.id == id) tipo else it })
            .auditar(ator, "Editar tipo", "Tipo", id, null, atual.nome, tipo.nome)
    }

    fun desativarTipo(estado: MobilisState, atorId: Int, id: Int): MobilisState {
        val ator = exigir(estado, atorId).also { it.administrar() }
        val tipo = estado.tipos.find { it.id == id } ?: erro("Tipo não encontrado.")
        if (!tipo.ativo) erro("Este tipo já está desativado.")
        return estado.copy(tipos = estado.tipos.map { if (it.id == id) it.copy(ativo = false) else it })
            .auditar(ator, "Desativar tipo", "Tipo", id, null, "Ativo", "Inativo")
    }

    fun salvarPoliticaTipo(
        estado: MobilisState,
        atorId: Int,
        tipoId: Int,
        modalidades: List<Modalidade>,
        prazoDias: Int,
        exigeComprovante: Boolean
    ): MobilisState {
        val ator = exigir(estado, atorId)
        val instituicaoId = ator.instituicaoId ?: erro("Esta ação é do gestor.")
        ator.gerir(instituicaoId)
        validarPolitica(modalidades, prazoDias)
        val existente = estado.politicasTipo.find { it.instituicaoId == instituicaoId && it.tipoId == tipoId }
        val politica = if (existente == null) {
            val (base, id) = estado.alocar()
            val nova = PoliticaTipo(id, instituicaoId, tipoId, modalidades.distinct(), prazoDias, exigeComprovante)
            return base.copy(politicasTipo = base.politicasTipo + nova).auditar(
                ator, "Salvar política do tipo", "Política", id, instituicaoId, "", modalidades.joinToString { it.rotulo }
            )
        } else {
            existente.copy(modalidades = modalidades.distinct(), prazoDias = prazoDias, exigeComprovanteDoacao = exigeComprovante)
        }
        return estado.copy(politicasTipo = estado.politicasTipo.map { if (it.id == politica.id) politica else it })
            .auditar(ator, "Salvar política do tipo", "Política", politica.id, instituicaoId, existente.modalidades.joinToString { it.rotulo }, politica.modalidades.joinToString { it.rotulo })
    }

    fun salvarPoliticaItem(
        estado: MobilisState,
        atorId: Int,
        equipamentoId: Int,
        modalidades: List<Modalidade>,
        prazoDias: Int,
        exigeComprovante: Boolean
    ): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = exigirEquipamento(estado, equipamentoId)
        ator.gerir(equipamento.instituicaoId)
        validarPolitica(modalidades, prazoDias)
        val existente = estado.politicasItem.find { it.equipamentoId == equipamentoId }
        if (existente == null) {
            val (base, id) = estado.alocar()
            val nova = PoliticaItem(id, equipamentoId, modalidades.distinct(), prazoDias, exigeComprovante)
            return base.copy(politicasItem = base.politicasItem + nova).auditar(
                ator, "Salvar política do item", "Política", id, equipamento.instituicaoId, "Política do tipo", "Política do item prevalece"
            )
        }
        val politica = existente.copy(modalidades = modalidades.distinct(), prazoDias = prazoDias, exigeComprovanteDoacao = exigeComprovante)
        return estado.copy(politicasItem = estado.politicasItem.map { if (it.id == politica.id) politica else it })
            .auditar(ator, "Salvar política do item", "Política", politica.id, equipamento.instituicaoId, "Item", "Item atualizado")
    }

    fun removerPoliticaItem(estado: MobilisState, atorId: Int, equipamentoId: Int): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = exigirEquipamento(estado, equipamentoId)
        ator.gerir(equipamento.instituicaoId)
        if (estado.politicasItem.none { it.equipamentoId == equipamentoId }) erro("Este item já segue a política do tipo.")
        return estado.copy(politicasItem = estado.politicasItem.filter { it.equipamentoId != equipamentoId })
            .auditar(ator, "Remover política do item", "Política", equipamentoId, equipamento.instituicaoId, "Item", "Tipo")
    }

    fun salvarEquipamento(
        estado: MobilisState,
        atorId: Int,
        id: Int,
        tipoId: Int,
        conservacao: Conservacao,
        observacoes: String,
        modalidades: List<Modalidade>,
        fotos: List<String>
    ): MobilisState {
        val ator = exigir(estado, atorId)
        if (fotos.size > 3) erro("É possível anexar no máximo 3 fotos.")
        if (modalidades.isEmpty()) erro("Selecione ao menos uma modalidade.")
        val tipo = estado.tipos.find { it.id == tipoId } ?: erro("Escolha o tipo do equipamento.")
        if (!tipo.ativo && id == 0) erro("Este tipo está desativado.")
        val agora = System.currentTimeMillis()
        if (id == 0) {
            val instituicaoId = ator.instituicaoId ?: erro("Esta ação é da equipe da instituição.")
            ator.operar(instituicaoId)
            val (base, novoId) = estado.alocar()
            val equipamento = Equipamento(
                id = novoId,
                instituicaoId = instituicaoId,
                tipoId = tipoId,
                codigo = "EQ-%04d".format(novoId),
                conservacao = conservacao,
                observacoes = observacoes.trim(),
                modalidades = modalidades.distinct(),
                fotos = fotos,
                status = StatusEquipamento.DISPONIVEL,
                atualizadoEm = agora,
                disponivelDesde = agora
            )
            return base.copy(equipamentos = base.equipamentos + equipamento)
                .movimento(novoId, "Cadastro", "", StatusEquipamento.DISPONIVEL.rotulo, ator.nome)
                .auditar(ator, "Cadastrar equipamento", "Equipamento", novoId, instituicaoId, "", equipamento.codigo)
                .oferecerFila(novoId, ator.nome)
        }
        val atual = exigirEquipamento(estado, id)
        ator.operar(atual.instituicaoId)
        if (atual.status == StatusEquipamento.DOADO || atual.status == StatusEquipamento.BAIXADO) {
            erro("Este equipamento está encerrado e não pode ser editado.")
        }
        val travaModalidade = atual.status != StatusEquipamento.DISPONIVEL && atual.status != StatusEquipamento.HIGIENIZACAO
        val equipamento = atual.copy(
            tipoId = tipoId,
            conservacao = conservacao,
            observacoes = observacoes.trim(),
            modalidades = if (travaModalidade) atual.modalidades else modalidades.distinct(),
            fotos = fotos,
            atualizadoEm = agora
        )
        return estado.copy(equipamentos = estado.equipamentos.map { if (it.id == id) equipamento else it })
            .auditar(ator, "Editar equipamento", "Equipamento", id, atual.instituicaoId, atual.codigo, equipamento.codigo)
    }

    fun baixar(estado: MobilisState, atorId: Int, equipamentoId: Int, motivo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = exigirEquipamento(estado, equipamentoId)
        ator.operar(equipamento.instituicaoId)
        Validacao.motivo(motivo, "o motivo da baixa")?.let { erro(it) }
        if (equipamento.status == StatusEquipamento.EMPRESTADO || equipamento.status == StatusEquipamento.DEVOLUCAO_PENDENTE) {
            erro("Não é possível dar baixa em um item emprestado.")
        }
        if (equipamento.status == StatusEquipamento.RESERVADO || equipamento.status == StatusEquipamento.DOADO || equipamento.status == StatusEquipamento.BAIXADO) {
            erro("Este item não pode receber baixa agora.")
        }
        val antes = equipamento.status.rotulo
        val atualizado = equipamento.copy(status = StatusEquipamento.BAIXADO, atualizadoEm = System.currentTimeMillis(), pedidoAtualId = null)
        return estado.copy(equipamentos = estado.equipamentos.map { if (it.id == equipamentoId) atualizado else it })
            .movimento(equipamentoId, "Baixa: ${motivo.trim()}", antes, StatusEquipamento.BAIXADO.rotulo, ator.nome)
            .auditar(ator, "Baixa", "Equipamento", equipamentoId, equipamento.instituicaoId, antes, motivo.trim())
    }

    fun enviarParaHigienizacao(estado: MobilisState, atorId: Int, equipamentoId: Int): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = exigirEquipamento(estado, equipamentoId)
        ator.operar(equipamento.instituicaoId)
        if (equipamento.status != StatusEquipamento.DISPONIVEL) erro("Só um item disponível pode ir para higienização.")
        val atualizado = equipamento.copy(status = StatusEquipamento.HIGIENIZACAO, atualizadoEm = System.currentTimeMillis())
        return estado.copy(equipamentos = estado.equipamentos.map { if (it.id == equipamentoId) atualizado else it })
            .movimento(equipamentoId, "Enviado para higienização", StatusEquipamento.DISPONIVEL.rotulo, StatusEquipamento.HIGIENIZACAO.rotulo, ator.nome)
    }

    fun registrarManutencao(
        estado: MobilisState,
        atorId: Int,
        equipamentoId: Int,
        tipo: TipoManutencao,
        data: String,
        responsavel: String,
        observacoes: String
    ): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = exigirEquipamento(estado, equipamentoId)
        ator.operar(equipamento.instituicaoId)
        val dataIso = Datas.interpretar(data) ?: erro("Informe a data no formato dd/mm/aaaa.")
        if (responsavel.trim().length < 3) erro("Informe o responsável.")
        if (observacoes.trim().length < 3) erro("Informe as observações.")
        val (base, id) = estado.alocar()
        val registro = Manutencao(id, equipamentoId, tipo, Datas.exibir(dataIso), responsavel.trim(), observacoes.trim(), System.currentTimeMillis())
        var atual = base.copy(manutencoes = base.manutencoes + registro)
        if (equipamento.status == StatusEquipamento.HIGIENIZACAO) {
            val liberado = equipamento.copy(
                status = StatusEquipamento.DISPONIVEL,
                atualizadoEm = System.currentTimeMillis(),
                disponivelDesde = System.currentTimeMillis()
            )
            atual = atual.copy(equipamentos = atual.equipamentos.map { if (it.id == equipamentoId) liberado else it })
                .movimento(equipamentoId, tipo.rotulo, StatusEquipamento.HIGIENIZACAO.rotulo, StatusEquipamento.DISPONIVEL.rotulo, ator.nome)
                .oferecerFila(equipamentoId, ator.nome)
        } else {
            atual = atual.movimento(equipamentoId, tipo.rotulo, equipamento.status.rotulo, equipamento.status.rotulo, ator.nome)
        }
        return atual.auditar(ator, "Registrar ${tipo.rotulo.lowercase()}", "Manutenção", id, equipamento.instituicaoId, "", observacoes.trim())
    }

    fun entrarFila(estado: MobilisState, solicitanteId: Int, instituicaoId: Int, tipoId: Int): MobilisState {
        val pessoa = exigir(estado, solicitanteId)
        if (pessoa.perfil != Perfil.SOLICITANTE) erro("A fila é para quem solicita o equipamento.")
        if (estado.filas.any { it.solicitanteId == solicitanteId && it.instituicaoId == instituicaoId && it.tipoId == tipoId && it.status == "AGUARDANDO" }) {
            erro("Você já está na fila deste equipamento.")
        }
        val (base, id) = estado.alocar()
        return base.copy(filas = base.filas + EntradaFila(id, solicitanteId, instituicaoId, tipoId, System.currentTimeMillis()))
    }

    fun sairFila(estado: MobilisState, solicitanteId: Int, filaId: Int): MobilisState {
        val fila = estado.filas.find { it.id == filaId && it.solicitanteId == solicitanteId } ?: erro("Fila não encontrada.")
        return estado.copy(filas = estado.filas.filter { it.id != fila.id })
    }

    fun criarPedido(
        estado: MobilisState,
        solicitanteId: Int,
        equipamentoId: Int,
        modalidade: Modalidade,
        justificativa: String,
        comprovanteNome: String?,
        comprovanteCaminho: String?
    ): MobilisState {
        val pessoa = exigir(estado, solicitanteId)
        if (pessoa.perfil != Perfil.SOLICITANTE) erro("Apenas o solicitante pode pedir um equipamento.")
        if (!pessoa.aceitouTermoGeral || !pessoa.aceitouDadosSaude) erro("Aceite os termos antes de pedir um equipamento.")
        if (estado.pedidos.any { it.solicitanteId == pessoa.id && it.atrasoGrave && it.status == StatusPedido.RETIRADO }) {
            erro("Há um empréstimo em atraso grave. Devolva o item antes de fazer outro pedido.")
        }
        val abertos = setOf(StatusPedido.AGUARDANDO_ANALISE, StatusPedido.APROVADO, StatusPedido.RETIRADO)
        if (estado.pedidos.count { it.solicitanteId == pessoa.id && it.status in abertos } >= LIMITE_PEDIDOS) {
            erro("Você já tem 3 pedidos em andamento.")
        }
        val equipamento = exigirEquipamento(estado, equipamentoId)
        if (equipamento.status != StatusEquipamento.DISPONIVEL) erro("Este equipamento não está disponível.")
        val instituicao = exigirInstituicao(estado, equipamento.instituicaoId)
        if (instituicao.status != StatusInstituicao.APROVADA) erro("A instituição ainda não está aprovada.")
        val politica = Consultas.politicaEfetiva(estado, equipamento)
        if (modalidade !in politica.modalidades) erro("Esta modalidade não é permitida para o item.")
        if (justificativa.trim().length < 10) erro("Descreva o motivo com pelo menos 10 caracteres.")
        exigirComprovanteSePreciso(modalidade, politica, comprovanteCaminho)
        val (base, pedidoId) = estado.alocar()
        val pedido = Pedido(
            id = pedidoId,
            solicitanteId = solicitanteId,
            instituicaoId = instituicao.id,
            equipamentoId = equipamento.id,
            tipoId = equipamento.tipoId,
            modalidade = modalidade,
            status = StatusPedido.AGUARDANDO_ANALISE,
            justificativa = justificativa.trim(),
            comprovanteNome = comprovanteNome,
            comprovanteCaminho = comprovanteCaminho,
            criadoEm = System.currentTimeMillis(),
            prazoRetirada = Datas.maisDias(3)
        )
        val reservado = equipamento.copy(status = StatusEquipamento.RESERVADO, pedidoAtualId = pedidoId, atualizadoEm = System.currentTimeMillis())
        return base.copy(
            pedidos = base.pedidos + pedido,
            equipamentos = base.equipamentos.map { if (it.id == equipamento.id) reservado else it }
        ).movimento(equipamento.id, "Reserva solicitada", StatusEquipamento.DISPONIVEL.rotulo, StatusEquipamento.RESERVADO.rotulo, pessoa.nome)
            .auditar(pessoa, "Solicitar reserva", "Pedido", pedidoId, instituicao.id, StatusEquipamento.DISPONIVEL.rotulo, StatusPedido.AGUARDANDO_ANALISE.rotulo)
    }

    fun cancelarPedido(estado: MobilisState, solicitanteId: Int, pedidoId: Int): MobilisState {
        val pessoa = exigir(estado, solicitanteId)
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != pessoa.id) erro("Este pedido é de outra pessoa.")
        if (pedido.status !in setOf(StatusPedido.AGUARDANDO_ANALISE, StatusPedido.APROVADO)) {
            erro("Não é possível cancelar depois da retirada.")
        }
        val atualizado = pedido.copy(status = StatusPedido.CANCELADO)
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .liberarReserva(pedido, pessoa.nome, "Pedido cancelado")
            .oferecerFila(pedido.equipamentoId, pessoa.nome)
    }

    fun proporModalidade(estado: MobilisState, solicitanteId: Int, pedidoId: Int, modalidade: Modalidade): MobilisState {
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != solicitanteId) erro("Este pedido é de outra pessoa.")
        if (pedido.status !in setOf(StatusPedido.AGUARDANDO_ANALISE, StatusPedido.APROVADO)) {
            erro("A troca de modalidade só vale antes da retirada.")
        }
        if (pedido.propostaStatus == StatusProposta.AGUARDANDO) erro("Já existe uma proposta aguardando resposta.")
        if (modalidade == pedido.modalidade) erro("Escolha a outra modalidade permitida.")
        val equipamento = exigirEquipamento(estado, pedido.equipamentoId)
        val politica = Consultas.politicaEfetiva(estado, equipamento)
        if (modalidade !in politica.modalidades) erro("Esta modalidade não é permitida para o item.")
        if (modalidade == Modalidade.DOACAO) exigirComprovanteSePreciso(modalidade, politica, pedido.comprovanteCaminho)
        val atualizado = pedido.copy(propostaModalidade = modalidade, propostaStatus = StatusProposta.AGUARDANDO, propostaMotivo = null)
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
    }

    fun responderProposta(estado: MobilisState, atorId: Int, pedidoId: Int, aceitar: Boolean, motivo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val pedido = exigirPedido(estado, pedidoId)
        ator.operar(pedido.instituicaoId)
        if (pedido.propostaStatus != StatusProposta.AGUARDANDO || pedido.propostaModalidade == null) {
            erro("Não há proposta aguardando resposta.")
        }
        val atualizado = if (aceitar) {
            pedido.copy(modalidade = pedido.propostaModalidade, propostaStatus = StatusProposta.ACEITA, termoAceito = false)
        } else {
            Validacao.motivo(motivo, "o motivo da recusa")?.let { erro(it) }
            pedido.copy(propostaStatus = StatusProposta.RECUSADA, propostaMotivo = motivo.trim())
        }
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .auditar(ator, "Responder proposta", "Pedido", pedidoId, pedido.instituicaoId, pedido.modalidade.rotulo, atualizado.modalidade.rotulo)
            .avisar(
                pedido.solicitanteId,
                "pedido",
                if (aceitar) "Modalidade alterada" else "Proposta recusada",
                if (aceitar) "A instituição aceitou ${atualizado.modalidade.rotulo}. Aceite o termo de novo antes da retirada." else "A modalidade original foi mantida. Motivo: ${motivo.trim()}",
                "proposta-$pedidoId-${if (aceitar) "sim" else "nao"}"
            )
    }

    fun aprovarPedido(estado: MobilisState, atorId: Int, pedidoId: Int): MobilisState {
        val ator = exigir(estado, atorId)
        val pedido = exigirPedido(estado, pedidoId)
        ator.operar(pedido.instituicaoId)
        if (pedido.status != StatusPedido.AGUARDANDO_ANALISE) erro("Este pedido não está aguardando análise.")
        val politica = Consultas.politicaEfetiva(estado, exigirEquipamento(estado, pedido.equipamentoId))
        if (pedido.modalidade == Modalidade.DOACAO && politica.exigeComprovanteDoacao && pedido.comprovanteCaminho.isNullOrBlank() && !pedido.validadoPorIndicacao) {
            erro("A instituição exige comprovante para aprovar a doação.")
        }
        val atualizado = pedido.copy(status = StatusPedido.APROVADO, prazoRetirada = Datas.maisDias(3), motivoRecusa = null)
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .auditar(ator, "Aprovar pedido", "Pedido", pedidoId, pedido.instituicaoId, StatusPedido.AGUARDANDO_ANALISE.rotulo, StatusPedido.APROVADO.rotulo)
            .avisar(pedido.solicitanteId, "pedido", "Pedido aprovado", "Retire o equipamento até ${Datas.exibir(atualizado.prazoRetirada)}, depois de aceitar o termo.", "aprovado-$pedidoId")
    }

    fun recusarPedido(estado: MobilisState, atorId: Int, pedidoId: Int, motivo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val pedido = exigirPedido(estado, pedidoId)
        ator.operar(pedido.instituicaoId)
        if (pedido.status != StatusPedido.AGUARDANDO_ANALISE) erro("Este pedido não está aguardando análise.")
        Validacao.motivo(motivo, "o motivo da recusa")?.let { erro(it) }
        val atualizado = pedido.copy(status = StatusPedido.RECUSADO, motivoRecusa = motivo.trim())
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .liberarReserva(pedido, ator.nome, "Pedido recusado")
            .oferecerFila(pedido.equipamentoId, ator.nome)
            .auditar(ator, "Recusar pedido", "Pedido", pedidoId, pedido.instituicaoId, StatusPedido.AGUARDANDO_ANALISE.rotulo, motivo.trim())
            .avisar(pedido.solicitanteId, "pedido", "Pedido recusado", motivo.trim(), "recusado-$pedidoId")
    }

    fun anexarComprovante(estado: MobilisState, solicitanteId: Int, pedidoId: Int, nome: String, caminho: String): MobilisState {
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != solicitanteId) erro("Este pedido é de outra pessoa.")
        if (pedido.status in setOf(StatusPedido.CANCELADO, StatusPedido.RECUSADO, StatusPedido.DEVOLVIDO)) {
            erro("Este pedido não aceita mais comprovante.")
        }
        return estado.copy(pedidos = substituir(estado.pedidos, pedido.copy(comprovanteNome = nome, comprovanteCaminho = caminho)))
    }

    fun aceitarTermo(estado: MobilisState, solicitanteId: Int, pedidoId: Int): MobilisState {
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != solicitanteId) erro("Este pedido é de outra pessoa.")
        if (pedido.status != StatusPedido.APROVADO) erro("O termo fica disponível depois da aprovação.")
        val politica = Consultas.politicaEfetiva(estado, exigirEquipamento(estado, pedido.equipamentoId))
        exigirComprovanteSePreciso(pedido.modalidade, politica, pedido.comprovanteCaminho)
        return estado.copy(pedidos = substituir(estado.pedidos, pedido.copy(termoAceito = true)))
    }

    fun confirmarRetirada(estado: MobilisState, atorId: Int, codigo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = localizarPorCodigo(estado, codigo)
        if (equipamento.status == StatusEquipamento.BAIXADO) erro("Este QR Code está baixado e indisponível.")
        ator.operar(equipamento.instituicaoId)
        val pedido = estado.pedidos.find { it.equipamentoId == equipamento.id && it.status == StatusPedido.APROVADO }
            ?: erro("Não há retirada aprovada para este QR Code.")
        if (!pedido.termoAceito) erro("O solicitante precisa aceitar o termo no aplicativo antes da retirada.")
        if (equipamento.status != StatusEquipamento.RESERVADO) erro("O equipamento não está reservado para retirada.")
        val politica = Consultas.politicaEfetiva(estado, equipamento)
        val agora = System.currentTimeMillis()
        val (pedidoNovo, equipamentoNovo) = if (pedido.modalidade == Modalidade.EMPRESTIMO) {
            pedido.copy(status = StatusPedido.RETIRADO, prazoDevolucao = Datas.maisDias(politica.prazoDias)) to
                equipamento.copy(status = StatusEquipamento.EMPRESTADO, atualizadoEm = agora, pedidoAtualId = pedido.id)
        } else {
            pedido.copy(status = StatusPedido.DOADO, prazoDevolucao = null, termoDoacaoAceito = true) to
                equipamento.copy(status = StatusEquipamento.DOADO, atualizadoEm = agora, pedidoAtualId = null)
        }
        val antes = equipamento.status.rotulo
        return estado.copy(
            pedidos = substituir(estado.pedidos, pedidoNovo),
            equipamentos = estado.equipamentos.map { if (it.id == equipamento.id) equipamentoNovo else it }
        ).movimento(equipamento.id, if (pedido.modalidade == Modalidade.EMPRESTIMO) "Retirada do empréstimo" else "Retirada da doação", antes, equipamentoNovo.status.rotulo, ator.nome)
            .auditar(ator, "Confirmar retirada", "Pedido", pedido.id, equipamento.instituicaoId, antes, equipamentoNovo.status.rotulo)
            .avisar(
                pedido.solicitanteId,
                "retirada",
                if (pedido.modalidade == Modalidade.EMPRESTIMO) "Retirada confirmada" else "Doação concluída",
                if (pedido.modalidade == Modalidade.EMPRESTIMO) "Devolva até ${Datas.exibir(pedidoNovo.prazoDevolucao)}." else "O equipamento ficou com você em definitivo.",
                "retirada-${pedido.id}"
            )
    }

    fun pedirProrrogacao(estado: MobilisState, solicitanteId: Int, pedidoId: Int): MobilisState {
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != solicitanteId) erro("Este pedido é de outra pessoa.")
        if (pedido.status != StatusPedido.RETIRADO || pedido.modalidade != Modalidade.EMPRESTIMO) {
            erro("A prorrogação vale para empréstimo já retirado.")
        }
        if (Datas.vencida(pedido.prazoDevolucao)) erro("O prazo já venceu. A prorrogação não está disponível.")
        if (pedido.prorrogado || pedido.prorrogacaoStatus == StatusProposta.ACEITA) erro("Este empréstimo já foi prorrogado.")
        if (pedido.prorrogacaoStatus == StatusProposta.AGUARDANDO) erro("A prorrogação já está com a instituição.")
        if (estado.filas.any { it.instituicaoId == pedido.instituicaoId && it.tipoId == pedido.tipoId && it.status == "AGUARDANDO" }) {
            erro("Há pessoas na fila deste equipamento. A prorrogação não está disponível.")
        }
        val atualizado = pedido.copy(prorrogacaoStatus = StatusProposta.AGUARDANDO)
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .avisarGestores(pedido.instituicaoId, "prazo", "Pedido de prorrogação", "Há um pedido de mais 15 dias para analisar.", "prorroga-$pedidoId")
    }

    fun decidirProrrogacao(estado: MobilisState, atorId: Int, pedidoId: Int, permitir: Boolean, motivo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val pedido = exigirPedido(estado, pedidoId)
        ator.operar(pedido.instituicaoId)
        if (pedido.prorrogacaoStatus != StatusProposta.AGUARDANDO) erro("Não há prorrogação aguardando análise.")
        val atualizado = if (permitir) {
            pedido.copy(prazoDevolucao = Datas.maisDias(15, pedido.prazoDevolucao), prorrogado = true, prorrogacaoStatus = StatusProposta.ACEITA)
        } else {
            Validacao.motivo(motivo, "o motivo da recusa")?.let { erro(it) }
            pedido.copy(prorrogacaoStatus = StatusProposta.RECUSADA)
        }
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .avisar(
                pedido.solicitanteId,
                "prazo",
                if (permitir) "Prorrogação aceita" else "Prorrogação recusada",
                if (permitir) "A nova data de devolução é ${Datas.exibir(atualizado.prazoDevolucao)}." else "A data original foi mantida. Motivo: ${motivo.trim()}",
                "prorroga-decisao-$pedidoId"
            )
    }

    fun registrarDevolucao(estado: MobilisState, atorId: Int, codigo: String, conservacao: Conservacao): MobilisState {
        val ator = exigir(estado, atorId)
        val equipamento = localizarPorCodigo(estado, codigo)
        if (equipamento.status == StatusEquipamento.BAIXADO) erro("Este QR Code está baixado e indisponível.")
        ator.operar(equipamento.instituicaoId)
        if (equipamento.status != StatusEquipamento.EMPRESTADO && equipamento.status != StatusEquipamento.DEVOLUCAO_PENDENTE) {
            erro("Este QR Code não está em empréstimo.")
        }
        val pedido = estado.pedidos.find { it.id == equipamento.pedidoAtualId && it.status == StatusPedido.RETIRADO }
            ?: erro("Não encontramos o empréstimo deste equipamento.")
        val agora = System.currentTimeMillis()
        val pedidoNovo = pedido.copy(status = StatusPedido.DEVOLVIDO, estadoNaDevolucao = conservacao)
        val equipamentoNovo = equipamento.copy(status = StatusEquipamento.HIGIENIZACAO, atualizadoEm = agora, pedidoAtualId = null, conservacao = conservacao)
        return estado.copy(
            pedidos = substituir(estado.pedidos, pedidoNovo),
            equipamentos = estado.equipamentos.map { if (it.id == equipamento.id) equipamentoNovo else it }
        ).movimento(equipamento.id, "Devolução registrada (${conservacao.rotulo})", equipamento.status.rotulo, StatusEquipamento.HIGIENIZACAO.rotulo, ator.nome)
            .auditar(ator, "Registrar devolução", "Pedido", pedido.id, equipamento.instituicaoId, equipamento.status.rotulo, StatusEquipamento.HIGIENIZACAO.rotulo)
    }

    fun solicitarConversao(
        estado: MobilisState,
        solicitanteId: Int,
        pedidoId: Int,
        justificativa: String,
        comprovanteNome: String?,
        comprovanteCaminho: String?
    ): MobilisState {
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != solicitanteId) erro("Este pedido é de outra pessoa.")
        if (pedido.status != StatusPedido.RETIRADO || pedido.modalidade != Modalidade.EMPRESTIMO) {
            erro("A conversão vale para um empréstimo ativo.")
        }
        if (Datas.vencida(pedido.prazoDevolucao)) erro("Não é possível converter um empréstimo vencido.")
        if (pedido.conversaoStatus == StatusConversao.AGUARDANDO) erro("Já existe um pedido de conversão aguardando análise.")
        if (justificativa.trim().length < 10) erro("Descreva o motivo com pelo menos 10 caracteres.")
        if (comprovanteCaminho.isNullOrBlank()) erro("Anexe o comprovante da doação.")
        val atualizado = pedido.copy(
            conversaoStatus = StatusConversao.AGUARDANDO,
            conversaoJustificativa = justificativa.trim(),
            conversaoComprovanteNome = comprovanteNome,
            conversaoComprovanteCaminho = comprovanteCaminho,
            conversaoMotivo = null
        )
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
    }

    fun analisarConversao(estado: MobilisState, atorId: Int, pedidoId: Int, permitir: Boolean, motivo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val pedido = exigirPedido(estado, pedidoId)
        ator.operar(pedido.instituicaoId)
        if (pedido.conversaoStatus != StatusConversao.AGUARDANDO) erro("Não há conversão aguardando análise.")
        val atualizado = if (permitir) {
            pedido.copy(conversaoStatus = StatusConversao.PERMITIDA, prazoDevolucao = null)
        } else {
            Validacao.motivo(motivo, "o motivo da negativa")?.let { erro(it) }
            pedido.copy(conversaoStatus = StatusConversao.NEGADA, conversaoMotivo = motivo.trim())
        }
        return estado.copy(pedidos = substituir(estado.pedidos, atualizado))
            .auditar(
                ator,
                if (permitir) "Permitir conversão" else "Negar conversão",
                "Pedido",
                pedidoId,
                pedido.instituicaoId,
                pedido.prazoDevolucao ?: "sem data",
                if (permitir) "Data de devolução removida" else motivo.trim()
            )
            .avisar(
                pedido.solicitanteId,
                "doacao",
                if (permitir) "Conversão permitida" else "Conversão negada",
                if (permitir) "Aceite o termo de doação para concluir." else "A data de devolução foi mantida. Motivo: ${motivo.trim()}",
                "conversao-$pedidoId"
            )
    }

    fun aceitarTermoDoacao(estado: MobilisState, solicitanteId: Int, pedidoId: Int): MobilisState {
        val pessoa = exigir(estado, solicitanteId)
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != pessoa.id) erro("Este pedido é de outra pessoa.")
        if (pedido.conversaoStatus != StatusConversao.PERMITIDA) erro("A instituição ainda não permitiu a doação.")
        if (pedido.termoDoacaoAceito) erro("O termo de doação já foi aceito.")
        val equipamento = exigirEquipamento(estado, pedido.equipamentoId)
        val agora = System.currentTimeMillis()
        val pedidoNovo = pedido.copy(status = StatusPedido.DOADO, modalidade = Modalidade.DOACAO, termoDoacaoAceito = true, prazoDevolucao = null)
        val equipamentoNovo = equipamento.copy(status = StatusEquipamento.DOADO, atualizadoEm = agora, pedidoAtualId = null)
        return estado.copy(
            pedidos = substituir(estado.pedidos, pedidoNovo),
            equipamentos = estado.equipamentos.map { if (it.id == equipamento.id) equipamentoNovo else it }
        ).movimento(equipamento.id, "Empréstimo convertido em doação", equipamento.status.rotulo, StatusEquipamento.DOADO.rotulo, pessoa.nome)
    }

    fun naoPrecisoMais(estado: MobilisState, solicitanteId: Int, pedidoId: Int): MobilisState {
        val pessoa = exigir(estado, solicitanteId)
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != pessoa.id) erro("Este pedido é de outra pessoa.")
        if (pedido.naoPrecisaMais) erro("A instituição já foi avisada.")
        if (pedido.status == StatusPedido.RETIRADO && pedido.modalidade == Modalidade.EMPRESTIMO) {
            val equipamento = exigirEquipamento(estado, pedido.equipamentoId)
            val atualizado = equipamento.copy(status = StatusEquipamento.DEVOLUCAO_PENDENTE, atualizadoEm = System.currentTimeMillis())
            return estado.copy(
                pedidos = substituir(estado.pedidos, pedido.copy(naoPrecisaMais = true)),
                equipamentos = estado.equipamentos.map { if (it.id == equipamento.id) atualizado else it }
            ).movimento(equipamento.id, "Pessoa informou que não precisa mais", equipamento.status.rotulo, StatusEquipamento.DEVOLUCAO_PENDENTE.rotulo, pessoa.nome)
        }
        if (pedido.status == StatusPedido.DOADO) {
            val equipamento = exigirEquipamento(estado, pedido.equipamentoId)
            val atualizado = equipamento.copy(status = StatusEquipamento.HIGIENIZACAO, atualizadoEm = System.currentTimeMillis(), disponivelDesde = System.currentTimeMillis())
            return estado.copy(
                pedidos = substituir(estado.pedidos, pedido.copy(naoPrecisaMais = true)),
                equipamentos = estado.equipamentos.map { if (it.id == equipamento.id) atualizado else it }
            ).movimento(equipamento.id, "Doação devolvida para reaproveitar", equipamento.status.rotulo, StatusEquipamento.HIGIENIZACAO.rotulo, pessoa.nome)
                .avisarGestores(pedido.instituicaoId, "estoque", "Item doado voltou", "A pessoa não precisa mais. O item foi para higienização e, ao concluir, segue para a fila.", "reuso-$pedidoId")
        }
        erro("Esta ação vale para empréstimo ativo ou doação já recebida.")
    }

    fun avaliar(estado: MobilisState, solicitanteId: Int, pedidoId: Int, notaAtendimento: Int, notaEquipamento: Int, comentario: String): MobilisState {
        val pedido = exigirPedido(estado, pedidoId)
        if (pedido.solicitanteId != solicitanteId) erro("Este pedido é de outra pessoa.")
        if (pedido.status != StatusPedido.DEVOLVIDO && pedido.status != StatusPedido.DOADO) {
            erro("A avaliação abre depois da devolução ou da doação.")
        }
        if (pedido.avaliado) erro("Esta cessão já foi avaliada.")
        if (notaAtendimento !in 1..5) erro("Informe a nota do atendimento.")
        if (notaEquipamento !in 1..5) erro("Informe a nota do equipamento.")
        val (base, id) = estado.alocar()
        val avaliacao = Avaliacao(id, pedidoId, pedido.instituicaoId, notaAtendimento, notaEquipamento, comentario.trim(), System.currentTimeMillis())
        return base.copy(
            avaliacoes = base.avaliacoes + avaliacao,
            pedidos = substituir(base.pedidos, pedido.copy(avaliado = true))
        )
    }

    fun atualizarPerfil(
        estado: MobilisState,
        usuarioId: Int,
        nome: String,
        telefone: String,
        cep: String,
        endereco: String,
        horarioRetirada: String
    ): MobilisState {
        val usuario = exigir(estado, usuarioId)
        val rotuloNome = if (usuario.papel == PapelInstituicao.GESTOR) "o nome do responsável" else "o nome"
        Validacao.nome(nome, rotuloNome)?.let { erro(it) }
        val telefoneDigitos = Validacao.digitos(telefone)
        val pedeTelefone = usuario.perfil != Perfil.ADMIN || telefoneDigitos.isNotEmpty()
        if (pedeTelefone) Validacao.telefone(telefoneDigitos)?.let { erro(it) }
        val cepDigitos = Validacao.digitos(cep)
        val pedeCep = usuario.perfil == Perfil.SOLICITANTE || usuario.papel == PapelInstituicao.GESTOR
        if (pedeCep) Validacao.cep(cepDigitos)?.let { erro(it) }
        if (usuario.papel == PapelInstituicao.GESTOR) {
            Validacao.obrigatorio(endereco, "o endereço")?.let { erro(it) }
            Validacao.obrigatorio(horarioRetirada, "o horário de retirada")?.let { erro(it) }
        }
        var atual = estado.copy(usuarios = estado.usuarios.map {
            if (it.id != usuarioId) it else it.copy(
                nome = nome.trim(),
                telefone = telefoneDigitos,
                cep = if (cepDigitos.length == 8) cepDigitos else "",
                endereco = endereco.trim()
            )
        })
        if (usuario.papel == PapelInstituicao.GESTOR && usuario.instituicaoId != null) {
            val coordenada = Geo.coordenadaDoCep(cepDigitos)
            atual = atual.copy(instituicoes = atual.instituicoes.map { instituicao ->
                if (instituicao.id != usuario.instituicaoId) instituicao else instituicao.copy(
                    endereco = endereco.trim(),
                    cep = cepDigitos,
                    horarioRetirada = horarioRetirada.trim(),
                    responsavelNome = nome.trim(),
                    telefone = telefoneDigitos,
                    latitude = coordenada.first,
                    longitude = coordenada.second
                )
            })
        }
        return atual.auditar(usuario, "Atualizar perfil", "Usuário", usuarioId, usuario.instituicaoId, usuario.nome, nome.trim())
    }

    fun alterarSenha(estado: MobilisState, usuarioId: Int, senhaAtual: String, novaSenha: String): MobilisState {
        val usuario = exigir(estado, usuarioId)
        if (usuario.senhaHash != Seguranca.hashSenha(senhaAtual)) erro("A senha atual não confere.")
        Validacao.senha(novaSenha)?.let { erro(it) }
        if (senhaAtual == novaSenha) erro("Escolha uma senha diferente da atual.")
        return estado.copy(usuarios = estado.usuarios.map {
            if (it.id == usuarioId) it.copy(senhaHash = Seguranca.hashSenha(novaSenha)) else it
        }).auditar(usuario, "Alterar senha", "Usuário", usuarioId, usuario.instituicaoId, "Senha anterior", "Senha alterada")
    }

    fun salvarNotificacoes(estado: MobilisState, usuarioId: Int, push: Boolean, email: Boolean): MobilisState {
        exigir(estado, usuarioId)
        return estado.copy(usuarios = estado.usuarios.map {
            if (it.id == usuarioId) it.copy(notificarPush = push, notificarEmail = email) else it
        })
    }

    fun excluirConta(estado: MobilisState, usuarioId: Int): MobilisState {
        val bloqueios = Consultas.bloqueiosExclusao(estado, usuarioId)
        if (bloqueios.isNotEmpty()) erro(bloqueios.joinToString(" "))
        val usuario = exigir(estado, usuarioId)
        val anonimo = usuario.copy(
            nome = "Conta excluída",
            email = "excluido-$usuarioId@mobilis.local",
            documento = "",
            telefone = "",
            cep = "",
            endereco = "",
            senhaHash = "",
            ativo = false
        )
        return estado.copy(usuarios = estado.usuarios.map { if (it.id == usuarioId) anonimo else it })
            .auditar(usuario, "Excluir conta", "Usuário", usuarioId, usuario.instituicaoId, usuario.email, "Dados removidos")
    }

    fun registrarIndicacao(estado: MobilisState, atorId: Int, unidade: String, pacienteNome: String, pacienteCpf: String, tipoId: Int, registro: String): MobilisState {
        val ator = exigir(estado, atorId)
        Validacao.obrigatorio(unidade, "a unidade de saúde")?.let { erro(it) }
        Validacao.nome(pacienteNome, "o nome do paciente")?.let { erro(it) }
        Validacao.cpf(pacienteCpf)?.let { erro(it) }
        if (registro.trim().length < 4) erro("Informe o registro profissional.")
        val tipo = estado.tipos.find { it.id == tipoId && it.ativo } ?: erro("Escolha um tipo de equipamento ativo.")
        val (base, id) = estado.alocar()
        val indicacao = Indicacao(
            id, unidade.trim(), pacienteNome.trim(), Validacao.digitos(pacienteCpf), tipo.id, ator.nome, registro.trim(), "PENDENTE", System.currentTimeMillis()
        )
        return base.copy(indicacoes = base.indicacoes + indicacao)
    }

    fun confirmarIndicacao(estado: MobilisState, solicitanteId: Int, indicacaoId: Int): MobilisState {
        val pessoa = exigir(estado, solicitanteId)
        if (pessoa.perfil != Perfil.SOLICITANTE) erro("A confirmação é feita pela pessoa indicada.")
        val indicacao = estado.indicacoes.find { it.id == indicacaoId } ?: erro("Indicação não encontrada.")
        if (indicacao.status != "PENDENTE") erro("Esta indicação já foi confirmada.")
        if (Validacao.digitos(pessoa.documento) != indicacao.pacienteCpf) erro("O CPF da conta não é o da indicação.")
        val origem = Geo.coordenadaDoCep(pessoa.cep)
        val disponiveis = estado.equipamentos.mapNotNull { equipamento ->
            if (equipamento.tipoId != indicacao.tipoId || equipamento.status != StatusEquipamento.DISPONIVEL) return@mapNotNull null
            val instituicao = estado.instituicoes.find { it.id == equipamento.instituicaoId && it.status == StatusInstituicao.APROVADA }
                ?: return@mapNotNull null
            Triple(equipamento, instituicao, Geo.distanciaKm(origem.first, origem.second, instituicao.latitude, instituicao.longitude))
        }.sortedBy { it.third }
        var atual = estado.copy(indicacoes = estado.indicacoes.map { if (it.id == indicacaoId) it.copy(status = "CONFIRMADA") else it })
        val escolhido = disponiveis.firstOrNull()
        if (escolhido == null) {
            val instituicao = estado.instituicoes.filter { it.status == StatusInstituicao.APROVADA }.minByOrNull {
                val km = Geo.distanciaKm(origem.first, origem.second, it.latitude, it.longitude)
                km
            } ?: erro("Não há instituição aprovada para esta indicação.")
            return if (atual.filas.any { it.solicitanteId == pessoa.id && it.instituicaoId == instituicao.id && it.tipoId == indicacao.tipoId }) {
                atual
            } else {
                val (base, id) = atual.alocar()
                base.copy(filas = base.filas + EntradaFila(id, pessoa.id, instituicao.id, indicacao.tipoId, System.currentTimeMillis()))
            }
        }
        val (equipamento, instituicao, _) = escolhido
        val politica = Consultas.politicaEfetiva(atual, equipamento)
        val modalidade = when {
            Modalidade.EMPRESTIMO in politica.modalidades -> Modalidade.EMPRESTIMO
            Modalidade.DOACAO in politica.modalidades -> Modalidade.DOACAO
            else -> erro("O item não tem modalidade permitida.")
        }
        val (base, pedidoId) = atual.alocar()
        val pedido = Pedido(
            id = pedidoId,
            solicitanteId = pessoa.id,
            instituicaoId = instituicao.id,
            equipamentoId = equipamento.id,
            tipoId = equipamento.tipoId,
            modalidade = modalidade,
            status = StatusPedido.APROVADO,
            justificativa = "Indicação confirmada da ${indicacao.unidade}.",
            criadoEm = System.currentTimeMillis(),
            prazoRetirada = Datas.maisDias(3),
            indicacaoId = indicacao.id,
            validadoPorIndicacao = true
        )
        val reservado = equipamento.copy(status = StatusEquipamento.RESERVADO, pedidoAtualId = pedidoId, atualizadoEm = System.currentTimeMillis())
        return base.copy(
            pedidos = base.pedidos + pedido,
            equipamentos = base.equipamentos.map { if (it.id == equipamento.id) reservado else it }
        ).movimento(equipamento.id, "Reserva por indicação", StatusEquipamento.DISPONIVEL.rotulo, StatusEquipamento.RESERVADO.rotulo, pessoa.nome)
    }

    fun convidar(estado: MobilisState, atorId: Int, email: String, codigo: String): MobilisState {
        val ator = exigir(estado, atorId)
        val instituicaoId = ator.instituicaoId ?: erro("Esta ação é do gestor.")
        ator.gerir(instituicaoId)
        val normalizado = Validacao.emailNormalizado(email)
        Validacao.email(normalizado)?.let { erro(it) }
        if (estado.usuarios.any { it.email == normalizado && it.ativo }) erro("Já existe uma conta com este e-mail.")
        if (estado.convites.any { it.email == normalizado && it.status == "PENDENTE" && it.instituicaoId == instituicaoId }) {
            erro("Já existe um convite pendente para este e-mail.")
        }
        val (base, id) = estado.alocar()
        val convite = Convite(id, instituicaoId, normalizado, codigo, "PENDENTE", System.currentTimeMillis())
        return base.copy(convites = base.convites + convite)
            .auditar(ator, "Convidar operador", "Convite", id, instituicaoId, "", normalizado)
    }

    fun aceitarConvite(
        estado: MobilisState,
        email: String,
        codigo: String,
        nome: String,
        senha: String,
        termoGeral: Boolean,
        termoSaude: Boolean
    ): MobilisState {
        exigirTermos(termoGeral, termoSaude)
        Validacao.nome(nome, "o seu nome")?.let { erro(it) }
        Validacao.senha(senha)?.let { erro(it) }
        val normalizado = Validacao.emailNormalizado(email)
        val convite = estado.convites.find { it.email == normalizado && it.status == "PENDENTE" && it.codigo == codigo.trim() }
            ?: erro("Convite não encontrado. Confira e-mail e código.")
        val limite = if (convite.expiraEm > 0) convite.expiraEm else convite.criadoEm + DIAS_CONVITE
        if (limite < System.currentTimeMillis()) erro("Este convite expirou. Peça outro ao gestor.")
        val (base, id) = estado.alocar()
        val operador = Usuario(
            id = id,
            perfil = Perfil.INSTITUICAO,
            papel = PapelInstituicao.OPERADOR,
            nome = nome.trim(),
            email = normalizado,
            senhaHash = Seguranca.hashSenha(senha),
            instituicaoId = convite.instituicaoId,
            emailConfirmado = true,
            aceitouTermoGeral = true,
            aceitouDadosSaude = true
        )
        return base.copy(
            usuarios = base.usuarios + operador,
            convites = base.convites.map { if (it.id == convite.id) it.copy(status = "ACEITO") else it }
        )
    }

    fun alterarPapel(estado: MobilisState, atorId: Int, usuarioId: Int, papel: PapelInstituicao): MobilisState {
        val ator = exigir(estado, atorId)
        val alvo = exigir(estado, usuarioId)
        val instituicaoId = ator.instituicaoId ?: erro("Esta ação é do gestor.")
        ator.gerir(instituicaoId)
        if (alvo.instituicaoId != instituicaoId || alvo.perfil != Perfil.INSTITUICAO) erro("Esta pessoa não é da sua equipe.")
        if (alvo.papel == PapelInstituicao.GESTOR && papel != PapelInstituicao.GESTOR && gestoresAtivos(estado, instituicaoId) <= 1) {
            erro("O único gestor não pode deixar de ser gestor.")
        }
        return estado.copy(usuarios = estado.usuarios.map { if (it.id == usuarioId) it.copy(papel = papel) else it })
            .auditar(ator, "Alterar papel", "Usuário", usuarioId, instituicaoId, alvo.papel?.rotulo ?: "", papel.rotulo)
    }

    fun desativarMembro(estado: MobilisState, atorId: Int, usuarioId: Int): MobilisState {
        val ator = exigir(estado, atorId)
        val alvo = exigir(estado, usuarioId)
        val instituicaoId = ator.instituicaoId ?: erro("Esta ação é do gestor.")
        ator.gerir(instituicaoId)
        if (alvo.instituicaoId != instituicaoId) erro("Esta pessoa não é da sua equipe.")
        if (!alvo.ativo) erro("Esta conta já está desativada.")
        if (alvo.papel == PapelInstituicao.GESTOR && gestoresAtivos(estado, instituicaoId) <= 1) {
            erro("O único gestor não pode ser desativado.")
        }
        return estado.copy(usuarios = estado.usuarios.map { if (it.id == usuarioId) it.copy(ativo = false) else it })
            .auditar(ator, "Desativar operador", "Usuário", usuarioId, instituicaoId, "Ativo", "Inativo")
    }

    private fun exigirTermos(geral: Boolean, saude: Boolean) {
        if (!geral || !saude) erro("Aceite o termo geral e o uso de dados de saúde para continuar.")
    }

    private fun validarSolicitante(dados: NovoSolicitante) {
        Validacao.nome(dados.nome)?.let { erro(it) }
        Validacao.cpf(dados.cpf)?.let { erro(it) }
        Validacao.email(dados.email)?.let { erro(it) }
        Validacao.telefone(dados.telefone)?.let { erro(it) }
        Validacao.cep(dados.cep)?.let { erro(it) }
    }

    private fun validarInstituicao(dados: NovaInstituicao) {
        Validacao.obrigatorio(dados.razaoSocial, "a razão social")?.let { erro(it) }
        Validacao.cnpj(dados.cnpj)?.let { erro(it) }
        Validacao.obrigatorio(dados.endereco, "o endereço")?.let { erro(it) }
        Validacao.cep(dados.cep)?.let { erro(it) }
        Validacao.obrigatorio(dados.horarioRetirada, "o horário de retirada")?.let { erro(it) }
        Validacao.nome(dados.responsavelNome, "o nome do responsável")?.let { erro(it) }
        Validacao.email(dados.email)?.let { erro(it) }
        Validacao.telefone(dados.telefone)?.let { erro(it) }
    }

    private fun garantirEmailLivre(estado: MobilisState, email: String) {
        val normalizado = Validacao.emailNormalizado(email)
        if (estado.usuarios.any { it.email.equals(normalizado, true) && it.ativo }) {
            erro("Já existe uma conta com este e-mail.")
        }
    }

    private fun validarPolitica(modalidades: List<Modalidade>, prazoDias: Int) {
        if (modalidades.isEmpty()) erro("Selecione ao menos uma modalidade.")
        if (prazoDias !in 1..365) erro("O prazo precisa ficar entre 1 e 365 dias.")
    }

    private fun exigirComprovanteSePreciso(modalidade: Modalidade, politica: PoliticaEfetiva, caminho: String?) {
        if (modalidade == Modalidade.DOACAO && politica.exigeComprovanteDoacao && caminho.isNullOrBlank()) {
            erro("Anexe o comprovante para a doação.")
        }
    }

    private fun exigir(estado: MobilisState, id: Int): Usuario =
        estado.usuarios.find { it.id == id } ?: erro("Usuário não encontrado.")

    private fun exigirInstituicao(estado: MobilisState, id: Int): Instituicao =
        estado.instituicoes.find { it.id == id } ?: erro("Instituição não encontrada.")

    private fun exigirEquipamento(estado: MobilisState, id: Int): Equipamento =
        estado.equipamentos.find { it.id == id } ?: erro("Equipamento não encontrado.")

    private fun exigirPedido(estado: MobilisState, id: Int): Pedido =
        estado.pedidos.find { it.id == id } ?: erro("Pedido não encontrado.")

    private fun localizarPorCodigo(estado: MobilisState, codigo: String): Equipamento {
        val limpo = codigo.trim().removePrefix("MOBILIS:").trim()
        return estado.equipamentos.find { it.codigo.equals(limpo, true) } ?: erro("QR Code não reconhecido.")
    }

    private fun gestoresAtivos(estado: MobilisState, instituicaoId: Int): Int =
        estado.usuarios.count {
            it.instituicaoId == instituicaoId && it.papel == PapelInstituicao.GESTOR && it.ativo
        }

    private fun Usuario.administrar() {
        if (!ativo || perfil != Perfil.ADMIN) erro("Apenas a administração pode fazer isso.")
    }

    private fun Usuario.operar(instituicaoId: Int) {
        if (!ativo || perfil != Perfil.INSTITUICAO || this.instituicaoId != instituicaoId) {
            erro("Esta ação pertence à equipe da instituição.")
        }
    }

    private fun Usuario.gerir(instituicaoId: Int) {
        operar(instituicaoId)
        if (papel != PapelInstituicao.GESTOR) erro("Apenas o gestor pode fazer isso.")
    }

    private fun <T> substituir(lista: List<T>, item: T): List<T> {
        return lista.map { atual ->
            val mesmo = when {
                atual is Pedido && item is Pedido -> atual.id == item.id
                atual is Instituicao && item is Instituicao -> atual.id == item.id
                else -> false
            }
            if (mesmo) item else atual
        }
    }

    private fun MobilisState.liberarReserva(pedido: Pedido, nome: String, descricao: String): MobilisState {
        val equipamento = equipamentos.find { it.id == pedido.equipamentoId } ?: return this
        if (equipamento.pedidoAtualId != pedido.id || equipamento.status != StatusEquipamento.RESERVADO) return this
        val livre = equipamento.copy(status = StatusEquipamento.DISPONIVEL, pedidoAtualId = null, atualizadoEm = System.currentTimeMillis(), disponivelDesde = System.currentTimeMillis())
        return copy(equipamentos = equipamentos.map { if (it.id == livre.id) livre else it })
            .movimento(livre.id, descricao, StatusEquipamento.RESERVADO.rotulo, StatusEquipamento.DISPONIVEL.rotulo, nome)
    }

    private fun MobilisState.movimento(equipamentoId: Int, descricao: String, de: String, para: String, usuarioNome: String): MobilisState {
        val (base, id) = alocar()
        val item = Movimentacao(id, equipamentoId, descricao, de, para, System.currentTimeMillis(), usuarioNome)
        return base.copy(movimentacoes = base.movimentacoes + item)
    }

    private fun MobilisState.auditar(
        ator: Usuario,
        acao: String,
        entidade: String,
        entidadeId: Int,
        instituicaoId: Int?,
        antes: String,
        depois: String
    ): MobilisState {
        val (base, id) = alocar()
        val registro = RegistroAuditoria(
            id = id,
            usuarioId = ator.id,
            usuarioNome = ator.nome,
            instituicaoId = instituicaoId ?: ator.instituicaoId,
            acao = acao,
            entidade = entidade,
            entidadeId = entidadeId,
            data = System.currentTimeMillis(),
            antes = antes,
            depois = depois
        )
        return base.copy(auditoria = base.auditoria + registro)
    }

    fun marcarAvisosLidos(estado: MobilisState, usuarioId: Int): MobilisState =
        estado.copy(notificacoes = estado.notificacoes.map {
            if (it.usuarioId == usuarioId) it.copy(lida = true) else it
        })

    fun marcarAvisoLido(estado: MobilisState, usuarioId: Int, avisoId: Int): MobilisState {
        val aviso = estado.notificacoes.find { it.id == avisoId } ?: erro("Aviso não encontrado.")
        if (aviso.usuarioId != usuarioId) erro("Este aviso é de outra pessoa.")
        return estado.copy(notificacoes = estado.notificacoes.map {
            if (it.id == avisoId) it.copy(lida = true) else it
        })
    }

    fun rotina(estado: MobilisState, agora: Long = System.currentTimeMillis()): MobilisState {
        var atual = estado
        val hoje = Datas.hoje()
        atual.pedidos.filter { it.status == StatusPedido.APROVADO && Datas.vencida(it.prazoRetirada) }.forEach { pedido ->
            atual = atual.copy(pedidos = substituir(atual.pedidos, pedido.copy(status = StatusPedido.EXPIRADO)))
                .liberarReserva(pedido, "Rotina", "Reserva expirada")
                .oferecerFila(pedido.equipamentoId, "Rotina")
                .avisar(pedido.solicitanteId, "prazo", "Reserva expirada", "O prazo de retirada acabou e o item voltou ao estoque.", "expira-${pedido.id}")
        }
        atual.pedidos.filter {
            it.status == StatusPedido.RETIRADO && it.modalidade == Modalidade.EMPRESTIMO && !it.atrasoGrave && Datas.diasDeAtraso(it.prazoDevolucao) >= DIAS_ATRASO_GRAVE
        }.forEach { pedido ->
            val nome = atual.usuarios.find { it.id == pedido.solicitanteId }?.nome ?: "Solicitante"
            atual = atual.copy(pedidos = substituir(atual.pedidos, pedido.copy(atrasoGrave = true)))
                .avisar(pedido.solicitanteId, "atraso", "Atraso grave", "O empréstimo passou de 7 dias do prazo. Novos pedidos ficam bloqueados até a devolução.", "grave-${pedido.id}")
                .avisarGestores(pedido.instituicaoId, "atraso", "Atraso grave", "$nome está em atraso grave no ${Consultas.nomeTipo(atual, pedido.tipoId)}.", "grave-inst-${pedido.id}")
        }
        atual.pedidos.filter { it.status == StatusPedido.RETIRADO && it.modalidade == Modalidade.EMPRESTIMO }.forEach { pedido ->
            val pessoa = atual.usuarios.find { it.id == pedido.solicitanteId } ?: return@forEach
            if (!pessoa.notificarPush && !pessoa.notificarEmail) return@forEach
            val (titulo, texto, sufixo) = when {
                Datas.diasDeAtraso(pedido.prazoDevolucao) > 0 -> Triple("Empréstimo em atraso", "A data de devolução já passou.", "atraso")
                pedido.prazoDevolucao == hoje -> Triple("Devolução hoje", "O empréstimo vence hoje.", "hoje")
                Datas.venceEmAte(pedido.prazoDevolucao, 2) -> Triple("Devolução perto", "O empréstimo vence em até 2 dias.", "proximo")
                else -> return@forEach
            }
            atual = atual.avisar(pedido.solicitanteId, "prazo", titulo, texto, "lembrete-${pedido.id}-$sufixo-$hoje")
            if (sufixo == "atraso") {
                atual = atual.avisarGestores(pedido.instituicaoId, "atraso", "Empréstimo em atraso", "${pessoa.nome} passou da data de ${Consultas.nomeTipo(atual, pedido.tipoId)}.", "lembrete-inst-${pedido.id}-$hoje")
            }
        }
        atual.equipamentos.filter { it.status == StatusEquipamento.DISPONIVEL }.forEach { equipamento ->
            val instituicao = atual.instituicoes.find { it.id == equipamento.instituicaoId } ?: return@forEach
            if (Datas.diasDesde(equipamento.disponivelDesde) < instituicao.limiteOciosidadeDias) return@forEach
            val regioes = atual.filas.filter {
                it.tipoId == equipamento.tipoId && it.instituicaoId != equipamento.instituicaoId && it.status == "AGUARDANDO"
            }.mapNotNull { fila -> atual.instituicoes.find { it.id == fila.instituicaoId }?.razaoSocial }.distinct()
            val onde = if (regioes.isEmpty()) "Não há fila deste tipo em outra instituição." else "Há fila em: ${regioes.joinToString()}."
            atual = atual.avisarGestores(
                equipamento.instituicaoId,
                "ociosidade",
                "Equipamento parado",
                "${equipamento.codigo} está disponível há ${Datas.diasDesde(equipamento.disponivelDesde)} dias. $onde",
                "ocioso-${equipamento.id}"
            )
        }
        val convites = atual.convites.map { convite ->
            val limite = if (convite.expiraEm > 0) convite.expiraEm else convite.criadoEm + DIAS_CONVITE
            if (convite.status == "PENDENTE" && limite < agora) convite.copy(status = "EXPIRADO") else convite
        }
        return if (convites == atual.convites) atual else atual.copy(convites = convites)
    }

    private fun MobilisState.avisar(usuarioId: Int, tipo: String, titulo: String, texto: String, chave: String): MobilisState {
        if (chave.isNotEmpty() && notificacoes.any { it.usuarioId == usuarioId && it.chave == chave }) return this
        val (base, id) = alocar()
        val aviso = Notificacao(id, usuarioId, tipo, titulo, texto, false, System.currentTimeMillis(), chave)
        return base.copy(notificacoes = base.notificacoes + aviso)
    }

    private fun MobilisState.avisarGestores(instituicaoId: Int, tipo: String, titulo: String, texto: String, chave: String): MobilisState {
        var atual = this
        usuarios.filter { it.instituicaoId == instituicaoId && it.papel == PapelInstituicao.GESTOR && it.ativo }.forEach { gestor ->
            atual = atual.avisar(gestor.id, tipo, titulo, texto, chave)
        }
        return atual
    }

    private fun MobilisState.oferecerFila(equipamentoId: Int, atorNome: String): MobilisState {
        val equipamento = equipamentos.find { it.id == equipamentoId } ?: return this
        if (equipamento.status != StatusEquipamento.DISPONIVEL) return this
        val fila = filas.filter { it.instituicaoId == equipamento.instituicaoId && it.tipoId == equipamento.tipoId && it.status == "AGUARDANDO" }
            .minByOrNull { it.criadoEm } ?: return this
        val politica = Consultas.politicaEfetiva(this, equipamento)
        val modalidade = politica.modalidades.firstOrNull() ?: return this
        val (base, pedidoId) = alocar()
        val pedido = Pedido(
            id = pedidoId,
            solicitanteId = fila.solicitanteId,
            instituicaoId = equipamento.instituicaoId,
            equipamentoId = equipamento.id,
            tipoId = equipamento.tipoId,
            modalidade = modalidade,
            status = StatusPedido.APROVADO,
            justificativa = "Reserva automática da fila de espera.",
            criadoEm = System.currentTimeMillis(),
            prazoRetirada = Datas.maisDias(3)
        )
        val reservado = equipamento.copy(status = StatusEquipamento.RESERVADO, pedidoAtualId = pedidoId, atualizadoEm = System.currentTimeMillis())
        return base.copy(
            pedidos = base.pedidos + pedido,
            equipamentos = base.equipamentos.map { if (it.id == equipamento.id) reservado else it },
            filas = base.filas.map { if (it.id == fila.id) it.copy(status = "ATENDIDO") else it }
        ).movimento(equipamento.id, "Reserva da fila", StatusEquipamento.DISPONIVEL.rotulo, StatusEquipamento.RESERVADO.rotulo, atorNome)
            .avisar(
                fila.solicitanteId,
                "fila",
                "Sua vez na fila",
                "Reservamos ${Consultas.nomeTipo(base, equipamento.tipoId)} para você. Aceite o termo e retire até ${Datas.exibir(pedido.prazoRetirada)}.",
                "fila-${fila.id}"
            )
    }
}
