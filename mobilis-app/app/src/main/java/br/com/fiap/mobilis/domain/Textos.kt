package br.com.fiap.mobilis.domain

object Textos {
    val termoGeral = """
        O Mobilis é um banco solidário de equipamentos de apoio. ONGs, igrejas e hospitais cedem, sem custo, cadeira de rodas, cadeira de banho, andador, muleta, bengala e cama hospitalar.

        A retirada e a devolução acontecem sempre no ponto da instituição. O aplicativo não organiza entrega em domicílio.

        Itens que exigem prescrição ou suporte técnico, como concentrador de oxigênio, ficam fora deste serviço.

        A plataforma e a instituição usam os dados de cadastro para criar a conta, analisar o pedido, combinar a retirada e cumprir obrigações legais. A base é o consentimento e a execução do empréstimo ou da doação.

        Você pode consultar, exportar ou pedir a exclusão dos seus dados em Meus dados. A exclusão fica bloqueada enquanto existir pedido, reserva ou empréstimo em aberto.

        O contato de privacidade desta versão é privacidade@mobilis.app.
    """.trimIndent()

    val termoSaude = """
        Motivo do pedido, indicação de profissional de saúde e comprovante podem revelar informação de saúde.

        Esse dado é usado só para a instituição decidir o empréstimo ou a doação e para o histórico daquele equipamento. Ele não é vendido e não aparece na avaliação, que é anônima.

        O aceite deste texto é separado do termo geral. Os dois são necessários para concluir o cadastro.
    """.trimIndent()

    fun termoCessao(modalidade: Modalidade, instituicao: String, equipamento: String, prazo: String?): String {
        val corpo = if (modalidade == Modalidade.EMPRESTIMO) {
            "Recebo $equipamento por empréstimo, sem custo, para uso assistivo. A devolução é no ponto da $instituicao até ${prazo ?: "a data combinada"}. Devolvo o item limpo e aviso se não precisar mais."
        } else {
            "Recebo $equipamento em doação definitiva, sem custo. A retirada é no ponto da $instituicao. Não há data de devolução. Se eu não precisar mais, aviso a instituição para o reaproveitamento."
        }
        return "Termo de ${modalidade.rotulo.lowercase()}\n\n$corpo\n\nAo aceitar, confirmo que li este termo antes da retirada."
    }
}
