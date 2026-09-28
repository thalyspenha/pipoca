# SETUP

Como preparar uma máquina (Mac ou Linux) para trabalhar no Pipoca.

## Requisitos

- **JDK:** o JBR que vem com o Android Studio basta (hoje OpenJDK 25). JDK 25 exige Gradle ≥ 9.1; o wrapper usa 9.8.0 (D-015). O bytecode alvo é Java 17.
- **Android SDK:** platform `android-37` (compileSdk/targetSdk), build-tools recentes, platform-tools. Instalar pelo SDK Manager do Android Studio.
- Gradle não precisa estar instalado: usar `./gradlew`.

## Variáveis de ambiente

Mac (`~/.zshrc`, já feito na máquina original):
```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
```

Linux (`~/.bashrc` ou `~/.zshrc`; ajustar o caminho de instalação do Android Studio):
```sh
export JAVA_HOME="/opt/android-studio/jbr"        # ou ~/android-studio/jbr, ou via JetBrains Toolbox
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
```

Conferir: `java -version` e `ls $ANDROID_HOME/platforms`.

## Clonar e configurar o Git (identidade pessoal)

A configuração do repo é local e não vem no clone. Em máquina nova:
```sh
git clone git@github.com:thalyspenha/pipoca.git   # se a chave pessoal não for a padrão, usar GIT_SSH_COMMAND abaixo
cd pipoca
git config user.name "Thalys Penha"
git config user.email "thalyspenha@outlook.com.br"
git config core.sshCommand "ssh -i ~/.ssh/id_rsa_personal -o IdentitiesOnly=yes"
```
Clone com a chave pessoal antes de configurar o repo:
`GIT_SSH_COMMAND="ssh -i ~/.ssh/id_rsa_personal -o IdentitiesOnly=yes" git clone git@github.com:thalyspenha/pipoca.git`

A chave `~/.ssh/id_rsa_personal` precisa existir na máquina (copiar ou gerar uma nova e cadastrar no GitHub).

## local.properties (não commitado)

Criar na raiz do projeto:
```properties
sdk.dir=/home/<usuario>/Android/Sdk          # Mac: /Users/<usuario>/Library/Android/sdk
TMDB_API_TOKEN=seu_token_aqui                # opcional; sem ele o app roda e mostra aviso
```
Ver `.env.example` e `docs/TMDB.md`.

## Verificar

```sh
./gradlew build
```
Deve terminar em `BUILD SUCCESSFUL` com todos os testes unitários passando.

Problema conhecido (D-041, D-042, D-045): o lint falhava às vezes com "Unexpected failure during lint analysis" no primeiro build depois de muitas mudanças. Subir a memória do Gradle para 4 GB (`gradle.properties`) não resolveu (D-045). Se acontecer, rodar `./gradlew build` de novo; persistindo, tratar como erro real.

## Rodar no aparelho

S25: ativar modo desenvolvedor e depuração USB, conectar e `./gradlew installDebug` (ou Run no Android Studio). No Linux pode ser preciso regra `udev` para o `adb` enxergar o aparelho (`adb devices`).

Testes instrumentados (DAOs, migrações) sem perder os dados do app (D-028):
```sh
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w com.thalyspenha.pipoca.test/androidx.test.runner.AndroidJUnitRunner
adb shell "run-as com.thalyspenha.pipoca sh -c 'rm -f databases/migration-test.db*'"
```
Não usar `./gradlew connectedDebugAndroidTest`: ao final ele desinstala o app e apaga o banco do aparelho.

Cada máquina tem sua chave de debug (`~/.android/debug.keystore`). Instalar a partir de outra máquina falha com `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; para não perder dados ao alternar Mac/Linux, copiar o mesmo `debug.keystore` para as duas. Em 2026-09-28 o app foi desinstalado no S25 (dados apagados) para instalar a partir do Linux.

## APKs (debug e release)

```sh
./gradlew assembleDebug assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```
Release sem R8 e assinado com a chave de debug da máquina (D-058): instala por cima do debug da mesma máquina sem perder dados. Para alternar Mac/Linux sem desinstalar, usar o mesmo `debug.keystore` nas duas (acima). O token TMDB fica dentro do APK: não compartilhar o arquivo. Notas da versão em `RELEASE_NOTES.md`. Com o release instalado, os testes instrumentados não rodam (release não é depurável): `./gradlew installDebug` antes, por cima, sem perder dados.

## Linux (Omarchy)

- Android Studio em `~/Documents/android-studio`; JDK = JBR dele: `JAVA_HOME=~/Documents/android-studio/jbr ./gradlew build` (sem `java` no PATH).
- SDK em `~/Android/Sdk` (`sdk.dir` no `local.properties`); `adb` já no PATH.

## Claude Code

A memória local do Claude (`~/.claude/projects/...`) não é sincronizada entre máquinas. O que importa para continuar o trabalho está em `CLAUDE.md` e `docs/` (estado atual: `docs/ROADMAP.md`).
