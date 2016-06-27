# -*- coding: utf-8 -*-

import falcon
import pika.exceptions
from . import services
from . import settings


_API_PREFIX = "/api/v1"


class Health:
    def on_get(self, req, resp):
        resp.status = falcon.HTTP_200
        resp.body = '{"status": "UP"}'


class FacebookHooks:
    def __init__(self):
        self._publisher = services.publisher()
        self._logger = services.logger()

    def on_get(self, req, resp, scope):
        if req.get_param("hub.mode") == "subscribe" and req.get_param("hub.verify_token") == settings.FACEBOOK_VERIFY_TOKEN:
            self._logger.info("GET 200 scope %s params: %s", scope, req.params)
            resp.status = falcon.HTTP_200
            resp.body = req.get_param("hub.challenge")
        else:
            self._logger.info("GET 400 scope %s params: %s", scope, req.params)
            resp.status = falcon.HTTP_400

    def on_post(self, req, resp, scope):
        payload = req.stream.read().decode("utf-8")
        self._logger.info("POST scope: %s payload: %r", scope, payload)

        try:
            with self._publisher as publisher:
                publisher.send_event(scope, payload)
            resp.status = falcon.HTTP_200

        except pika.exceptions.AMQPError:
            self._logger.exception("Exception while publishing to RabbitMQ.")
            resp.status = falcon.HTTP_500


class API(falcon.API):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.add_route("/health", Health())
        self.add_route(_API_PREFIX + "/{scope}/fb/hooks", FacebookHooks())
