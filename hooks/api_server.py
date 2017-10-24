# -*- coding: utf-8 -*-

import re
from urllib.parse import quote

import falcon
import pika.exceptions
import requests

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

            self._send_to_heroku(raw_data)
            resp.status = falcon.HTTP_200
        except pika.exceptions.AMQPError:
            self._logger.exception("Exception while publishing to RabbitMQ.")
            resp.status = falcon.HTTP_500

    def _send_to_heroku(self, raw_data):
        """
        This some kind of new endpoint for FB events. I don't know why it's needed
        and what is there but Andre with Pavel asked for this now.
        see https://is.roihunter.com/issues/18794
        """
        try:
            requests.post("https://sheltered-garden-54333.herokuapp.com/webhook", data=raw_data, timeout=(5, 10))
        except:
            self._logger.exception("Unexpected error during sending event to Heroku endpoint.")


class CorsProxy:
    _DEFAULT_USER_AGENT = "ROI Hunter/CORS proxy; http://roihunter.com/"
    _ALLANI_PL_PATTERN = re.compile(r"(?P<prefix>http://st\.allani\.pl/.+/)(?P<url>http.+)$", re.UNICODE)

    def __init__(self):
        self._logger = services.logger()

    def __call__(self, req, resp):
        resp.set_header("access-control-allow-origin", "*")
        resp.set_header("access-control-allow-methods", "GET")
        resp.set_header("access-control-max-age", "21600")
        allow_headers = req.get_header("access-control-request-headers")
        if allow_headers:
            resp.set_header("access-control-allow-headers", allow_headers)

        url = req.relative_uri.replace(_API_PREFIX + "/proxy/", "")
        url = self._fix_exceptional_urls(url)

        try:
            self._logger.info("Trying to proxy URL: %s", url)
            response = requests.get(url, headers={"User-Agent": self._get_user_agent(req)})
            response.raise_for_status()

            resp.data = response.content
            resp.content_type = response.headers["Content-Type"]
            resp.status = falcon.HTTP_200
        except requests.HTTPError:
            resp.status = str(response.status_code) + " " + response.reason
        except:
            self._logger.exception("Not able to proxy URL: %s", url)
            resp.status = falcon.HTTP_400

    def _fix_exceptional_urls(self, url):
        """
        This is here because we were not able to set this working by nginx so this is hack to handle our client properly.
        See http://is.roihunter.com/issues/11785#note-22 and related tasks for more info.
        """
        match = self._ALLANI_PL_PATTERN.match(url)

        if match:
            return match.group("prefix") + quote(match.group("url"), safe="")
        else:
            return url

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
        self.add_sink(CorsProxy(), _API_PREFIX + "/proxy/")
