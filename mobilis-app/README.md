# Mobilis — Global Solution 2026.2

Aplicativo Android do banco solidário de equipamentos de apoio. ONGs, igrejas e hospitais cedem cadeira de rodas, cadeira de banho, andador, muleta, bengala e cama hospitalar por empréstimo ou doação. A retirada e a devolução acontecem na instituição.

**Tema:** CitizenTech — tecnologia centrada nas necessidades reais do cidadão  
**Disciplina:** Android Kotlin Developer  
**Professor:** Ewerton Luiz de Lima Carreira

## Integrantes

| Nome | RM     |
|------|--------|
| Graziely Tavares da Silva | 559111 |
| Mariana Pacheco Torres | 558956 |
| Mayara Santos Pereira Tavares | 559031 |

## Perfis

- **Solicitante:** busca por distância, pede empréstimo ou doação, acompanha fila e pedidos, aceita termo, avalia e cuida dos próprios dados.
- **Instituição:** gestor configura política, equipe, painel e auditoria; operador cuida do estoque, do QR Code, da retirada, da devolução e da análise dos pedidos.
- **Administração:** aprova instituições, mantém o catálogo de tipos e consulta a auditoria.

Cada perfil só vê as ações do próprio papel. A interface é mobile-first, com alvos de toque grandes, rótulos visíveis e contraste pensado para WCAG 2.1 AA.

## Contas de demonstração

Senha de todas: `Mobilis@2026`

| E-mail | Perfil |
|--------|--------|
| ana.costa@mobilis.app | Solicitante |
| gestor@mobilis.app | Gestor da ONG Caminhar |
| operador@mobilis.app | Operador |
| admin@mobilis.app | Administração |
| igreja@mobilis.app | Instituição aguardando aprovação |
| hospital@mobilis.app | Gestor de outra instituição |

O código de confirmação, de recuperação de senha e de convite aparece na tela. O envio real por e-mail ainda não tem servidor.

## Como executar

1. Instalar o Android Studio com SDK 36 ou superior.
2. Abrir a pasta `mobilis-app`.
3. Aguardar o sync do Gradle.
4. Executar a configuração `app` em um emulador ou aparelho com Android 7.0 (API 24) ou superior.

Na pasta `mobilis-app`, com o `local.properties` apontando para o Android SDK:

```bash
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

Os dados ficam no aparelho, inclusive a sessão. Esta versão não altera o backend do monorepo.

O roteiro para apresentar o fluxo, da igreja em análise até a devolução, está em `ROTEIRO-PITCH.md`. Para recomeçar a demonstração do zero, limpe o armazenamento do app antes de abrir.

## Tecnologias

- Kotlin, Jetpack Compose e Material 3
- Navigation Compose, ViewModel e StateFlow
- Persistência local em JSON e token em EncryptedSharedPreferences
- CameraX e ZXing para o QR Code
- Coil para as fotos do equipamento
