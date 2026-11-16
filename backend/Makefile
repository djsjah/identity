COMPOSE := docker compose --env-file .env -f deploy/docker-compose.yml

.PHONY: config up dev down stop restart ps logs

config:
	$(COMPOSE) config --quiet

up:
	$(COMPOSE) up -d

dev:
	$(COMPOSE) up

down:
	$(COMPOSE) down

stop:
	$(COMPOSE) stop

restart:
	$(COMPOSE) restart

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs -f --tail=100
