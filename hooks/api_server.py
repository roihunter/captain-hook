# -*- coding: utf-8 -*-

import falcon
import pika.exceptions
import requests

from . import services, settings


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
            self._logger.info("GET 200 scope %s params: %s headers: %s", scope, req.params, req.headers)
            resp.status = falcon.HTTP_200
            resp.body = req.get_param("hub.challenge")
        else:
            self._logger.info("GET 400 scope %s params: %s headers: %s", scope, req.params, req.headers)
            resp.status = falcon.HTTP_400

    def on_post(self, req, resp, scope):
        raw_data = req.stream.read()
        payload = raw_data.decode("utf-8")
        self._logger.info("POST scope: %s payload: %r headers: %s", scope, payload, req.headers)

        try:
            with self._publisher as publisher:
                publisher.send_event(scope, payload)

            resp.status = falcon.HTTP_200
        except pika.exceptions.AMQPError:
            self._logger.exception("Exception while publishing to RabbitMQ.")
            resp.status = falcon.HTTP_500


class HubspotHooks:
    def __init__(self):
        self._publisher = services.publisher()
        self._logger = services.logger()

    def on_post(self, req, resp):
        raw_data = req.stream.read()
        payload = raw_data.decode("utf-8")
        self._logger.info("POST hubspot payload: %r headers: %s", payload, req.headers)

        try:
            with self._publisher as publisher:
                publisher.send_hubspot_event(payload)

            resp.status = falcon.HTTP_200
        except pika.exceptions.AMQPError:
            self._logger.exception("Exception while publishing to RabbitMQ.")
            resp.status = falcon.HTTP_500


class CorsProxy:
    _DEFAULT_USER_AGENT = "ROI Hunter/CORS proxy; http://roihunter.com/"
    _TIMEOUT_IN_SECONDS = (7, 10)

    def __init__(self):
        self._logger = services.logger()

    def on_get(self, req, resp):
        resp.set_header("access-control-allow-origin", "*")
        resp.set_header("access-control-allow-methods", "GET")
        resp.set_header("access-control-max-age", "21600")
        allow_headers = req.get_header("access-control-request-headers")
        if allow_headers:
            resp.set_header("access-control-allow-headers", allow_headers)

        url = req.get_param("url")

        try:
            self._logger.info("Trying to proxy URL: %s", url)
            response = requests.get(url, timeout=self._TIMEOUT_IN_SECONDS, headers={"User-Agent": self._get_user_agent(req)})
            response.raise_for_status()

            resp.data = response.content
            resp.content_type = response.headers["Content-Type"]
            resp.status = falcon.HTTP_200
        except requests.HTTPError:
            self._logger.exception("Not able to proxy URL: %s", url)
            resp.status = str(response.status_code) + " " + response.reason
        except Exception:
            self._logger.exception("Not able to proxy URL: %s", url)
            resp.status = falcon.HTTP_400

    @classmethod
    def _get_user_agent(cls, api_request):
        user_agent = api_request.headers.get("USER-AGENT")
        if user_agent:
            return user_agent
        else:
            return cls._DEFAULT_USER_AGENT


class API(falcon.API):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.add_route("/health", Health())
        self.add_route(_API_PREFIX + "/{scope}/fb/hooks", FacebookHooks())
        self.add_route(_API_PREFIX + "/hubspot/hooks", HubspotHooks())
        self.add_route(_API_PREFIX + "/proxy", CorsProxy())
