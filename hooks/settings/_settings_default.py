# -*- coding: utf-8 -*-

from ._utils import get_env


DEVELOPMENT_MODE = get_env("DEVELOPMENT_MODE", default=True)
FACEBOOK_VERIFY_TOKEN = get_env("FACEBOOK_VERIFY_TOKEN", default="token")

RABBIT_LOGIN = get_env("RABBIT_LOGIN", default="guest")
RABBIT_PASSWORD = get_env("RABBIT_PASSWORD", default="guest")
RABBIT_HOST = get_env("RABBIT_HOST", default="localhost")
RABBIT_PORT = get_env("RABBIT_PORT", default=5672)
RABBIT_VIRTUAL_HOST = "hooks"
RABBIT_EXCHANGE = "events"

GRAYLOG_HOST = get_env("GRAYLOG_HOST", default=None)
GRAYLOG_PORT = get_env("GRAYLOG_PORT", default=None)
