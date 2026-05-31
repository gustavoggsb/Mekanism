# Makefile local de conveniência para o Mekanism (branch 26.1)
#
# O projeto exige Java 25 para a COMPILAÇÃO (gradle.properties: java_version=25),
# mas o Gradle em si roda bem em Java 24 — o resolvedor foojay baixa o toolchain
# JDK 25 automaticamente. Por isso apontamos JAVA_HOME para o JDK 24 instalado.
#
# Para sobrescrever o JDK usado para rodar o Gradle:
#   make compile JAVA_HOME=/caminho/para/jdk
#
# Uso: `make` ou `make help` lista todos os alvos.

# Descobre o JDK 24 via java_home (macOS). Pode ser sobrescrito na linha de comando.
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 24 2>/dev/null)
export JAVA_HOME

GRADLE := ./gradlew --console=plain
LIBS   := build/libs

.DEFAULT_GOAL := help

# ---------------------------------------------------------------------------
# Ajuda
# ---------------------------------------------------------------------------
.PHONY: help
help: ## Mostra esta ajuda
	@echo "JAVA_HOME = $(JAVA_HOME)"
	@echo ""
	@echo "Alvos disponíveis:"
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}'

.PHONY: java
java: ## Mostra o JDK que será usado para rodar o Gradle
	@echo "JAVA_HOME = $(JAVA_HOME)"
	@"$(JAVA_HOME)/bin/java" -version

# ---------------------------------------------------------------------------
# Compilação / testes
# ---------------------------------------------------------------------------
.PHONY: compile
compile: ## Compila TODOS os source sets (main, api, tools, additions, generators, datagen)
	$(GRADLE) compileJava compileApiJava compileToolsJava compileAdditionsJava compileGeneratorsJava \
		compileDatagenMainJava compileDatagenAdditionsJava compileDatagenGeneratorsJava compileDatagenToolsJava \
		compileTestJava compileGameTestJava

.PHONY: test
test: ## Roda os testes unitários (JUnit)
	$(GRADLE) test

.PHONY: gametest
gametest: ## Roda os game tests in-game no servidor (headless) e sai
	$(GRADLE) runGameTestServer

.PHONY: gametest-client
gametest-client: ## Roda os game tests in-game no cliente
	$(GRADLE) runGameTestClient

.PHONY: check
check: ## Compila tudo + testes unitários + game tests do servidor
	$(GRADLE) compileJava test runGameTestServer

# ---------------------------------------------------------------------------
# Datagen
# ---------------------------------------------------------------------------
.PHONY: datagen
datagen: ## Roda o data generation (recipes, loot, models, etc.)
	$(GRADLE) runData

# ---------------------------------------------------------------------------
# Empacotamento (.jar)
# ---------------------------------------------------------------------------
.PHONY: jars
jars: ## Gera TODOS os jars (all + individuais) e lista o resultado
	$(GRADLE) allJar jar additionsJar generatorsJar toolsJar
	@echo "" && echo "Jars em $(LIBS):" && ls -lh $(LIBS)/*.jar | awk '{print "  "$$5"\t"$$9}'

.PHONY: jar-all
jar-all: ## Gera apenas o jar único 'all' (Mekanism + Additions + Generators + Tools) para testar in-game
	$(GRADLE) allJar
	@echo "" && ls -lh $(LIBS)/*-all.jar | awk '{print "  "$$5"\t"$$9}'

.PHONY: build
build: ## Build completo dos artefatos (assemble, sem rodar testes)
	$(GRADLE) assemble

# ---------------------------------------------------------------------------
# Rodar o jogo
# ---------------------------------------------------------------------------
.PHONY: client
client: ## Inicia o cliente Minecraft com o mod (abre janela)
	$(GRADLE) runClient

.PHONY: server
server: ## Inicia o servidor dedicado com o mod
	$(GRADLE) runServer

# ---------------------------------------------------------------------------
# Manutenção
# ---------------------------------------------------------------------------
.PHONY: clean
clean: ## Limpa os artefatos de build
	$(GRADLE) clean

.PHONY: refresh
refresh: ## Re-resolve dependências (use após mudar versões em gradle.properties)
	$(GRADLE) --refresh-dependencies build -x test

.PHONY: tasks
tasks: ## Lista todas as tasks do Gradle
	$(GRADLE) tasks --all
